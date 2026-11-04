package com.makkispacejam.fluxa.data

import com.makkispacejam.fluxa.data.filters.RecentVideosTracker
import com.makkispacejam.fluxa.data.local.FluxaDao
import com.makkispacejam.fluxa.data.shorts.ShortSource
import com.makkispacejam.fluxa.data.shorts.ShortsCache
import com.makkispacejam.fluxa.data.shorts.ShortsClassifier
import com.makkispacejam.fluxa.data.shorts.ShortsGraph
import com.makkispacejam.fluxa.data.shorts.ShortsRanker
import com.makkispacejam.fluxa.data.shorts.ShortsSeenRegistry
import com.makkispacejam.fluxa.data.shorts.ScoredShort
import com.makkispacejam.fluxa.models.VideoModel
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.util.Collections

internal class FeedSession(
    val dao: FluxaDao,
    val signals: Signals,
    private val onProgress: ((List<VideoModel>) -> Unit)?
) {
    private val pool = Collections.synchronizedList(mutableListOf<ScoredShort>())
    private val poolIds = Collections.synchronizedSet(mutableSetOf<String>())
    private val emitted = Collections.synchronizedSet(mutableSetOf<String>())
    private val poolChannels = HashMap<String, Int>()
    private val seenWindow = ShortsSeenRegistry.WINDOW_SHADOW

    fun add(items: List<ScoredShort>) {
        if (items.isEmpty()) return
        val accepted = mutableListOf<ScoredShort>()
        synchronized(pool) {
            for (item in items) {
                val id = item.video.id
                if (id.isBlank() || !poolIds.add(id)) continue
                if (id in signals.excludedIds || id in ShortsSessionState.hiddenShorts) continue
                if (item.video.channelId in signals.blockedChannelIds) continue
                if (item.video.channelName.trim().lowercase() in signals.dislikedNames) continue
                if (ShortsSeenRegistry.isServedWithin(id, seenWindow)) continue
                if (RecentVideosTracker.isRecentlySeen(id)) continue
                val channelId = ShortsClassifier.normalizeChannelId(item.video.channelId)
                val taken = poolChannels[channelId] ?: 0
                if (taken >= ShortsSessionState.POOL_PER_CHANNEL) continue
                poolChannels[channelId] = taken + 1
                pool.add(item)
                accepted.add(item)
                if (pool.size >= ShortsSessionState.POOL_TARGET) break
            }
        }
        if (accepted.isNotEmpty() && pool.size >= ShortsSessionState.PAGE_SIZE) emitPreview()
    }

    fun addUnchecked(items: List<ScoredShort>) {
        if (items.isEmpty()) return
        synchronized(pool) {
            for (item in items) {
                val id = item.video.id
                if (id.isBlank() || !poolIds.add(id)) continue
                if (id in signals.excludedIds || id in ShortsSessionState.hiddenShorts) continue
                val channelId = ShortsClassifier.normalizeChannelId(item.video.channelId)
                val taken = poolChannels[channelId] ?: 0
                if (taken >= ShortsSessionState.POOL_PER_CHANNEL) continue
                poolChannels[channelId] = taken + 1
                pool.add(item)
                if (pool.size >= ShortsSessionState.POOL_TARGET) break
            }
        }
    }

    fun summary(): String = synchronized(pool) {
        val byChannel = HashMap<String, Int>()
        pool.forEach { byChannel.merge(ShortsClassifier.normalizeChannelId(it.video.channelId), 1, Int::plus) }
        "pool=${pool.size} canales=${byChannel.size} " +
            byChannel.entries.sortedByDescending { it.value }.take(8)
                .joinToString { "${it.key.take(6)}=${it.value}" }
    }

    fun size(): Int = synchronized(pool) { pool.size }

    fun poolSnapshot(): List<ScoredShort> = synchronized(pool) { pool.toList() }

    fun acceptsRelated(item: StreamInfoItem): Boolean {
        if (!ShortsGraph.isUsableRelated(item)) return false
        val id = VideoExtractor.cleanVideoId(item.url ?: "")
        if (id.isBlank() || id in ShortsSessionState.hiddenShorts || id in signals.excludedIds) return false
        if (ShortsSeenRegistry.isServedWithin(id, seenWindow)) return false
        if (RecentVideosTracker.isRecentlySeen(id)) return false
        val uploaderChannel = ShortsClassifier.normalizeChannelId(item.uploaderUrl)
        if (uploaderChannel in signals.blockedChannelIds) return false
        if (item.uploaderName?.trim()?.lowercase() in signals.dislikedNames) return false
        if (signals.hasSubscriptions && !isRelevantRelated(item.name, uploaderChannel, item.uploaderName)) {
            return false
        }
        return true
    }

    private fun isRelevantRelated(
        title: String?,
        channelId: String,
        channelName: String?
    ): Boolean {
        if (channelId.isNotEmpty() && channelId in signals.subscribedChannelIds) return true
        val name = channelName?.trim().orEmpty()
        if (name.isNotEmpty() && signals.taste.subscribedChannelNames.any {
                it.equals(name, ignoreCase = true)
            }
        ) return true
        if (ShortsClassifier.isOnTopic(title, signals.subscriptionVocabulary)) return true
        return signals.seeds.any { seed ->
            ShortsClassifier.sharesToken(title, seed.title) ||
                ShortsClassifier.sharesToken(title, seed.channelName)
        }
    }

    fun instantCache(): List<ScoredShort> {
        val cached = runCatching { dao.getRecentCachedVideos(60) }.getOrElse { emptyList() }
        if (cached.isEmpty()) return emptyList()
        val out = mutableListOf<ScoredShort>()
        cached.forEach { entity ->
            val model = ShortsCache.toVideoModel(entity)
            if (model.id in signals.excludedIds || model.id in ShortsSessionState.hiddenShorts) return@forEach
            if (model.channelId in signals.blockedChannelIds) return@forEach
            if (model.channelName in signals.dislikedChannelNames) return@forEach
            if (ShortsSeenRegistry.isServedWithin(model.id, seenWindow)) return@forEach
            if (RecentVideosTracker.isRecentlySeen(model.id)) return@forEach
            val isSubscribed = model.channelId in signals.subscribedChannelIds ||
                signals.taste.subscribedChannelNames.any {
                    it.equals(model.channelName.trim(), ignoreCase = true)
                }
            if (signals.hasSubscriptions &&
                !isSubscribed &&
                !ShortsClassifier.isOnTopic(model.title, signals.subscriptionVocabulary)
            ) return@forEach
            out.add(
                ScoredShort(
                    video = model,
                    score = 0.0,
                    source = ShortSource.SESSION_POOL,
                    originChannelId = model.channelId.orEmpty(),
                    originTitle = model.title
                )
            )
        }
        return out
    }

    private fun emitPreview() {
        val callback = onProgress ?: return
        if (poolChannels.size < ShortsSessionState.PREVIEW_MIN_CHANNELS) return
        val ranked = ShortsRanker.rank(
            poolSnapshot(),
            signals.taste,
            sessionSeed = ShortsSessionState.sessionSeed
        )
        val preview = mutableListOf<VideoModel>()
        ShortsRanker
            .select(
                ranked,
                ShortsSessionState.PAGE_SIZE,
                subscribedChannelIds = signals.subscribedChannelIds,
                sessionSeed = ShortsSessionState.sessionSeed
            )
            .forEach { entry ->
                if (emitted.add(entry.video.id)) preview.add(entry.video)
            }
        if (preview.isNotEmpty()) callback(preview)
    }
}
