package com.makkispacejam.fluxa.data.shorts

import java.util.Locale
import kotlin.math.ln

object ShortsClassifier {

    const val MIN_DURATION = 20L
    const val MAX_DURATION = 180L
    const val MAX_DURATION_TAGGED = 300L
    const val SWEET_MIN = 34L
    const val SWEET_MAX = 90L
    const val MIN_VIEWS = 300L

    private val SHORT_TAG =
        Regex("(?<![\\p{L}\\p{N}_])#shorts?(?![\\p{L}\\p{N}_])", RegexOption.IGNORE_CASE)
    private val HASHTAG = Regex("(?<![\\p{L}\\p{N}_])#([\\p{L}\\p{N}_]{2,40})")
    private val NOISE_TITLE = Regex("^(avatar|shorts?|reels?|short|reel)\\s*#?\\s*$", RegexOption.IGNORE_CASE)
    private val TOKEN_SPLIT = Regex("[^\\p{L}\\p{N}]+")
    private val NUMERIC = Regex("^\\d+$")

    private val STOP_WORDS = setOf(
        "este", "esta", "esto", "estos", "estas", "aqui", "todo", "todos", "cada", "otro",
        "mejor", "peor", "nuevo", "nueva", "ver", "ves", "verlo", "como", "para", "por",
        "que", "con", "sin", "sobre", "entre", "cuando", "donde", "porque", "the", "and",
        "for", "with", "you", "your", "this", "that", "from", "have", "has", "not", "but"
    )

    fun hasShortTag(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return SHORT_TAG.containsMatchIn(text)
    }

    fun hashtags(text: String?): Set<String> {
        if (text.isNullOrBlank()) return emptySet()
        return HASHTAG.findAll(text)
            .map { it.groupValues[1].lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() && it.length <= 30 }
            .toSet()
    }

    fun isShortsUrl(url: String?): Boolean =
        url != null && (url.contains("/shorts/", ignoreCase = true) || url.contains("/reels/", ignoreCase = true))

    fun isLiveUrl(url: String?): Boolean = url != null && url.contains("/live/", ignoreCase = true)

    fun isShort(title: String?, durationSeconds: Long, url: String? = null): Boolean {
        if (title.isNullOrBlank()) return false
        if (isLiveUrl(url)) return false

        val tagged = hasShortTag(title)
        val shortsUrl = isShortsUrl(url)
        val duration = durationSeconds

        return when {
            duration > 0L -> {
                duration in MIN_DURATION..MAX_DURATION ||
                    (tagged && duration <= MAX_DURATION_TAGGED)
            }
            else -> tagged || shortsUrl
        }
    }

    fun isShortLenient(title: String?, durationSeconds: Long): Boolean {
        if (title.isNullOrBlank()) return false
        if (durationSeconds <= 0L) return true
        if (durationSeconds in MIN_DURATION..MAX_DURATION) return true
        return hasShortTag(title) && durationSeconds <= MAX_DURATION_TAGGED
    }

    fun formatScore(title: String?, durationSeconds: Long, url: String? = null): Double {
        val tagged = hasShortTag(title)
        val shortsUrl = isShortsUrl(url)
        val duration = durationSeconds

        val durationScore = when {
            duration <= 0L -> if (tagged || shortsUrl) 0.5 else 0.0
            duration in SWEET_MIN..SWEET_MAX -> 1.0
            duration in MIN_DURATION..MAX_DURATION -> {
                val distance = if (duration < SWEET_MIN) SWEET_MIN - duration else duration - SWEET_MAX
                (1.0 - distance.toDouble() / 60.0).coerceIn(0.35, 0.9)
            }
            duration <= MAX_DURATION_TAGGED -> if (tagged) 0.3 else 0.12
            duration <= 600L -> 0.1
            else -> 0.0
        }

        var score = durationScore
        if (tagged) score += 0.35
        if (shortsUrl) score += 0.25
        return score.coerceIn(0.0, 1.0)
    }

    fun looksLikeJunk(title: String?, durationSeconds: Long, viewCount: Long): Boolean {
        if (title.isNullOrBlank()) return true
        if (NOISE_TITLE.containsMatchIn(title.trim())) return true
        if (durationSeconds < 0L) return true
        if (viewCount in 1 until MIN_VIEWS) return true
        return false
    }

    fun freshnessScore(timestamp: Long, now: Long = System.currentTimeMillis()): Double {
        if (timestamp <= 0L) return 0.45
        val ageHours = (now - timestamp) / 3_600_000.0
        if (ageHours < 0) return 1.0
        val decay = ln(2.0) / (10.0 * 24.0)
        return Math.exp(-decay * ageHours).coerceIn(0.05, 1.0)
    }

    fun popularityScore(viewCount: Long): Double {
        if (viewCount <= 0L) return 0.15
        val ratio = viewCount.toDouble() / 1_000_000.0
        return (ratio / (1.0 + ratio)).coerceIn(0.1, 1.0)
    }

    fun keywordTokens(text: String?): Set<String> {
        if (text.isNullOrBlank()) return emptySet()
        return TOKEN_SPLIT.split(text.lowercase(Locale.ROOT))
            .filter { it.length >= 4 && it !in STOP_WORDS && !NUMERIC.matches(it) }
            .toSet()
    }

    fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val intersection = a.count { it in b }
        val union = a.size + b.size - intersection
        return if (union == 0) 0.0 else intersection.toDouble() / union
    }

    fun sharesToken(a: String?, b: String?): Boolean {
        val left = keywordTokens(a)
        if (left.isEmpty()) return false
        val right = keywordTokens(b)
        if (right.isEmpty()) return false
        return left.any { it in right }
    }

    fun isOnTopic(text: String?, vocabulary: Set<String>): Boolean {
        if (vocabulary.isEmpty()) return true
        val tokens = keywordTokens(text)
        if (tokens.isEmpty()) return false
        return tokens.any { it in vocabulary }
    }

    fun onTopicStrength(text: String?, vocabulary: Set<String>): Double {
        if (vocabulary.isEmpty()) return 0.5
        val tokens = keywordTokens(text)
        if (tokens.isEmpty()) return 0.0
        val hits = tokens.count { it in vocabulary }
        if (hits == 0) return 0.0
        return (hits.toDouble() / tokens.size.toDouble()).coerceIn(0.0, 1.0)
    }

    fun titleSimilarity(a: String?, b: String?): Double =
        jaccard(keywordTokens(a), keywordTokens(b))

    fun normalizeChannelId(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val id = raw.trim()
        if (!id.contains("/")) return id.substringBefore("?")
        return id.substringAfter("/channel/", "").substringBefore("/").substringBefore("?")
    }

    fun sameChannel(a: String?, b: String?): Boolean {
        val ca = normalizeChannelId(a)
        val cb = normalizeChannelId(b)
        return ca.isNotEmpty() && cb.isNotEmpty() && ca == cb
    }

    fun durationBucket(durationSeconds: Long): Int = when {
        durationSeconds <= 0L -> 0
        durationSeconds < SWEET_MIN -> 1
        durationSeconds <= SWEET_MAX -> 2
        durationSeconds <= MAX_DURATION -> 3
        else -> 4
    }
}
