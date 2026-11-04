package com.makkispacejam.fluxa.data

import com.makkispacejam.fluxa.data.filters.WatchedArchive
import com.makkispacejam.fluxa.data.local.FluxaDao
import com.makkispacejam.fluxa.data.local.VideoInteractionEntity
import com.makkispacejam.fluxa.data.shorts.RecentShort
import com.makkispacejam.fluxa.data.shorts.SeedCandidate
import com.makkispacejam.fluxa.data.shorts.ShortsCacheManager
import com.makkispacejam.fluxa.data.shorts.ShortsClassifier
import com.makkispacejam.fluxa.data.shorts.ShortsRanker
import com.makkispacejam.fluxa.data.shorts.ShortsSeenRegistry
import com.makkispacejam.fluxa.data.shorts.ShortsSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class Signals(
    val blockedChannelIds: Set<String>,
    val dislikedChannelNames: Set<String>,
    val excludedIds: Set<String>,
    val dislikedVideoIds: Set<String>,
    val seeds: List<SeedCandidate>,
    val taste: ShortsRanker.Taste,
    val topicTitles: List<String>,
    val subscribedChannelIds: Set<String>,
    val subscriptionVocabulary: MutableSet<String>
) {
    val dislikedNames: Set<String> =
        dislikedChannelNames.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    val hasSubscriptions: Boolean
        get() = subscribedChannelIds.isNotEmpty() || taste.subscribedChannelNames.isNotEmpty()
}

internal suspend fun readSignals(dao: FluxaDao): Signals = withContext(Dispatchers.IO) {
    val blocked = dao.getBlockedChannelIds().toSet()
    val interactions: List<VideoInteractionEntity> = dao.getRecentInteractions(400)
    val watched = dao.getAllWatchedVideoIds().toSet() + WatchedArchive.snapshot()
    val cached = runCatching { dao.getRecentCachedVideos(150) }.getOrElse { emptyList() }
    val subscriptions = runCatching { dao.getAllSubscriptions() }.getOrElse { emptyList() }

    val subscribedIds = subscriptions
        .map { ShortsClassifier.normalizeChannelId(it.channelId) }
        .filter { it.isNotEmpty() }
        .toSet()
    val subscribedNames = subscriptions
        .map { it.channelName?.trim().orEmpty() }
        .filter { it.isNotEmpty() }
        .toSet()
    val subscribedNamesLower = subscribedNames.map { it.lowercase() }.toSet()

    val vocabulary = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    subscriptions.forEach { sub ->
        ShortsClassifier.keywordTokens(sub.channelName).forEach { vocabulary.add(it) }
        sub.channelName.orEmpty().lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
            .split(' ')
            .filter { it.length >= 3 }
            .forEach { vocabulary.add(it) }
    }

    cached.filter { entity ->
        val id = entity.channelId?.let { ShortsClassifier.normalizeChannelId(it) }.orEmpty()
        val name = entity.channelName?.trim().orEmpty()
        (id.isNotEmpty() && id in subscribedIds) || name.lowercase() in subscribedNamesLower
    }.forEach { entity ->
        ShortsClassifier.keywordTokens(entity.title).forEach { vocabulary.add(it) }
    }

    val disliked = interactions.filter { it.isDisliked }
    val dislikedNamesRaw = disliked.mapNotNull { it.channelName }
        .filter { it.isNotBlank() }.toSet()
    val dislikedNames = dislikedNamesRaw.map { it.trim().lowercase() }.toSet()

    val dislikedVideoIds = disliked.map { it.videoId }.toSet()
    val blockedByDislike = cached
        .filter { it.channelName?.trim()?.lowercase() in dislikedNames }
        .mapNotNull { it.channelId }
        .filter { it.isNotBlank() }
        .toSet()

    val liked = interactions.filter { it.isLiked }
    val history = interactions.filter { it.lastWatchedAt > 0 }

    val likedChannels = liked.mapNotNull { it.channelName }
        .map { ShortsClassifier.normalizeChannelId(it) }.filter { it.isNotEmpty() }.toSet()
    val watchedChannels = history.mapNotNull { it.channelName }
        .map { ShortsClassifier.normalizeChannelId(it) }.filter { it.isNotEmpty() }.toSet()

    val recentShorts = ShortsSeenRegistry.recentShorts()
    val seeds = buildSeeds(
        liked = liked,
        history = history,
        recentShorts = recentShorts,
        dislikedVideoIds = dislikedVideoIds,
        dislikedNames = dislikedNames,
        subscribedIds = subscribedIds,
        subscribedNamesLower = subscribedNamesLower
    )

    val seedTokens = seeds.flatMap { ShortsClassifier.keywordTokens(it.title).toList() }.toSet()
    val topicTitles = buildList {
        addAll(liked.mapNotNull { it.title })
        addAll(recentShorts.map { it.title })
        addAll(history.mapNotNull { it.title })
    }.filter { it.isNotBlank() }.take(60)

    Signals(
        blockedChannelIds = blocked + blockedByDislike,
        dislikedChannelNames = dislikedNamesRaw,
        excludedIds = dislikedVideoIds.toSet() + watched + ShortsCacheManager.getSeenIds() + ShortsSessionState.hiddenShorts,
        dislikedVideoIds = dislikedVideoIds.toSet(),
        seeds = seeds,
        taste = ShortsRanker.Taste(
            likedChannelIds = likedChannels,
            watchedChannelIds = watchedChannels,
            dislikedChannelIds = blockedByDislike,
            dislikedChannelNames = dislikedNames,
            subscribedChannelIds = subscribedIds,
            subscribedChannelNames = subscribedNames,
            subscriptionVocabulary = vocabulary,
            likedTitles = liked.mapNotNull { it.title },
            seedTokens = seedTokens,
            softServedIds = ShortsSeenRegistry.servedWithin(ShortsSeenRegistry.WINDOW_SOFT)
        ),
        topicTitles = topicTitles,
        subscribedChannelIds = subscribedIds,
        subscriptionVocabulary = vocabulary
    )
}

internal fun buildQueries(signals: Signals): List<String> {
    if (!signals.hasSubscriptions) {
        return ShortsSource.buildTopicQueries(signals.topicTitles)
    }
    val fromSubs = ShortsSource.buildTopicQueries(
        signals.subscriptionVocabulary.take(40),
        limit = 5
    )
    val fromTitles = ShortsSource.buildTopicQueries(signals.topicTitles, limit = 3)
    return (fromSubs + fromTitles).distinct().take(6)
}

private fun buildSeeds(
    liked: List<VideoInteractionEntity>,
    history: List<VideoInteractionEntity>,
    recentShorts: List<RecentShort>,
    dislikedVideoIds: Set<String>,
    dislikedNames: Set<String>,
    subscribedIds: Set<String>,
    subscribedNamesLower: Set<String>
): List<SeedCandidate> {
    val now = System.currentTimeMillis()
    val weighted = LinkedHashMap<String, SeedCandidate>()
    val hasSubscriptions = subscribedIds.isNotEmpty() || subscribedNamesLower.isNotEmpty()

    fun push(candidate: SeedCandidate) {
        if (candidate.videoId.isBlank()) return
        if (candidate.videoId in dislikedVideoIds) return
        if (candidate.channelName.isNotBlank() &&
            candidate.channelName.trim().lowercase() in dislikedNames
        ) return
        val existing = weighted[candidate.videoId]
        weighted[candidate.videoId] = when {
            existing == null -> candidate
            candidate.weight > existing.weight -> candidate.copy(
                title = candidate.title.ifBlank { existing.title },
                channelId = candidate.channelId.ifBlank { existing.channelId },
                channelName = candidate.channelName.ifBlank { existing.channelName }
            )
            else -> existing
        }
    }

    liked.forEach {
        push(
            SeedCandidate(
                videoId = it.videoId,
                title = it.title ?: "",
                channelId = ShortsClassifier.normalizeChannelId(it.channelName),
                channelName = it.channelName ?: "",
                weight = 1.4
            )
        )
    }

    history.forEach { entity ->
        val ageHours = (now - entity.lastWatchedAt) / 3_600_000.0
        if (ageHours !in 0.0..720.0) return@forEach
        val name = entity.channelName?.trim().orEmpty()
        val channelId = ShortsClassifier.normalizeChannelId(entity.channelName)
        if (hasSubscriptions &&
            channelId !in subscribedIds &&
            name.lowercase() !in subscribedNamesLower
        ) return@forEach
        push(
            SeedCandidate(
                videoId = entity.videoId,
                title = entity.title ?: "",
                channelId = channelId,
                channelName = name,
                weight = (1.25 - (ageHours / 336.0)).coerceIn(0.3, 1.25)
            )
        )
    }

    recentShorts.forEachIndexed { index, item ->
        val ageHours = (now - item.servedAt) / 3_600_000.0
        val channelId = ShortsClassifier.normalizeChannelId(item.channelId)
        val name = item.channelName.trim()
        if (hasSubscriptions &&
            channelId !in subscribedIds &&
            name.lowercase() !in subscribedNamesLower
        ) return@forEachIndexed
        push(
            SeedCandidate(
                videoId = item.videoId,
                title = item.title,
                channelId = item.channelId,
                channelName = item.channelName,
                weight = (1.3 - (index * 0.06) - (ageHours / 480.0)).coerceIn(0.25, 1.3)
            )
        )
    }

    return weighted.values.sortedByDescending { it.weight }.take(14)
}
