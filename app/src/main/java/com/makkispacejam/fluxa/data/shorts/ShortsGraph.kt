@file:Suppress("SuppressedCancellationException")

package com.makkispacejam.fluxa.data.shorts

import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.models.VideoModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.stream.StreamInfoItem

enum class ShortSource(val weight: Double) {
    RELATED_L1(1.0),
    RELATED_L2(0.62),
    CHANNEL_RELATED(0.72),
    TOPIC_SEARCH(0.78),
    SUBSCRIPTION(0.85),
    SESSION_POOL(0.70)
}

data class SeedCandidate(
    val videoId: String,
    val title: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val weight: Double = 1.0
)

data class SeedFrontier(
    val seed: SeedCandidate,
    val child: ScoredShort,
    val childId: String
)

data class ScoredShort(
    val video: VideoModel,
    val score: Double,
    val source: ShortSource,
    val originChannelId: String = "",
    val originTitle: String = "",
    val originUrl: String = ""
)

object ShortsGraph {

    private const val MAX_RELATED_TIMEOUT_MS = 4500L

    suspend fun expand(
        seeds: List<SeedCandidate>,
        maxLevel2PerSeed: Int = 3,
        maxLevel2Total: Int = 10,
        maxConcurrent: Int = 6,
        onLevel1: (List<ScoredShort>) -> Unit = {},
        accept: (StreamInfoItem) -> Boolean = { true }
    ): List<ScoredShort> = coroutineScope {
        if (seeds.isEmpty()) return@coroutineScope emptyList()

        val gate = Semaphore(maxConcurrent)
        val level1 = mutableListOf<ScoredShort>()
        val visited = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
        val frontier = java.util.Collections.synchronizedList(mutableListOf<SeedFrontier>())

        val batches: List<Pair<List<ScoredShort>, List<SeedFrontier>>> = seeds.map { seed ->
            async(Dispatchers.IO) {
                gate.withPermit {
                    val snapshot = StreamInfoCache.relatedOf(seed.videoId, MAX_RELATED_TIMEOUT_MS)
                    if (snapshot == null) {
                        emptyList<ScoredShort>() to emptyList<SeedFrontier>()
                    } else {
                        val local = mutableListOf<ScoredShort>()
                        val localFrontier = mutableListOf<SeedFrontier>()

                        snapshot.related.forEach { item ->
                            val id = VideoExtractor.cleanVideoId(item.url ?: "")
                            if (id.isBlank() || id == seed.videoId) return@forEach
                            if (!visited.add(id)) return@forEach
                            if (!accept(item)) return@forEach

                            val video = toVideoModel(item, "Related")
                            val scored = ScoredShort(
                                video = video,
                                score = seed.weight * ShortSource.RELATED_L1.weight,
                                source = ShortSource.RELATED_L1,
                                originChannelId = snapshot.seedChannelId.ifBlank { seed.channelId },
                                originTitle = snapshot.seedTitle.ifBlank { seed.title },
                                originUrl = shortsUrl(id)
                            )
                            local.add(scored)
                            localFrontier.add(SeedFrontier(seed, scored, id))
                        }
                        local to localFrontier
                    }
                }.also { batch ->
                    if (batch.first.isNotEmpty()) onLevel1(batch.first)
                }
            }
        }.awaitAll()

        batches.forEach { (items, nextFrontier) ->
            level1.addAll(items)
            frontier.addAll(nextFrontier)
        }

        val level2 = expandLevel2(frontier, maxLevel2PerSeed, maxLevel2Total, maxConcurrent, visited, accept)

        level1 + level2
    }

    private suspend fun expandLevel2(
        frontier: List<SeedFrontier>,
        maxPerSeed: Int,
        maxTotal: Int,
        maxConcurrent: Int,
        visited: MutableSet<String>,
        accept: (StreamInfoItem) -> Boolean
    ): List<ScoredShort> = coroutineScope {
        if (frontier.isEmpty() || maxTotal <= 0) return@coroutineScope emptyList()

        val perSeedCount = HashMap<String, Int>()
        val picks = frontier
            .sortedByDescending { it.child.score }
            .filter { node ->
                val key = node.seed.videoId
                val used = perSeedCount.getOrDefault(key, 0)
                if (used >= maxPerSeed) return@filter false
                perSeedCount[key] = used + 1
                true
            }
            .take(maxTotal)

        if (picks.isEmpty()) return@coroutineScope emptyList()

        val gate = Semaphore(maxConcurrent)
        val results = mutableListOf<ScoredShort>()

        val batches: List<List<ScoredShort>> = picks.map { node ->
            async(Dispatchers.IO) {
                gate.withPermit {
                    val seed = node.seed
                    val parentId = node.childId
                    val snapshot = StreamInfoCache.relatedOf(parentId, MAX_RELATED_TIMEOUT_MS)
                        ?: return@withPermit emptyList<ScoredShort>()

                    snapshot.related.mapNotNull { item ->
                        val id = VideoExtractor.cleanVideoId(item.url ?: "")
                        if (id.isBlank() || id == parentId || id == seed.videoId) return@mapNotNull null
                        if (!visited.add(id)) return@mapNotNull null
                        if (!accept(item)) return@mapNotNull null

                        ScoredShort(
                            video = toVideoModel(item, "Related"),
                            score = seed.weight * ShortSource.RELATED_L2.weight * 0.9,
                            source = ShortSource.RELATED_L2,
                            originChannelId = snapshot.seedChannelId.ifBlank { seed.channelId },
                            originTitle = snapshot.seedTitle.ifBlank { seed.title },
                            originUrl = shortsUrl(id)
                        )
                    }
                }
            }
        }.awaitAll()

        batches.forEach { results.addAll(it) }

        results
    }

    fun toVideoModel(item: StreamInfoItem, origin: String): VideoModel {
        val rawUrl = item.url ?: ""
        val id = VideoExtractor.cleanVideoId(rawUrl)
        return VideoModel(
            id = id,
            title = item.name ?: "",
            channelName = item.uploaderName ?: "",
            channelId = ShortsClassifier.normalizeChannelId(item.uploaderUrl),
            imageUrl = item.thumbnails.firstOrNull()?.url
                ?: if (id.isNotBlank()) "https://img.youtube.com/vi/$id/hqdefault.jpg" else "",
            videoUrl = origin,
            timestamp = try { item.uploadDate?.instant?.toEpochMilli() ?: 0L } catch (_: Exception) { 0L },
            viewCount = try { item.viewCount } catch (_: Exception) { 0L },
            duration = try { item.duration } catch (_: Exception) { 0L }
        )
    }

    fun shortsUrl(videoId: String): String = "https://www.youtube.com/shorts/$videoId"

    fun isUsableRelated(item: StreamInfoItem): Boolean {
        if (item.contentAvailability != ContentAvailability.AVAILABLE) return false
        val streamType = item.streamType?.name ?: ""
        if (streamType == "NONE" || streamType.contains("UPCOMING") || streamType.contains("PREMIERE")) return false
        if (streamType.contains("LIVE")) return false
        val title = item.name
        if (title.isNullOrBlank()) return false
        val duration = try { item.duration } catch (_: Exception) { 0L }
        val views = try { item.viewCount } catch (_: Exception) { 0L }
        if (ShortsClassifier.looksLikeJunk(title, duration, views)) return false
        if (ShortsClassifier.isShort(title, duration, item.url)) return true
        return runCatching { item.isShortFormContent }.getOrDefault(false)
    }
}
