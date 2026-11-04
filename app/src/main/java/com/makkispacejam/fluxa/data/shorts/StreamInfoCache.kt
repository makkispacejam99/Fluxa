package com.makkispacejam.fluxa.data.shorts

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

internal object StreamInfoCache {

    private const val TTL_MS = 20 * 60 * 1000L
    private const val MAX_ENTRIES = 64

    data class RelatedSnapshot(
        val seedChannelId: String,
        val seedTitle: String,
        val related: List<StreamInfoItem>
    )

    private val cache = LinkedHashMap<String, Pair<Long, RelatedSnapshot>>(0, 0.75f, true)
    private val mutex = Mutex()

    fun peek(videoId: String): RelatedSnapshot? {
        val hit = synchronized(cache) { cache[videoId] }
        if (hit == null) return null
        if (System.currentTimeMillis() - hit.first > TTL_MS) {
            synchronized(cache) { cache.remove(videoId) }
            return null
        }
        return hit.second
    }

    suspend fun relatedOf(videoId: String, timeoutMs: Long): RelatedSnapshot? {
        peek(videoId)?.let { return it }

        val snapshot = withTimeoutOrNull(timeoutMs) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val info = StreamInfo.getInfo(
                        ServiceList.YouTube,
                        "https://www.youtube.com/watch?v=$videoId"
                    )
                    val related = info.relatedItems.filterIsInstance<StreamInfoItem>()
                    if (related.isEmpty()) {
                        null
                    } else {
                        RelatedSnapshot(
                            seedChannelId = ShortsClassifier.normalizeChannelId(info.uploaderUrl),
                            seedTitle = info.name ?: "",
                            related = related
                        )
                    }
                }.getOrElse {
                    Log.d("FluxaShorts", "related no disponibles para $videoId: ${it.message}")
                    null
                }
            }
        }

        if (snapshot != null) {
            mutex.withLock {
                synchronized(cache) {
                    cache[videoId] = System.currentTimeMillis() to snapshot
                    while (cache.size > MAX_ENTRIES) {
                        val oldest = cache.entries.minByOrNull { it.value.first }?.key ?: break
                        cache.remove(oldest)
                    }
                }
            }
        }
        return snapshot
    }

    fun clear() {
        synchronized(cache) { cache.clear() }
    }
}
