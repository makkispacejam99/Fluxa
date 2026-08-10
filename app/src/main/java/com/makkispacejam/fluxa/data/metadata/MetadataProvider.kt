package com.makkispacejam.fluxa.data.metadata

import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.models.VideoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class VideoInfo(
    val videoModel: VideoModel,
    val description: String,
    val viewCount: Long,
    val uploaderName: String,
    val uploaderAvatarUrl: String? = null,
    val subscriberCount: Long = -1L,
    val relatedVideos: List<VideoModel> = emptyList()
)

class VideoMetadataProvider {

    // Cargar metadatos de video
    suspend fun loadVideoData(videoId: String): VideoInfo? = withContext(Dispatchers.IO) {
        try {
            val service = ServiceList.YouTube
            val cleanId = VideoExtractor.cleanVideoId(videoId)
            val videoUrl = "https://www.youtube.com/watch?v=$cleanId"
            val streamInfo = StreamInfo.getInfo(service, videoUrl)

            val cleanedChannelName = streamInfo.uploaderName ?: "Fluxa Creator"
            val rawViews = streamInfo.viewCount
            val rawDescription = streamInfo.description?.content ?: "Sin descripción disponible."
            val rawSubs = streamInfo.uploaderSubscriberCount

            val uploaderAvatar = streamInfo.uploaderAvatars
                .firstOrNull()?.url
                ?.let { if (it.startsWith("//")) "https:$it" else it }

            val model = VideoModel(
                id = videoId,
                title = streamInfo.name ?: "Video sin título",
                channelName = cleanedChannelName,
                channelId = streamInfo.uploaderUrl?.substringAfter("/channel/") ?: "",
                channelAvatarUrl = uploaderAvatar ?: "",
                imageUrl = streamInfo.thumbnails.firstOrNull()?.url
                    ?: "https://img.youtube.com/vi/$videoId/maxresdefault.jpg",
                videoUrl = videoUrl
            )

            val related = streamInfo.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .filter { item ->
                    val isAgeRestricted = try {
                        item.javaClass.getMethod("isAgeLimit").invoke(item) as Boolean
                    } catch (_: Exception) { false }
                    
                    item.contentAvailability == ContentAvailability.AVAILABLE && !isAgeRestricted
                }
                .map { item ->
                    VideoModel(
                        id = item.url.substringAfter("v=").substringBefore("&"),
                        title = item.name ?: "Sin título",
                        channelName = item.uploaderName ?: "",
                        channelId = item.uploaderUrl?.substringAfter("/channel/") ?: "",
                        imageUrl = item.thumbnails.firstOrNull()?.url ?: "",
                        videoUrl = item.url
                    )
                }

            return@withContext VideoInfo(
                videoModel = model,
                description = rawDescription,
                viewCount = rawViews,
                uploaderName = cleanedChannelName,
                uploaderAvatarUrl = uploaderAvatar,
                subscriberCount = rawSubs,
                relatedVideos = related
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
