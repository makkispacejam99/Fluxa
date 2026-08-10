package com.makkispacejam.fluxa.video.video.controls

import java.util.Locale

// Formato del reproductor principal
fun formatTime(ms: Long): String {
    if (ms <= 0) return "00:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600

    return if (hours > 0) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

fun stripEmojis(text: String): String {
    return text.replace(Regex("[\\p{So}\\p{Cn}]"), "").trim()
}
