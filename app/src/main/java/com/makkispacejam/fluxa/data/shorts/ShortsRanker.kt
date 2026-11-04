package com.makkispacejam.fluxa.data.shorts

import com.makkispacejam.fluxa.models.VideoModel

object ShortsRanker {

    private const val SOFT_REPEAT_PENALTY = 0.55
    private const val JITTER = 0.16
    private const val PICK_WINDOW = 4
    private const val TITLE_SIMILARITY_CEILING = 0.72

    fun noise(seed: Long, key: String): Double {
        var h = seed xor 0x61c8864680b583ebuL.toLong()
        for (c in key) h = h * 31 + c.code
        h = h xor (h ushr 33)
        h *= 0x7ee3623a03d3c83fuL.toLong()
        h = h xor (h ushr 29)
        h *= 0xbf58476d1ce4e5b9uL.toLong()
        h = h xor (h ushr 32)
        return ((h ushr 11).toDouble() * (1.0 / (1L shl 53).toDouble())).coerceIn(0.0, 1.0)
    }

    data class Taste(
        val likedChannelIds: Set<String> = emptySet(),
        val watchedChannelIds: Set<String> = emptySet(),
        val dislikedChannelIds: Set<String> = emptySet(),
        val dislikedChannelNames: Set<String> = emptySet(),
        val subscribedChannelIds: Set<String> = emptySet(),
        val subscribedChannelNames: Set<String> = emptySet(),
        val subscriptionVocabulary: Set<String> = emptySet(),
        val likedTitles: List<String> = emptyList(),
        val seedTokens: Set<String> = emptySet(),
        val softServedIds: Set<String> = emptySet()
    ) {
        val hasSubscriptions: Boolean
            get() = subscribedChannelIds.isNotEmpty() || subscribedChannelNames.isNotEmpty()
    }

    data class Options(
        val maxPerChannel: Int = 4,
        val maxPerSubscribedChannel: Int = 3,
        val maxPerDurationBucket: Int = 18,
        val titleSimilarityCeiling: Double = 0.72,
        val minScore: Double = 0.0
    )

    fun score(candidate: ScoredShort, taste: Taste, now: Long = System.currentTimeMillis()): Double {
        val video = candidate.video
        val channelId = ShortsClassifier.normalizeChannelId(video.channelId)

        if (channelId in taste.dislikedChannelIds) return -1.0
        if (video.channelName.isNotBlank() && taste.dislikedChannelNames.any {
                it.equals(video.channelName.trim(), ignoreCase = true)
            }
        ) return -1.0

        val duration = videoDuration(video)
        val formatOk = ShortsClassifier.isShort(video.title, duration, candidate.originUrl) ||
            (ShortsClassifier.hasShortTag(video.title) && duration <= ShortsClassifier.MAX_DURATION_TAGGED)
        if (!formatOk) return -1.0

        if (taste.hasSubscriptions && !isRelevant(candidate, taste)) return -1.0

        return rawScore(candidate, taste, now)
    }

    fun scoreRelaxed(candidate: ScoredShort, taste: Taste, now: Long = System.currentTimeMillis()): Double {
        val channelId = ShortsClassifier.normalizeChannelId(candidate.video.channelId)
        if (channelId in taste.dislikedChannelIds) return -1.0
        return rawScore(candidate, taste, now)
    }

    private fun isSubscribed(candidate: ScoredShort, taste: Taste): Boolean {
        val video = candidate.video
        val channelId = ShortsClassifier.normalizeChannelId(video.channelId)
        if (channelId.isNotEmpty() && channelId in taste.subscribedChannelIds) return true
        val name = video.channelName.trim()
        if (name.isEmpty()) return false
        return taste.subscribedChannelNames.any { it.equals(name, ignoreCase = true) }
    }

    private fun isRelevant(candidate: ScoredShort, taste: Taste): Boolean {
        if (isSubscribed(candidate, taste)) return true
        if (candidate.source == ShortSource.RELATED_L1 ||
            candidate.source == ShortSource.RELATED_L2 ||
            candidate.source == ShortSource.CHANNEL_RELATED
        ) {
            if (ShortsClassifier.isOnTopic(candidate.video.title, taste.seedTokens)) return true
            if (ShortsClassifier.sharesToken(candidate.video.title, candidate.originTitle)) return true
            if (ShortsClassifier.isOnTopic(candidate.video.title, taste.subscriptionVocabulary)) return true
            val origin = candidate.originChannelId
            return origin.isNotEmpty() &&
                ShortsClassifier.normalizeChannelId(candidate.video.channelId) == origin
        }
        return false
    }

    private fun rawScore(candidate: ScoredShort, taste: Taste, now: Long): Double {
        val video = candidate.video
        val channelId = ShortsClassifier.normalizeChannelId(video.channelId)
        val duration = videoDuration(video)

        val format = ShortsClassifier.formatScore(video.title, duration, candidate.originUrl)
        val freshness = ShortsClassifier.freshnessScore(video.timestamp, now)
        val popularity = ShortsClassifier.popularityScore(video.viewCount)

        val subscribed = isSubscribed(candidate, taste)

        var affinity = 0.0
        if (subscribed) affinity += 0.55
        if (channelId.isNotEmpty() && channelId in taste.likedChannelIds) affinity += 0.30
        if (channelId.isNotEmpty() && channelId in taste.watchedChannelIds) affinity += 0.16
        if (ShortsClassifier.sameChannel(channelId, candidate.originChannelId) && !subscribed) affinity -= 0.12

        val anchors = taste.subscriptionVocabulary.ifEmpty {
            taste.seedTokens
        }
        val topic = ShortsClassifier.onTopicStrength(video.title, anchors)

        val likeSimilarity = taste.likedTitles
            .take(4)
            .maxOfOrNull { ShortsClassifier.titleSimilarity(video.title, it) } ?: 0.0

        val sourceWeight = if (subscribed) 1.0 else candidate.source.weight

        var total = sourceWeight * 0.42 +
            format * 0.24 +
            freshness * 0.18 +
            popularity * 0.12 +
            affinity +
            topic * 0.22 +
            likeSimilarity * 0.10

        if (video.viewCount in 1 until ShortsClassifier.MIN_VIEWS) total -= 0.25
        if (ShortsClassifier.hasShortTag(video.title)) total += 0.05
        if (video.id in taste.softServedIds) total -= SOFT_REPEAT_PENALTY

        return total
    }

    fun rank(
        candidates: List<ScoredShort>,
        taste: Taste,
        now: Long = System.currentTimeMillis(),
        sessionSeed: Long = 0L,
        options: Options = Options()
    ): List<ScoredShort> = candidates
        .map { it.copy(score = score(it, taste, now)) }
        .filter { it.score > options.minScore && it.score >= 0 }
        .shuffledRanked(sessionSeed)

    private fun List<ScoredShort>.shuffledRanked(sessionSeed: Long): List<ScoredShort> = this
        .map { it to (it.score + (noise(sessionSeed, it.video.id) * 2.0 - 1.0) * JITTER) }
        .sortedByDescending { it.second }
        .map { it.first }

    fun rankRelaxed(
        candidates: List<ScoredShort>,
        taste: Taste,
        sessionSeed: Long = 0L
    ): List<ScoredShort> = candidates
        .map { it.copy(score = scoreRelaxed(it, taste)) }
        .filter { it.score >= 0 }
        .shuffledRanked(sessionSeed)

    fun shuffleRestricted(items: List<ScoredShort>, sessionSeed: Long, attempts: Int = 6): List<ScoredShort> {
        if (items.size < 3) return items
        val channels = items.map { ShortsClassifier.normalizeChannelId(it.video.channelId) }
        val tokens = items.map { ShortsClassifier.keywordTokens(it.video.title) }
        val bestCost = adjacencyCost(channels, tokens)
        if (bestCost == 0) return items

        val order = items.indices.toMutableList()
        var best = items
        var bestScore = bestCost
        for (i in 0 until attempts) {
            val candidate = shuffledBySeed(items, order, "$sessionSeed:$i")
            val cost = adjacencyCost(
                candidate.map { ShortsClassifier.normalizeChannelId(it.video.channelId) },
                candidate.map { ShortsClassifier.keywordTokens(it.video.title) }
            )
            if (cost < bestScore) {
                best = candidate
                bestScore = cost
                if (cost == 0) break
            }
        }
        return best
    }

    private fun shuffledBySeed(
        items: List<ScoredShort>,
        order: MutableList<Int>,
        salt: String
    ): List<ScoredShort> {
        val scratch = order.toMutableList()
        for (i in scratch.indices) {
            val j = (noise(i.toLong(), "$salt:$i") * scratch.size).toInt()
                .coerceIn(0, scratch.size - 1)
            val tmp = scratch[i]
            scratch[i] = scratch[j]
            scratch[j] = tmp
        }
        return scratch.map { items[it] }
    }

    private fun adjacencyCost(channels: List<String>, tokens: List<Set<String>>): Int {
        var cost = 0
        for (i in 1 until channels.size) {
            val prevChannel = channels[i - 1]
            if (prevChannel.isNotEmpty() && prevChannel == channels[i]) cost += 2
            if (ShortsClassifier.jaccard(tokens[i], tokens[i - 1]) > TITLE_SIMILARITY_CEILING) cost += 1
        }
        return cost
    }

    fun select(
        ranked: List<ScoredShort>,
        limit: Int,
        options: Options = Options(),
        subscribedChannelIds: Set<String> = emptySet(),
        sessionSeed: Long = 0L
    ): List<ScoredShort> {
        if (ranked.isEmpty() || limit <= 0) return emptyList()

        val remaining = ArrayDeque(ranked)
        val chosen = mutableListOf<ScoredShort>()
        val seenIds = HashSet<String>()
        val channelCount = HashMap<String, Int>()
        val bucketCount = HashMap<Int, Int>()
        var lastTitleTokens: Set<String> = emptySet()
        var lastChannelId = ""

        fun capFor(channelId: String): Int =
            if (channelId.isNotEmpty() && channelId in subscribedChannelIds) {
                options.maxPerSubscribedChannel
            } else {
                options.maxPerChannel
            }

        fun channelOf(v: VideoModel) = ShortsClassifier.normalizeChannelId(v.channelId)

        fun durationOf(v: VideoModel) = videoDuration(v)
        fun bucketOf(v: VideoModel) = ShortsClassifier.durationBucket(durationOf(v))

        fun passes(v: VideoModel, channelId: String, bucket: Int): Boolean {
            if (v.id in seenIds) return false
            if (channelCount.getOrDefault(channelId, 0) >= capFor(channelId)) return false
            if (bucketCount.getOrDefault(bucket, 0) >= options.maxPerDurationBucket) return false
            if (channelId.isNotEmpty() && channelId == lastChannelId) return false
            if (lastTitleTokens.isNotEmpty()) {
                val sim = ShortsClassifier.jaccard(ShortsClassifier.keywordTokens(v.title), lastTitleTokens)
                if (sim > options.titleSimilarityCeiling) return false
            }
            return true
        }

        fun commit(v: VideoModel) {
            val channelId = ShortsClassifier.normalizeChannelId(v.channelId)
            val bucket = bucketOf(v)
            seenIds.add(v.id)
            channelCount[channelId] = channelCount.getOrDefault(channelId, 0) + 1
            bucketCount[bucket] = bucketCount.getOrDefault(bucket, 0) + 1
            lastChannelId = channelId
            lastTitleTokens = ShortsClassifier.keywordTokens(v.title)
        }

        fun pickAmong(indices: List<Int>, salt: String): Int {
            if (indices.size <= 1) return indices.first()
            val window = minOf(PICK_WINDOW, indices.size)
            val offset = (noise(sessionSeed, "$salt:${remaining.size}:$window") * window)
                .toInt().coerceIn(0, window - 1)
            return indices[offset]
        }

        fun bestIndex(): Int {
            if (remaining.isEmpty()) return -1
            val passing = ArrayList<Int>(minOf(remaining.size, 40))
            for (i in remaining.indices) {
                val entry = remaining[i]
                val cid = channelOf(entry.video)
                if (passes(entry.video, cid, bucketOf(entry.video))) passing.add(i)
            }
            if (passing.isEmpty()) return -1
            var minPicks = Int.MAX_VALUE
            passing.forEach { i ->
                val picks = channelCount.getOrDefault(channelOf(remaining[i].video), 0)
                if (picks < minPicks) minPicks = picks
            }
            val leastUsed = passing.filter {
                channelCount.getOrDefault(channelOf(remaining[it].video), 0) == minPicks
            }
            val preferred = leastUsed.filter { channelOf(remaining[it].video) in subscribedChannelIds }
            return pickAmong(preferred.ifEmpty { leastUsed }, "rr")
        }

        fun bestIndexRelaxed(): Int {
            var best = -1
            var bestPicks = Int.MAX_VALUE
            var ties = 0
            for (i in remaining.indices) {
                val entry = remaining[i]
                if (entry.video.id in seenIds) continue
                val cid = channelOf(entry.video)
                if (cid.isNotEmpty() && cid == lastChannelId) continue
                val bucket = bucketOf(entry.video)
                if (bucketCount.getOrDefault(bucket, 0) >= options.maxPerDurationBucket) continue
                if (lastTitleTokens.isNotEmpty()) {
                    val sim = ShortsClassifier.jaccard(ShortsClassifier.keywordTokens(entry.video.title), lastTitleTokens)
                    if (sim > options.titleSimilarityCeiling) continue
                }
                val picks = channelCount.getOrDefault(cid, 0)
                if (picks < bestPicks) {
                    bestPicks = picks
                    best = i
                    ties = 1
                } else if (picks == bestPicks) {
                    ties++
                    if ((noise(sessionSeed, "relax:$ties") * ties).toInt() == 0) best = i
                }
            }
            return best
        }

        while (remaining.isNotEmpty() && chosen.size < limit) {
            val before = remaining.size
            val burst = maxOf(1, limit / 8)

            repeat(burst) {
                if (remaining.isEmpty() || chosen.size >= limit) return@repeat
                var pick = bestIndex()
                if (pick < 0) pick = bestIndexRelaxed()
                if (pick >= 0) {
                    val picked = remaining.removeAt(pick)
                    chosen.add(picked)
                    commit(picked.video)
                } else {
                    val alt = if (lastChannelId.isEmpty()) -1 else remaining.indexOfFirst {
                        channelOf(it.video) != lastChannelId
                    }
                    val forcedIndex = if (alt >= 0) alt else 0
                    val forced = remaining.removeAt(forcedIndex)
                    chosen.add(forced)
                    commit(forced.video)
                }
            }

            if (remaining.size == before) break
        }

        return chosen
    }

    private fun videoDuration(video: VideoModel): Long = video.duration
}
