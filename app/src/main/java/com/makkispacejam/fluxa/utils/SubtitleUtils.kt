package com.makkispacejam.fluxa.utils

import androidx.media3.common.text.CueGroup

// Formato de subtítulos
object SubtitleUtils {
    fun filterSubtitleText(cueGroup: CueGroup): String {
        val fullText = cueGroup.cues
            .mapNotNull { it.text?.toString() }
            .joinToString(" ")
            .replace(Regex("\\[.*?]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (fullText.isEmpty()) return ""

        val words = fullText.split(Regex("\\s+")).filter { it.isNotBlank() }
        return if (words.size > 8) {
            words.takeLast(8).joinToString(" ")
        } else {
            fullText
        }
    }
}
