package com.makkispacejam.fluxa.data.shorts

import android.util.Log
import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.data.channels.ChannelDataExtractor
import com.makkispacejam.fluxa.data.filters.SearchFilters
import com.makkispacejam.fluxa.data.local.FluxaDao
import com.makkispacejam.fluxa.data.newpipe.FluxaStreamItem
import com.makkispacejam.fluxa.models.VideoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfoItem

object ShortsSource {

    private const val TAG = "FluxaShorts"
    private const val RELATED_SEEDS_PER_EMPTY_CHANNEL = 2
    private const val RELATED_TIMEOUT_MS = 4500L

    private class ChannelFetch(
        val shorts: List<FluxaStreamItem>,
        val seeds: List<FluxaStreamItem>
    )

    suspend fun getSubscriptionShorts(
        dao: FluxaDao,
        blockedChannelIds: Set<String> = emptySet(),
        maxChannels: Int = 30,
        perChannel: Int = 9,
        maxConcurrent: Int = 8,
        timeoutMs: Long = 6000,
        overallTimeoutMs: Long = 12_000,
        minTotal: Int = 40,
        expandEmptyChannels: Boolean = true,
        relatedPerChannel: Int = 6,
        onBatch: (List<ScoredShort>) -> Unit = {}
    ): List<ScoredShort> = coroutineScope {
        val subs = dao.getAllSubscriptions()
            .orEmpty()
            .filter { it.channelId !in blockedChannelIds }
        if (subs.isEmpty()) return@coroutineScope emptyList()

        val gate = Semaphore(maxConcurrent)
        val collected = java.util.Collections.synchronizedList(mutableListOf<ScoredShort>())
        val seenIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
        val start = kotlin.random.Random.nextInt(subs.size)
        val deadline = System.currentTimeMillis() + overallTimeoutMs
        val waveSize = minOf(maxChannels, subs.size)

        var wave = 0
        while (collected.size < minTotal && wave * waveSize < subs.size) {
            val slice = (0 until waveSize)
                .map { subs[(it + start + wave * waveSize) % subs.size] }
            if (slice.isEmpty()) break

            val workers = slice.map { sub ->
                async(Dispatchers.IO) {
                    gate.withPermit {
                        val channelId = sub.channelId
                        val channelName = sub.channelName ?: ""
                        val fetch = runCatching {
                            withTimeout(timeoutMs) {
                                fetchChannelShorts(channelId, channelName, sub.avatarUrl ?: "")
                            }
                        }.getOrElse {
                            Log.d(TAG, "canal $channelName sin shorts: ${it.message}")
                            ChannelFetch(emptyList(), emptyList())
                        }

                        val mapped = fetch.shorts
                            .asSequence()
                            .mapNotNull { toScored(it, channelId, channelName) }
                            .filter { it.video.id !in seenIds }
                            .filter { ShortsClassifier.isShort(it.video.title, it.video.duration, it.originUrl) }
                            .sortedByDescending { ShortsClassifier.formatScore(it.video.title, it.video.duration, it.originUrl) }
                            .take(perChannel)
                            .toList()

                        if (mapped.isNotEmpty()) {
                            synchronized(seenIds) { mapped.forEach { seenIds.add(it.video.id) } }
                            collected.addAll(mapped)
                            onBatch(mapped)
                        } else if (expandEmptyChannels) {
                            val related = runCatching {
                                expandChannelRelated(
                                    channelId = channelId,
                                    seeds = fetch.seeds,
                                    blockedChannelIds = blockedChannelIds,
                                    perChannel = relatedPerChannel
                                )
                            }.getOrElse {
                                Log.d(TAG, "relacionados de $channelName fallo: ${it.message}")
                                emptyList()
                            }
                            val fresh = related.filter { it.video.id !in seenIds }
                            if (fresh.isNotEmpty()) {
                                synchronized(seenIds) { fresh.forEach { seenIds.add(it.video.id) } }
                                collected.addAll(fresh)
                                onBatch(fresh)
                            }
                        }
                    }
                }
            }

            withTimeoutOrNull((deadline - System.currentTimeMillis()).coerceAtLeast(500L)) {
                workers.awaitAll()
            }
            wave++
        }

        Log.d(TAG, "suscripciones: pool=${collected.size} oleadas=$wave subs=${subs.size}")
        collected.toList()
    }

    private suspend fun expandChannelRelated(
        channelId: String,
        seeds: List<FluxaStreamItem>,
        blockedChannelIds: Set<String>,
        perChannel: Int
    ): List<ScoredShort> {
        if (seeds.isEmpty() || channelId.isBlank()) return emptyList()
        val out = mutableListOf<ScoredShort>()
        val localSeen = HashSet<String>()

        seeds.take(RELATED_SEEDS_PER_EMPTY_CHANNEL).forEach { seed ->
            if (out.size >= perChannel) return@forEach
            val seedId = VideoExtractor.cleanVideoId(seed.url)
            if (seedId.isBlank()) return@forEach
            val snapshot = runCatching { StreamInfoCache.relatedOf(seedId, RELATED_TIMEOUT_MS) }
                .getOrNull() ?: return@forEach

            snapshot.related.forEach { item ->
                if (out.size >= perChannel) return@forEach
                val id = VideoExtractor.cleanVideoId(item.url ?: "")
                if (id.isBlank() || id == seedId) return@forEach
                if (!localSeen.add(id)) return@forEach
                if (!ShortsGraph.isUsableRelated(item)) return@forEach
                val uploaderChannel = ShortsClassifier.normalizeChannelId(item.uploaderUrl)
                if (uploaderChannel.isNotEmpty() && uploaderChannel in blockedChannelIds) return@forEach
                if (channelId == uploaderChannel) return@forEach

                out.add(
                    ScoredShort(
                        video = ShortsGraph.toVideoModel(item, "Related"),
                        score = ShortSource.CHANNEL_RELATED.weight,
                        source = ShortSource.CHANNEL_RELATED,
                        originChannelId = channelId,
                        originTitle = seed.title,
                        originUrl = ShortsGraph.shortsUrl(id)
                    )
                )
            }
        }
        return out
    }

    private suspend fun fetchChannelShorts(
        channelId: String,
        channelName: String,
        avatarUrl: String
    ): ChannelFetch {
        if (channelId.isBlank()) return ChannelFetch(emptyList(), emptyList())
        val channelUrl = "https://www.youtube.com/channel/$channelId"
        val shorts = runCatching {
            ChannelDataExtractor.getChannelShorts(channelUrl, channelName, channelId, avatarUrl)
        }.getOrElse {
            Log.d(TAG, "pestana /shorts fallo para $channelName: ${it.message}")
            emptyList()
        }
        if (shorts.isNotEmpty()) return ChannelFetch(shorts, shorts)

        val videos = runCatching {
            ChannelDataExtractor.getChannelVideos(channelUrl)
        }.getOrElse { emptyList() }
        return ChannelFetch(videos.filter { ShortsClassifier.isShortLenient(it.title, it.duration) }, videos)
    }

    private fun toScored(
        item: FluxaStreamItem,
        fallbackChannelId: String,
        fallbackChannelName: String
    ): ScoredShort? {
        val id = VideoExtractor.cleanVideoId(item.url)
        if (id.isBlank() || item.title.isBlank()) return null
        val safeDuration = if (item.duration < 0L) 0L else item.duration
        val video = VideoModel(
            id = id,
            title = item.title,
            channelName = item.uploaderName.ifBlank { fallbackChannelName },
            channelId = item.channelId.ifBlank { fallbackChannelId },
            channelAvatarUrl = item.uploaderAvatar,
            imageUrl = item.thumbnail.ifBlank {
                "https://img.youtube.com/vi/$id/hqdefault.jpg"
            },
            videoUrl = "Subscription",
            timestamp = item.timestamp,
            viewCount = if (item.views < 0L) 0L else item.views,
            duration = safeDuration
        )
        return ScoredShort(
            video = video,
            score = ShortSource.SUBSCRIPTION.weight,
            source = ShortSource.SUBSCRIPTION,
            originChannelId = fallbackChannelId,
            originTitle = item.title,
            originUrl = shortsUrl(id)
        )
    }

    private fun shortsUrl(videoId: String): String = "https://www.youtube.com/shorts/$videoId"

    fun buildTopicQueries(titles: List<String>, limit: Int = 6): List<String> {
        val hashtagQueries = mutableListOf<String>()
        val tagCounts = HashMap<String, Int>()
        titles.forEach { title ->
            ShortsClassifier.hashtags(title).forEach { tag ->
                if (tag == "short" || tag == "shorts") return@forEach
                tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
            }
        }
        tagCounts.entries
            .sortedByDescending { it.value }
            .take(limit / 2)
            .forEach { hashtagQueries.add("#${it.key} #shorts") }

        val wordCounts = HashMap<String, Int>()
        titles.forEach { title ->
            ShortsClassifier.keywordTokens(title).forEach { token ->
                wordCounts[token] = (wordCounts[token] ?: 0) + 1
            }
        }
        val words = wordCounts.entries
            .sortedByDescending { it.value }
            .take(limit - hashtagQueries.size)
            .map { it.key }

        words.forEach { word -> hashtagQueries.add("$word #shorts") }

        return hashtagQueries.distinct().take(limit)
    }

    suspend fun getTopicShorts(
        queries: List<String>,
        blockedChannelIds: Set<String>,
        dislikedChannelNames: Set<String>,
        perQueryLimit: Int = 14,
        timeoutMs: Long = 4000,
        maxConcurrent: Int = 4
    ): List<ScoredShort> = coroutineScope {
        if (queries.isEmpty()) return@coroutineScope emptyList()
        val gate = Semaphore(maxConcurrent)
        val collected = java.util.Collections.synchronizedList(mutableListOf<ScoredShort>())

        queries.map { query ->
            async(Dispatchers.IO) {
                gate.withPermit {
                    val items = runCatching {
                        withTimeout(timeoutMs) {
                            searchVideos(query)
                        }
                    }.getOrElse {
                        Log.d(TAG, "busqueda '$query' fallo: ${it.message}")
                        emptyList()
                    }

                    val mapped = items
                        .mapNotNull { toScored(it) }
                        .filter {
                            it.video.channelId !in blockedChannelIds &&
                                    it.video.channelName !in dislikedChannelNames
                        }
                        .filter { ShortsClassifier.isShort(it.video.title, it.video.duration, null) }
                        .take(perQueryLimit)

                    collected.addAll(mapped)
                }
            }
        }.awaitAll()

        collected.toList()
    }

    suspend fun getDiscoveryShorts(
        blockedChannelIds: Set<String>,
        dislikedChannelNames: Set<String>,
        queryCount: Int = 3,
        timeoutMs: Long = 4000
    ): List<ScoredShort> = coroutineScope {
        val baseQueries = SearchFilters.getEntertainmentBlocks().shuffled().take(queryCount).map { block ->
            block.substringAfter("(").substringBefore(")")
                .split("|").map { it.trim() }.shuffled().firstOrNull() ?: "curiosidades"
        }
        val queries = baseQueries.map { "$it #shorts" }

        val gate = Semaphore(queries.size.coerceAtLeast(1))
        val collected = java.util.Collections.synchronizedList(mutableListOf<ScoredShort>())

        queries.map { query ->
            async(Dispatchers.IO) {
                gate.withPermit {
                    val items = runCatching {
                        withTimeout(timeoutMs) { searchVideos(query) }
                    }.getOrElse { emptyList() }

                    collected.addAll(
                        items.asSequence().mapNotNull { toScored(it) }
                            .filter {
                                it.video.channelId !in blockedChannelIds &&
                                        it.video.channelName !in dislikedChannelNames
                            }
                            .filter { ShortsClassifier.isShort(it.video.title, it.video.duration, null) }
                            .sortedByDescending {
                                ShortsClassifier.freshnessScore(it.video.timestamp) *
                                        ShortsClassifier.formatScore(it.video.title, it.video.duration, null)
                            }
                            .take(16).toList()
                    )
                }
            }
        }.awaitAll()

        collected.toList()
    }

    private fun searchVideos(query: String): List<StreamInfoItem> {
        val service = ServiceList.YouTube
        val handler = service.searchQHFactory.fromQuery(query, listOf("videos"), "")
        val extractor = service.getSearchExtractor(handler)
        extractor.fetchPage()
        return extractor.initialPage.items.filterIsInstance<StreamInfoItem>()
    }

    private fun toScored(item: StreamInfoItem): ScoredShort? {
        val id = VideoExtractor.cleanVideoId(item.url ?: "")
        val title = item.name ?: ""
        if (id.isBlank() || title.isBlank()) return null
        val duration = try { item.duration } catch (_: Exception) { 0L }
        val views = try { item.viewCount } catch (_: Exception) { 0L }
        if (ShortsClassifier.looksLikeJunk(title, duration, views)) return null

        val video = ShortsGraph.toVideoModel(item, "Topic")
        return ScoredShort(
            video = video,
            score = ShortSource.TOPIC_SEARCH.weight,
            source = ShortSource.TOPIC_SEARCH,
            originChannelId = video.channelId.orEmpty(),
            originTitle = title
        )
    }
}
