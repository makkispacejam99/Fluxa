package com.makkispacejam.fluxa.models.home

data class HomeFeedItem(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelId: String = "",
    val channelAvatarUrl: String,
    val thumbnailUrl: String,
    val viewCount: Long = 0L,
    val durationSeconds: Long = 0L,
    val publishedTime: String = "",
    val timestamp: Long = 0L,
    val itemType: HomeFeedItemType = HomeFeedItemType.VIDEO,
    val playlistVideoCount: String = "",
    val subscriberCount: String = ""
) {
    // Formato de views
    val formattedViews: String get() = formatCount(viewCount) + " vistas"

    // Formato de duración
    val formattedDuration: String get() {
        val min = durationSeconds / 60
        val sec = durationSeconds % 60
        return if (min >= 60) {
            String.format(java.util.Locale.US, "%d:%02d:%02d", min / 60, min % 60, sec)
        } else {
            String.format(java.util.Locale.US, "%d:%02d", min, sec)
        }
    }

    companion object {
        // Conteo de views
        fun formatCount(count: Long): String {
            return when {
                count >= 1_000_000 -> "${String.format(java.util.Locale.US, "%.1f", count / 1_000_000f)}M"
                count >= 1_000 -> "${String.format(java.util.Locale.US, "%.1f", count / 1_000f)}k"
                else -> count.toString()
            }
        }
    }
}

enum class HomeFeedItemType {
    VIDEO, PLAYLIST, CHANNEL, LIVE
}
