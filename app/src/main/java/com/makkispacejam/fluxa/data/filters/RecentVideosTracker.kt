package com.makkispacejam.fluxa.data.filters

// Historial de videos recientes
object RecentVideosTracker {
    private const val MAX_HISTORY_SIZE = 1000
    private const val EXPIRE_HOURS = 48

    private val seenVideos = mutableMapOf<String, Long>()

    fun isRecentlySeen(videoId: String): Boolean {
        val timestamp = seenVideos[videoId] ?: return false
        val hoursSinceSeen = (System.currentTimeMillis() - timestamp) / (1000 * 60 * 60)
        return hoursSinceSeen < EXPIRE_HOURS
    }

    // Videos ya vistos
    fun markAsSeen(videoIds: List<String>) {
        val now = System.currentTimeMillis()
        videoIds.forEach { id ->
            seenVideos[id] = now
        }
        if (seenVideos.size > MAX_HISTORY_SIZE) {
            val oldestEntries = seenVideos.entries
                .sortedBy { it.value }
                .take(MAX_HISTORY_SIZE / 4)
                .map { it.key }
            oldestEntries.forEach { seenVideos.remove(it) }
        }
    }

    fun clearAll() { seenVideos.clear() }
}