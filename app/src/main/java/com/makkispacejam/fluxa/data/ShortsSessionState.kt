@file:Suppress("HasPlatformType")

package com.makkispacejam.fluxa.data

import com.makkispacejam.fluxa.data.shorts.ShortSource
import com.makkispacejam.fluxa.data.shorts.ShortsRanker
import com.makkispacejam.fluxa.data.shorts.ScoredShort
import com.makkispacejam.fluxa.models.VideoModel
import java.util.Collections

internal object ShortsSessionState {

    const val CACHE_TTL = 8 * 60 * 1000L
    const val PAGE_SIZE = 30
    const val POOL_TARGET = 260
    const val POOL_PER_CHANNEL = 8
    const val PREVIEW_MIN_CHANNELS = 8

    val hiddenShorts = Collections.synchronizedSet(mutableSetOf<String>())
    val sessionPool = Collections.synchronizedList(mutableListOf<VideoModel>())

    @Volatile
    var sessionSeed: Long = freshSeed()

    @Volatile
    private var shortsCache: List<VideoModel>? = null

    @Volatile
    private var shortsCacheTime: Long = 0L

    private fun freshSeed(): Long =
        System.nanoTime() xor java.util.Random().nextLong() xor (System.identityHashCode(sessionPool).toLong() shl 1)

    fun beginSession(): Long {
        sessionSeed = freshSeed()
        return sessionSeed
    }

    fun shuffleWithSession(items: List<VideoModel>, seed: Long): List<VideoModel> {
        if (items.size < 2) return items
        val tagged = items.map { ScoredShort(it, 0.0, ShortSource.SESSION_POOL) }
        val shuffled = ShortsRanker.shuffleRestricted(tagged, seed)
        if (shuffled.size == tagged.size) return shuffled.map { it.video }
        return items
    }

    fun freshCache(): List<VideoModel>? {
        val cached = shortsCache ?: return null
        if (System.currentTimeMillis() - shortsCacheTime >= CACHE_TTL) return null
        return cached
    }

    fun storeCache(items: List<VideoModel>) {
        shortsCache = items
        shortsCacheTime = System.currentTimeMillis()
    }

    fun invalidateCache() {
        shortsCache = null
        shortsCacheTime = 0L
    }

    fun hide(videoId: String) {
        hiddenShorts.add(videoId)
        shortsCache = shortsCache?.filter { it.id != videoId }
    }

    fun clearPool() {
        synchronized(sessionPool) { sessionPool.clear() }
    }

    fun drain(limit: Int): List<VideoModel> {
        val out = mutableListOf<VideoModel>()
        synchronized(sessionPool) {
            val iterator = sessionPool.iterator()
            while (iterator.hasNext() && out.size < limit) {
                out.add(iterator.next())
                iterator.remove()
            }
        }
        return out
    }

    fun refill(items: List<VideoModel>) {
        if (items.isEmpty()) return
        synchronized(sessionPool) {
            val known = sessionPool.mapTo(HashSet()) { it.id }
            items.forEach { if (it.id !in known) sessionPool.add(it) }
        }
    }
}
