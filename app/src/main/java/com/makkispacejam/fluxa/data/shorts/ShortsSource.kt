package com.makkispacejam.fluxa.data.shorts

import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.data.channels.ChannelDataExtractor
import com.makkispacejam.fluxa.data.local.FluxaDao
import com.makkispacejam.fluxa.data.filters.SearchFilters
import com.makkispacejam.fluxa.models.VideoModel
import kotlinx.coroutines.*
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.Collections

object ShortsSource {

    private const val MAX_SHORT_DURATION = 100L

    // Obtener shorts de suscripciones
    suspend fun getSubscriptionShorts(dao: FluxaDao, blockedChannelIds: Set<String> = emptySet()): List<VideoModel> = coroutineScope {
        val subs = dao.getAllSubscriptions()?.shuffled()?.take(20)
            ?.filter { it.channelId !in blockedChannelIds }
            ?: return@coroutineScope emptyList()
        val results = Collections.synchronizedList(mutableListOf<VideoModel>())

        subs.map { sub ->
            async(Dispatchers.IO) {
                try {
                    withTimeout(4000) {
                        val channelUrl = "https://www.youtube.com/channel/${sub.channelId}"
                        var streams = try {
                            ChannelDataExtractor.getChannelShorts(channelUrl, sub.channelName ?: "", sub.channelId, sub.avatarUrl ?: "")
                        } catch (_: Exception) { emptyList() }

                        if (streams.isEmpty()) {
                            streams = try {
                                ChannelDataExtractor.getChannelVideos(channelUrl)
                                    .filter { it.duration in 1..100 }
                            } catch (_: Exception) { emptyList() }
                        }

                        val items = streams.shuffled().take(5).map { item ->
                            VideoModel(
                                id = VideoExtractor.cleanVideoId(item.url),
                                title = item.title,
                                channelName = item.uploaderName,
                                channelId = item.channelId.ifBlank { sub.channelId },
                                channelAvatarUrl = item.uploaderAvatar.ifBlank { sub.avatarUrl ?: "" },
                                imageUrl = item.thumbnail,
                                videoUrl = "Subscription",
                                timestamp = item.timestamp,
                                viewCount = item.views
                            )
                        }
                        results.addAll(items)
                    }
                } catch (_: Exception) {}
            }
        }.awaitAll()
        results
    }

    // Obtener shorts similares a subs
    suspend fun getChannelSimilarShorts(
        seedIds: List<String>,
        subChannelIds: Set<String> = emptySet(),
        blockedChannelIds: Set<String> = emptySet(),
        dislikedChannelNames: Set<String> = emptySet()
    ): List<VideoModel> = withContext(Dispatchers.IO) {
        val results = Collections.synchronizedList(mutableListOf<VideoModel>())
        val seeds = seedIds.distinct().shuffled().take(8)

        seeds.map { seedId ->
            async(Dispatchers.IO) {
                try {
                    withTimeout(2500) {
                        val info = StreamInfo.getInfo(ServiceList.YouTube, "https://www.youtube.com/watch?v=$seedId")
                        val seedChannelId = info.uploaderUrl?.substringAfter("/channel/") ?: ""

                        val related = info.relatedItems.filterIsInstance<StreamInfoItem>()
                            .filter { it.duration in 1..MAX_SHORT_DURATION }
                            .filter { it.viewCount >= 1000 }
                            .filter { item ->
                                val itemChannelId = item.uploaderUrl?.substringAfter("/channel/") ?: ""
                                itemChannelId != seedChannelId && itemChannelId !in subChannelIds && itemChannelId !in blockedChannelIds && item.uploaderName !in dislikedChannelNames
                            }

                        val titleWords = (info.name ?: "")
                            .replace(Regex("[^a-zA-ZáéíóúñÁÉÍÓÚÑ ]"), "")
                            .split(" ")
                            .filter { it.length >= 4 }
                            .shuffled()
                            .take(3)

                        val searchResults = mutableListOf<StreamInfoItem>()
                        for (word in titleWords) {
                            try {
                                val service = ServiceList.YouTube
                                val handler = service.searchQHFactory.fromQuery("$word shorts", listOf("videos"), "")
                                val extractor = service.getSearchExtractor(handler)
                                extractor.fetchPage()
                                val items = extractor.initialPage.items.asSequence()
                                    .filterIsInstance<StreamInfoItem>()
                                    .filter { it.duration in 1..MAX_SHORT_DURATION }
                                    .filter { it.viewCount >= 1000 }
                                    .filter { item ->
                                        val itemChannelId = item.uploaderUrl?.substringAfter("/channel/") ?: ""
                                        itemChannelId !in subChannelIds && itemChannelId !in blockedChannelIds && item.uploaderName !in dislikedChannelNames
                                    }
                                    .take(5).toList()
                                searchResults.addAll(items)
                            } catch (_: Exception) {}
                        }

                        (related + searchResults).map { item ->
                            val id = VideoExtractor.cleanVideoId(item.url)
                            VideoModel(
                                id = id,
                                title = item.name ?: "",
                                channelName = item.uploaderName ?: "",
                                channelId = item.uploaderUrl?.substringAfter("/channel/") ?: "",
                                imageUrl = item.thumbnails.firstOrNull()?.url ?: "",
                                videoUrl = "Similar",
                                timestamp = try { item.uploadDate?.instant?.toEpochMilli() ?: 0L } catch(_: Exception) { 0L },
                                viewCount = item.viewCount
                            )
                        }.let { results.addAll(it) }
                    }
                } catch (_: Exception) {}
            }
        }.awaitAll()
        results.distinctBy { it.id }
            .distinctBy { it.channelId }
            .sortedByDescending { scoreRelevance(it) }
    }

    // Obtener shorts genéricos
    suspend fun getGenericShorts(
        blockedChannelIds: Set<String> = emptySet(),
        dislikedChannelNames: Set<String> = emptySet()
    ): List<VideoModel> = withContext(Dispatchers.IO) {
        val results = Collections.synchronizedList(mutableListOf<VideoModel>())
        val categories = SearchFilters.getEntertainmentBlocks().shuffled().take(4)

        categories.map { category ->
            async(Dispatchers.IO) {
                try {
                    withTimeout(2500) {
                        val queryPool = category.substringAfter("(").substringBefore(")")
                            .split("|").map { it.trim() }
                        val query = queryPool.shuffled().take(3).joinToString(" ")

                        val service = ServiceList.YouTube
                        val searchExtractor = service.getSearchExtractor(service.searchQHFactory.fromQuery("$query shorts", listOf("videos"), ""))
                        searchExtractor.fetchPage()
                        val items = searchExtractor.initialPage.items.asSequence().filterIsInstance<StreamInfoItem>()
                            .filter { it.duration in 1..MAX_SHORT_DURATION }
                            .filter { it.viewCount >= 5000 }
                            .filter { item ->
                                val itemChannelId = item.uploaderUrl?.substringAfter("/channel/") ?: ""
                                itemChannelId !in blockedChannelIds && item.uploaderName !in dislikedChannelNames
                            }
                            .map { item ->
                                val id = VideoExtractor.cleanVideoId(item.url)
                                VideoModel(
                                    id = id,
                                    title = item.name ?: "",
                                    channelName = item.uploaderName ?: "",
                                    channelId = item.uploaderUrl?.substringAfter("/channel/") ?: "",
                                    imageUrl = item.thumbnails.firstOrNull()?.url ?: "",
                                    videoUrl = "Trending",
                                    timestamp = try { item.uploadDate?.instant?.toEpochMilli() ?: 0L } catch(_: Exception) { 0L },
                                    viewCount = item.viewCount
                                )
                            }.toList()
                        results.addAll(items)
                    }
                } catch (_: Exception) {}
            }
        }.awaitAll()
        results.distinctBy { it.id }
            .distinctBy { it.channelId }
            .sortedByDescending { scoreRelevance(it) }
    }

    // Sistema de puntaje
    fun scoreRelevance(video: VideoModel): Long {
        val views = video.viewCount
        val age = System.currentTimeMillis() - video.timestamp
        val hoursOld = age / (1000 * 60 * 60)
        val recencyBonus = when {
            hoursOld < 24 -> 3L
            hoursOld < 72 -> 2L
            hoursOld < 168 -> 1L
            else -> 0L
        }
        return views * (1 + recencyBonus)
    }
}
