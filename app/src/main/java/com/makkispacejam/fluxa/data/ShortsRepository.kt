@file:Suppress("USELESS_ELVIS_LEFT_IS_NULL")

package com.makkispacejam.fluxa.data

import android.content.Context
import android.util.Log
import com.makkispacejam.fluxa.data.local.FluxaDatabase
import com.makkispacejam.fluxa.models.VideoModel
import com.makkispacejam.fluxa.ui.components.system.NewPipeServerException
import com.makkispacejam.fluxa.data.shorts.ShortsCacheManager
import com.makkispacejam.fluxa.data.shorts.ShortsCache
import com.makkispacejam.fluxa.data.shorts.ShortsSource
import com.makkispacejam.fluxa.data.filters.RecentVideosTracker
import kotlinx.coroutines.*
import java.util.Collections

class ShortsRepository {

    companion object {
        private var shortsCache: List<VideoModel>? = null
        private var shortsCacheTime: Long = 0L
        private val hiddenShorts = Collections.synchronizedSet(mutableSetOf<String>())
        private const val CACHE_TTL = 10 * 60 * 1000L
        private const val MAX_PER_CHANNEL = 5
    }

    // Ocultar shorts
    fun hideShort(videoId: String) {
        hiddenShorts.add(videoId)
        shortsCache = shortsCache?.filter { it.id != videoId }
    }

    // Obtener shorts de diferentes fuentes
    suspend fun getShorts(
        context: Context,
        forceLoadMore: Boolean = false,
        forceRefresh: Boolean = false,
        onProgress: ((List<VideoModel>) -> Unit)? = null
    ): List<VideoModel> = withContext(Dispatchers.IO) {

            if (forceRefresh) {
                shortsCache = null
                shortsCacheTime = 0L
                ShortsCacheManager.clearCache()
                RecentVideosTracker.clearAll()
            } else if (!forceLoadMore) {
                shortsCache?.let {
                    if (System.currentTimeMillis() - shortsCacheTime < CACHE_TTL) {
                        val dao = FluxaDatabase.getDatabase(context).fluxaDao()
                        val blockedIds = dao.getBlockedChannelIds().toSet()
                        return@withContext it.filter { v -> v.id !in hiddenShorts && v.channelId !in blockedIds }
                    }
                }
            }

            val dao = FluxaDatabase.getDatabase(context).fluxaDao()

            val blockedChannelIdsDeferred = async { dao.getBlockedChannelIds().toSet() }
            val interactionsDeferred = async { dao.getRecentInteractions(500) }
            val watchedVideoIdsDeferred = async { dao.getAllWatchedVideoIds().toSet() }

            val cachedShortsDeferred = async {
                val cached = dao.getRecentCachedVideos(80)
                if (cached.isNotEmpty()) {
                    cached.map { ShortsCache.toVideoModel(it) }.filter { it.id !in hiddenShorts }
                } else emptyList()
            }

            val blockedChannelIds = blockedChannelIdsDeferred.await()
            val interactions = interactionsDeferred.await()
            val watchedVideoIds = watchedVideoIdsDeferred.await()
            val filterIds = interactions.filter { it.isDisliked }.map { it.videoId }.toSet() +
                    watchedVideoIds + ShortsCacheManager.getSeenIds() + hiddenShorts
            val dislikedChannelNames = interactions.filter { it.isDisliked }.mapNotNull { it.channelName }.toSet()

            val cachedShorts = cachedShortsDeferred.await()
            val dislikedChannelIdsFromCached = cachedShorts.filter { it.channelName in dislikedChannelNames }.mapNotNull { it.channelId }.toSet()
            if (cachedShorts.isNotEmpty()) {
                val filteredCached = cachedShorts.filter { it.id !in filterIds && it.channelId !in blockedChannelIds }
                if (filteredCached.isNotEmpty()) {
                    onProgress?.invoke(filteredCached.shuffled())
                }
            }

            val genericShortsDeferred = async { ShortsSource.getGenericShorts(blockedChannelIds, dislikedChannelNames) }

            val subShorts = try { ShortsSource.getSubscriptionShorts(dao, blockedChannelIds) } catch(e: Exception) { Log.e("FluxaShorts", "fallo al obtener shorts de las suscripciones", e); emptyList() }
            val dislikedChannelIdsFromSubs = subShorts.filter { it.channelName in dislikedChannelNames }.mapNotNull { it.channelId }.toSet()
            val dislikedChannelIds = dislikedChannelIdsFromCached + dislikedChannelIdsFromSubs

            val initialShorts = subShorts.filter { it.id !in filterIds && it.channelId !in blockedChannelIds && it.channelId !in dislikedChannelIds }
            if (initialShorts.isNotEmpty()) {
                onProgress?.invoke(initialShorts.shuffled())
            }

            val seedVideoIds = subShorts.distinctBy { it.channelId }.shuffled().take(8).map { it.id }
            val likedSeedIds = interactions.filter { it.isLiked }.map { it.videoId }.takeLast(10).distinct()
            val allSeeds = (seedVideoIds + likedSeedIds).shuffled().take(8)
            val channelSimilarShortsDeferred = async {
                if (allSeeds.isNotEmpty()) ShortsSource.getChannelSimilarShorts(allSeeds, subShorts.mapNotNull { it.channelId }.toSet(), blockedChannelIds, dislikedChannelNames) else emptyList()
            }

            val channelSimilarShorts = try { channelSimilarShortsDeferred.await() } catch(e: Exception) { Log.e("FluxaShorts", "fallo al obtener shorts similares de canales", e); emptyList() }
            val genericShorts = try { genericShortsDeferred.await() } catch(e: Exception) { Log.e("FluxaShorts", "fallo al obtener shorts genericos", e); emptyList() }

            val allDislikedChannelIds = dislikedChannelIds +
                    channelSimilarShorts.filter { it.channelName in dislikedChannelNames }.mapNotNull { it.channelId }.toSet() +
                    genericShorts.filter { it.channelName in dislikedChannelNames }.mapNotNull { it.channelId }.toSet()

            val finalMix = interleaveShorts(subShorts, channelSimilarShorts, genericShorts, filterIds, blockedChannelIds, dislikedChannelNames, allDislikedChannelIds)

            if (finalMix.isEmpty()) {
                val fallback = genericShorts.ifEmpty { ShortsSource.getGenericShorts(blockedChannelIds, dislikedChannelNames) }
                if (fallback.isEmpty()) throw NewPipeServerException()
                return@withContext fallback.filter { it.id !in hiddenShorts && it.channelId !in blockedChannelIds }
            }

            if (finalMix.size < 30) {
                val fillers = (channelSimilarShorts + genericShorts + subShorts).shuffled()
                    .filter { it.id !in (finalMix.map { m -> m.id }.toSet() + filterIds) && it.channelId !in blockedChannelIds && it.channelId !in allDislikedChannelIds && it.channelName !in dislikedChannelNames }
                    .distinctBy { it.id }
                return@withContext (finalMix + fillers).take(60).shuffled()
            }

            val result = finalMix.take(80).shuffled()
            if (!forceLoadMore) {
                shortsCache = result
                shortsCacheTime = System.currentTimeMillis()
            }

            ShortsCache.saveToDiskCache(dao, result)

            finalMix.take(60).shuffled()
        }

    // Mezcla de fuentes
    private fun interleaveShorts(
        subShorts: List<VideoModel>,
        similarShorts: List<VideoModel>,
        genericShorts: List<VideoModel>,
        filterIds: Set<String>,
        blockedChannelIds: Set<String>,
        dislikedChannelNames: Set<String> = emptySet(),
        dislikedChannelIds: Set<String> = emptySet()
    ): List<VideoModel> {
        val subQueue = ArrayDeque(
            subShorts.filter { it.id !in filterIds && it.channelId !in blockedChannelIds && it.channelId !in dislikedChannelIds && it.channelName !in dislikedChannelNames && !RecentVideosTracker.isRecentlySeen(it.id) }
        )
        val similarQueue = ArrayDeque(
            similarShorts.filter { it.id !in filterIds && it.channelId !in blockedChannelIds && it.channelId !in dislikedChannelIds && it.channelName !in dislikedChannelNames && !RecentVideosTracker.isRecentlySeen(it.id) }
        )
        val genericQueue = ArrayDeque(
            genericShorts.filter { it.id !in filterIds && it.channelId !in blockedChannelIds && it.channelId !in dislikedChannelIds && it.channelName !in dislikedChannelNames && !RecentVideosTracker.isRecentlySeen(it.id) }
        )

        val result = mutableListOf<VideoModel>()
        val seenIds = mutableSetOf<String>()
        val channelCount = mutableMapOf<String, Int>()

        fun tryAdd(video: VideoModel): Boolean {
            if (video.id in seenIds) return false
            val chCount = channelCount[video.channelId ?: ""] ?: 0
            if (chCount >= MAX_PER_CHANNEL) return false
            result.add(video)
            seenIds.add(video.id)
            channelCount[video.channelId ?: ""] = chCount + 1
            return true
        }

        val queues = listOf(
            ArrayDeque(subQueue.shuffled()),
            ArrayDeque(similarQueue.shuffled()),
            ArrayDeque(genericQueue.shuffled())
        )

        val pattern = (0 until 60).map { it % 3 }.shuffled()
        var subDone = 0
        val maxSubInRow = 1

        for (source in pattern) {
            if (queues.all { it.isEmpty() }) break
            if (result.size >= 60) break

            if (queues[source].isNotEmpty()) {
                if (source == 0) {
                    if (subDone < maxSubInRow) {
                        if (tryAdd(queues[source].removeFirst())) subDone++
                    } else {
                        val fallback = queues.indices.firstOrNull { it != 0 && queues[it].isNotEmpty() }
                        if (fallback != null) {
                            tryAdd(queues[fallback].removeFirst())
                        } else if (queues[source].isNotEmpty()) {
                            tryAdd(queues[source].removeFirst())
                        }
                        subDone = 0
                    }
                } else {
                    if (tryAdd(queues[source].removeFirst())) subDone = 0
                }
            } else {
                for (i in queues.indices) {
                    if (queues[i].isNotEmpty()) {
                        tryAdd(queues[i].removeFirst())
                        break
                    }
                }
            }
        }

        while (result.size < 60 && queues.any { it.isNotEmpty() }) {
            for (q in queues) {
                if (q.isNotEmpty() && result.size >= 60) break
                while (q.isNotEmpty() && result.size < 60) {
                    tryAdd(q.removeFirst())
                }
            }
        }

        return result
    }
}
