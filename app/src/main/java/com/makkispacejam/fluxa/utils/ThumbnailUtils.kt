package com.makkispacejam.fluxa.utils

// Extractor de thumbnail
object ThumbnailUtils {
    fun getHighQualityThumbnail(input: String?): String {
        if (input.isNullOrBlank()) return ""
        if (input.length == 11 && !input.contains("/") && !input.contains(".")) {
            return "https://img.youtube.com/vi/$input/hqdefault.jpg"
        }

        return try {
            val videoId = when {
                input.contains("v=") -> input.substringAfter("v=").substringBefore("&")
                input.contains("youtu.be/") -> input.substringAfter("youtu.be/").substringBefore("?")
                input.contains("/vi/") -> input.substringAfter("/vi/").substringBefore("/")
                else -> ""
            }

            if (videoId.isNotBlank() && videoId.length >= 11) {
                "https://img.youtube.com/vi/${videoId.take(11)}/hqdefault.jpg"
            } else {
                input
            }
        } catch (_: Exception) {
            input
        }
    }

    fun getBestThumbnailUrl(videoId: String?, fallbackUrl: String?): String {
        if (!videoId.isNullOrBlank()) {
            return "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
        }
        return getHighQualityThumbnail(fallbackUrl)
    }
}