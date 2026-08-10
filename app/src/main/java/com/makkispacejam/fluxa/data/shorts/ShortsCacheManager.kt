package com.makkispacejam.fluxa.data.shorts

import com.makkispacejam.fluxa.models.VideoModel

object ShortsCacheManager {
    private val seenVideoIds = mutableSetOf<String>()
    private const val MAX_SEEN_HISTORY = 1000

    fun markAsSeen(videos: List<VideoModel>) {
        seenVideoIds.addAll(videos.map { it.id })
        if (seenVideoIds.size > MAX_SEEN_HISTORY) {
            val toRemove = seenVideoIds.take(seenVideoIds.size - MAX_SEEN_HISTORY)
            seenVideoIds.removeAll(toRemove.toSet())
        }
    }

    fun getSeenIds(): Set<String> = seenVideoIds.toSet()
    fun clearCache() { seenVideoIds.clear() }
    fun markLeaveTime() {}
}
