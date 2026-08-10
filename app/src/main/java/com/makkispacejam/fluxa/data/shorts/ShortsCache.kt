package com.makkispacejam.fluxa.data.shorts

import com.makkispacejam.fluxa.data.local.CachedVideoEntity
import com.makkispacejam.fluxa.data.local.FluxaDao
import com.makkispacejam.fluxa.models.VideoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ShortsCache {

    // Guardar videos en cache 
    suspend fun saveToDiskCache(dao: FluxaDao, videos: List<VideoModel>) = withContext(Dispatchers.IO) {
        try {
            val oneWeekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
            dao.pruneOldCachedVideos(oneWeekAgo)
            val entities = videos.map { v ->
                CachedVideoEntity(v.id).apply {
                    title = v.title
                    channelName = v.channelName
                    channelId = v.channelId ?: ""
                    channelAvatarUrl = v.channelAvatarUrl ?: ""
                    imageUrl = v.imageUrl
                    videoUrl = v.videoUrl
                    timestamp = v.timestamp
                    viewCount = v.viewCount
                }
            }
            dao.insertCachedVideos(entities)
        } catch (_: Exception) {}
    }

    fun toVideoModel(entity: CachedVideoEntity) = VideoModel(
        id = entity.videoId,
        title = entity.title,
        channelName = entity.channelName,
        channelId = entity.channelId,
        channelAvatarUrl = entity.channelAvatarUrl,
        imageUrl = entity.imageUrl,
        videoUrl = entity.videoUrl,
        timestamp = entity.timestamp,
        viewCount = entity.viewCount
    )
}
