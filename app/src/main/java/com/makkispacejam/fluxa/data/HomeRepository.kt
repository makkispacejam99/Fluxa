package com.makkispacejam.fluxa.data

import android.content.Context
import com.makkispacejam.fluxa.data.channels.ChannelDataExtractor
import com.makkispacejam.fluxa.data.filters.SearchFilters
import com.makkispacejam.fluxa.data.local.FluxaDatabase
import com.makkispacejam.fluxa.models.home.HomeFeedItem
import com.makkispacejam.fluxa.utils.ThumbnailUtils
import kotlinx.coroutines.*
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.Collections
import java.util.concurrent.atomic.AtomicInteger

class HomeRepository {

    companion object {
        private var feedCache: List<HomeFeedItem>? = null
        private var feedCacheTime: Long = 0L
        private const val CACHE_TTL = 30 * 60 * 1000L
    }

    // Obtención del feed principal
    suspend fun getHomeFeed(context: Context, quickLoad: Boolean = false): List<HomeFeedItem> = withContext(Dispatchers.IO) {
        val cached = feedCache
        if ((cached != null) && (System.currentTimeMillis() - feedCacheTime < CACHE_TTL)) {
            return@withContext cached
        }

        val dao = FluxaDatabase.getDatabase(context).fluxaDao()
        val watchedVideoIds = dao.allWatchedVideoIds.takeLast(500).toSet()
        val blockedChannelIds = dao.blockedChannelIds.toSet()
        val interactions = dao.getRecentInteractions(500)

        val shadowBannedIds = getShadowBannedIds(interactions) + watchedVideoIds
        val likedIds = interactions.filter { it.isLiked }.map { it.videoId }
        val seeds = likedIds.takeLast(10).distinct()

        val shownIds = FeedPreferences.loadShownVideoIds(context)
        val excludeIds = shadowBannedIds + shownIds

        val finalFeedList = Collections.synchronizedList(mutableListOf<HomeFeedItem>())
        val subFetchFailures = AtomicInteger(0)

        val subsCount = if (quickLoad) 8 else 20
        val subs = dao.getAllSubscriptions()?.shuffled()?.take(subsCount) ?: emptyList()

        if (subs.isNotEmpty()) {
            val allResults = coroutineScope {
                subs.map { sub ->
                    async(Dispatchers.IO) {
                        try {
                             withTimeout(10000) {
                                 val streams = ChannelDataExtractor.getChannelVideos("https://www.youtube.com/channel/${sub.channelId}")
                                streams.take(8).map { item ->
                                    HomeFeedItem(
                                        videoId = VideoExtractor.cleanVideoId(item.url),
                                        title = item.title,
                                        channelName = item.uploaderName,
                                        channelId = sub.channelId,
                                        channelAvatarUrl = sub.avatarUrl ?: item.uploaderAvatar,
                                        thumbnailUrl = item.thumbnail,
                                        viewCount = item.views,
                                        durationSeconds = item.duration,
                                        publishedTime = item.uploadDate,
                                        timestamp = item.timestamp,
                                    )
                                }
                            }
                        } catch (_: Exception) {
                            subFetchFailures.incrementAndGet()
                            emptyList()
                        }
                    }
                }.awaitAll().flatten().filter { it.videoId !in excludeIds && it.channelId !in blockedChannelIds && it.viewCount > 0 }
            }
            finalFeedList.addAll(allResults)
        }

        if (!quickLoad || finalFeedList.isEmpty()) {
            val recommendedResult = if (seeds.isNotEmpty()) {
                getPersonalizedFeed(seeds)
            } else {
                getGenericFeed()
            }.filter { it.videoId !in excludeIds && it.channelId !in blockedChannelIds && it.viewCount >= 0 }

            finalFeedList.addAll(recommendedResult)
        }

        if (finalFeedList.isEmpty()) {
            try {
                val desperate = SearchRepository.searchVideos("videos").items
                finalFeedList.addAll(desperate)
            } catch (_: Exception) {}
        }

        val result = finalFeedList.distinctBy { it.videoId }.shuffled().take(if (quickLoad) 20 else 45)

        FeedPreferences.saveShownVideoIds(context, result.map { it.videoId })
        feedCache = result
        feedCacheTime = System.currentTimeMillis()
        result
    }

    fun clearFeedCache() {
        feedCache = null
        feedCacheTime = 0L
    }

    // Shadowban
    private fun getShadowBannedIds(interactions: List<com.makkispacejam.fluxa.data.local.VideoInteractionEntity>): Set<String> {
        return interactions.asSequence().filter { it.isDisliked }.map { it.videoId }.toSet()
    }

    // Feed personalizado
    private suspend fun getPersonalizedFeed(seedIds: List<String>): List<HomeFeedItem> = withContext(Dispatchers.IO) {
        val results = Collections.synchronizedList(mutableListOf<HomeFeedItem>())
        val seeds = seedIds.takeLast(4).shuffled().take(2)
        seeds.map { seedId ->
            async(Dispatchers.IO) {
                try {
                    withTimeout(5000) {
                        results.addAll(getRelatedVideos(seedId).take(10))
                    }
                } catch (_: Exception) {}
            }
        }.awaitAll()
        results.distinctBy { it.videoId }
    }

    // Recomendaciones basadas en contenido similar
    private fun getRelatedVideos(videoId: String): List<HomeFeedItem> {
        try {
            val service = ServiceList.YouTube
            val streamInfo = StreamInfo.getInfo(service, "https://www.youtube.com/watch?v=$videoId")

            return streamInfo.relatedItems
                .filterIsInstance<StreamInfoItem>()
                .filter { it.duration > 40L && it.url != null }
                .filter { item ->
                    val isAgeRestricted = try {
                        item.javaClass.getMethod("isAgeLimit").invoke(item) as Boolean
                    } catch (_: Exception) { false }

                    val streamType = item.streamType?.name ?: ""
                    val isPlayable = streamType != "NONE" && !streamType.contains("PREMIERE") && !streamType.contains("UPCOMING")
                    val hasDuration = item.duration > 0L

                    item.contentAvailability == org.schabi.newpipe.extractor.stream.ContentAvailability.AVAILABLE &&
                    isPlayable && hasDuration && !isAgeRestricted
                }
                .mapNotNull { item ->
                    val url = item.url ?: return@mapNotNull null
                    val id = when {
                        url.contains("/shorts/") -> url.substringAfter("/shorts/").substringBefore("?")
                        url.contains("v=") -> url.substringAfter("v=").substringBefore("&")
                        else -> return@mapNotNull null
                    }
                    if (id.isBlank()) return@mapNotNull null

                    val uploader = item.uploaderName ?: "Fluxa Creator"
                    val channelId = item.uploaderUrl?.substringAfter("/channel/")?.substringBefore("/") ?: ""
                    val avatar = item.uploaderAvatars.firstOrNull()?.url ?: ""

                    HomeFeedItem(
                        videoId = id,
                        title = item.name ?: "Sin título",
                        channelName = uploader,
                        channelId = channelId,
                        channelAvatarUrl = avatar,
                        thumbnailUrl = ThumbnailUtils.getBestThumbnailUrl(id, item.thumbnails.firstOrNull()?.url),
                        viewCount = try { item.viewCount } catch (_: Exception) { 0L },
                        durationSeconds = item.duration,
                        publishedTime = item.textualUploadDate ?: "",
                        timestamp = try { item.uploadDate?.instant?.toEpochMilli() ?: 0L } catch(_: Exception) { 0L }
                    )
                }
        } catch (_: Exception) {
            return emptyList()
        }
    }

    // Recomendaciones genéricas
    private suspend fun getGenericFeed(): List<HomeFeedItem> = withContext(Dispatchers.IO) {
        val results = Collections.synchronizedList(mutableListOf<HomeFeedItem>())
        val blocks = SearchFilters.getEntertainmentBlocks().shuffled()
        
        for (query in blocks.take(3)) {
            try {
                withTimeout(8000) {
                    val searchTerm = query.substringAfter("(").substringBefore(")").split("|").shuffled().firstOrNull() ?: "trending"
                    val searchResult = SearchRepository.searchVideos(searchTerm)
                    if (searchResult.items.isNotEmpty()) {
                        results.addAll(searchResult.items)
                    }
                }
            } catch (_: Exception) { }
            if (results.isNotEmpty()) break
        }
        
        if (results.isEmpty()) {
            try {
                results.addAll(SearchRepository.searchVideos("trending").items)
            } catch (_: Exception) {}
        }
        
        results.distinctBy { it.videoId }.take(25)
    }
}
