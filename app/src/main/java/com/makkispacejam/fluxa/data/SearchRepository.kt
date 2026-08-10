package com.makkispacejam.fluxa.data

import com.makkispacejam.fluxa.models.home.HomeFeedItem
import com.makkispacejam.fluxa.models.home.HomeFeedItemType
import com.makkispacejam.fluxa.utils.ThumbnailUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.Page
import java.io.IOException

data class SearchPageResult(
    val items: List<HomeFeedItem>,
    val nextPage: Page?
)

object SearchRepository {

    suspend fun searchVideos(
        query: String,
        retryCount: Int = 0,
        filter: String = "Todo"
    ): SearchPageResult = withContext(Dispatchers.IO) {
        try {
            val service = ServiceList.YouTube
            val contentFilter = when (filter) {
                "Videos" -> listOf("videos")
                "Canales" -> listOf("channels")
                "Playlist", "Listas" -> listOf("playlists")
                "En Vivo" -> listOf("videos")
                else -> listOf("all")
            }

            val searchHandler = service.searchQHFactory.fromQuery(query, contentFilter, "")
            val searchExtractor = service.getSearchExtractor(searchHandler)
            searchExtractor.fetchPage()

            val items = mutableListOf<HomeFeedItem>()
            val page1Items = searchExtractor.initialPage.items ?: emptyList()
            items.addAll(mapInfoItems(page1Items))

            val nextPage: Page? = if (searchExtractor.initialPage.hasNextPage()) searchExtractor.initialPage.nextPage else null

            SearchPageResult(items.distinctBy { it.videoId }, nextPage)
        } catch (_: IOException) {
            if (retryCount < 2) {
                delay(2000)
                return@withContext searchVideos(query, retryCount + 1)
            }
            SearchPageResult(emptyList(), null)
        } catch (_: Exception) {
            SearchPageResult(emptyList(), null)
        }
    }

    suspend fun loadMoreSearch(
        query: String,
        nextPageUrl: Page,
        filter: String = "Todo"
    ): SearchPageResult = withContext(Dispatchers.IO) {
        try {
            val service = ServiceList.YouTube
            val contentFilter = when (filter) {
                "Videos" -> listOf("videos")
                "Canales" -> listOf("channels")
                "Playlist", "Listas" -> listOf("playlists")
                "En Vivo" -> listOf("videos")
                else -> listOf("all")
            }

            val searchHandler = service.searchQHFactory.fromQuery(query, contentFilter, "")
            val searchExtractor = service.getSearchExtractor(searchHandler)
            searchExtractor.fetchPage()

            val nextPageItems = searchExtractor.getPage(nextPageUrl)
            val items = mapInfoItems(nextPageItems.items ?: emptyList())

            val nextPage: Page? = if (nextPageItems.hasNextPage()) nextPageItems.nextPage else null

            SearchPageResult(items, nextPage)
        } catch (_: Exception) {
            SearchPageResult(emptyList(), null)
        }
    }

    private fun mapInfoItems(
        rawItems: List<org.schabi.newpipe.extractor.InfoItem>
    ): List<HomeFeedItem> {
        return rawItems.mapNotNull { item ->
            when (item) {
                is StreamInfoItem -> {
                    val streamType = item.streamType?.name ?: ""
                    val isPlayable = streamType != "NONE" && !streamType.contains("PREMIERE") && !streamType.contains("UPCOMING")
                    val isLiveStream = streamType.contains("LIVE", ignoreCase = true)
                    val hasDuration = item.duration > 0L || isLiveStream

                    val isAgeRestricted = try {
                        item.javaClass.getMethod("isAgeLimit").invoke(item) as Boolean
                    } catch (_: Exception) { false }
                    if (item.contentAvailability != org.schabi.newpipe.extractor.stream.ContentAvailability.AVAILABLE ||
                        !isPlayable || !hasDuration || isAgeRestricted) return@mapNotNull null

                    val url = item.url ?: return@mapNotNull null
                    val id = when {
                        url.contains("/shorts/") -> url.substringAfter("/shorts/").substringBefore("?")
                        url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
                        else -> return@mapNotNull null
                    }
                    if (id.isBlank()) return@mapNotNull null

                    val uploader = item.uploaderName ?: "Fluxa Creator"
                    val channelId = item.uploaderUrl?.substringAfter("/channel/")?.substringBefore("/") ?: ""

                    HomeFeedItem(
                        videoId = id,
                        title = item.name ?: "Sin título",
                        channelName = uploader,
                        channelId = channelId,
                        channelAvatarUrl = item.uploaderAvatars.firstOrNull()?.url ?: "",
                        thumbnailUrl = ThumbnailUtils.getBestThumbnailUrl(id, item.thumbnails.firstOrNull()?.url),
                        viewCount = try { item.viewCount } catch (_: Exception) { 0L },
                        durationSeconds = if (isLiveStream) 0 else item.duration,
                        publishedTime = item.textualUploadDate ?: "",
                        timestamp = try { item.uploadDate?.instant?.toEpochMilli() ?: 0L } catch(_: Exception) { 0L },
                        itemType = if (isLiveStream) HomeFeedItemType.LIVE else HomeFeedItemType.VIDEO
                    )
                }
                is ChannelInfoItem -> {
                    val uploader = item.name ?: "Canal"
                    val formattedSubs = if (item.subscriberCount >= 0) HomeFeedItem.formatCount(item.subscriberCount) else "N/A"
                    HomeFeedItem(
                        videoId = item.url ?: "",
                        title = uploader,
                        channelName = uploader,
                        channelId = item.url?.substringAfter("/channel/") ?: "",
                        channelAvatarUrl = item.thumbnails.firstOrNull()?.url ?: "",
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url ?: "",
                        itemType = HomeFeedItemType.CHANNEL,
                        subscriberCount = "$formattedSubs suscriptores",
                        playlistVideoCount = if (item.streamCount > 0) item.streamCount.toString() else ""
                    )
                }
                is PlaylistInfoItem -> {
                    HomeFeedItem(
                        videoId = item.url ?: "",
                        title = item.name ?: "Playlist",
                        channelName = item.uploaderName ?: "",
                        channelAvatarUrl = "",
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url ?: "",
                        itemType = HomeFeedItemType.PLAYLIST,
                        playlistVideoCount = if (item.streamCount > 0) item.streamCount.toString() else ""
                    )
                }
                else -> null
            }
        }
    }
}
