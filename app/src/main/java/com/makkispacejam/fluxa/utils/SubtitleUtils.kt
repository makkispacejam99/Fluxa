package com.makkispacejam.fluxa.utils

import androidx.media3.common.text.CueGroup

object SubtitleUtils {
    private val tagRegex = Regex("\\[.*?]")
    private val spaceRegex = Regex("[ \\t\\u00A0]+")

    fun filterSubtitleText(cueGroup: CueGroup, previousLine: String): String {
        val raw = cueGroup.cues
            .mapNotNull { it.text?.toString() }
            .lastOrNull { it.isNotBlank() }
            ?: return ""

        val lines = raw
            .split('\n')
            .map { it.replace(tagRegex, "").replace(spaceRegex, " ").trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) return ""

        val visible = if (lines.size > 1 &&
            previousLine.isNotEmpty() &&
            lines.first().equals(previousLine, ignoreCase = true)
        ) {
            lines.drop(1)
        } else {
            lines
        }

        return visible.joinToString("\n")
    }
}