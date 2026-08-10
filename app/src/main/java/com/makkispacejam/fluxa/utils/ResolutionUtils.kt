@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.utils

import org.schabi.newpipe.extractor.stream.StreamInfo

// Extractor de resolución
object ResolutionUtils {
    fun extractAvailableResolutions(streamInfo: StreamInfo): List<Int> {
        val resolutions = mutableSetOf<Int>()
        streamInfo.videoStreams?.forEach { stream ->
            val res = parseResolution(stream.resolution)
            if (res in 1..1080) resolutions.add(res)
        }
        streamInfo.videoOnlyStreams?.forEach { stream ->
            val res = parseResolution(stream.resolution)
            if (res in 1..1080) resolutions.add(res)
        }
        return resolutions.toList().sortedDescending()
    }

    private fun parseResolution(resolutionStr: String?): Int {
        if (resolutionStr == null) return 0
        return try {
            val matches = Regex("\\d+").findAll(resolutionStr).toList()
            if (matches.isEmpty()) return 0
            if (resolutionStr.contains("x") && matches.size >= 2) {
                matches[1].value.toInt()
            } else {
                matches[0].value.toInt()
            }
        } catch (_: Exception) { 0 }
    }

    fun findLowerQuality(available: List<Int>, current: Int): Int? =
        available.filter { it < current }.maxOrNull()

    fun findHigherQuality(available: List<Int>, current: Int, maxQuality: Int): Int? =
        available.filter { it in (current + 1)..maxQuality }.maxOrNull()
}
