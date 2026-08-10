@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.utils

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import org.schabi.newpipe.extractor.stream.SubtitlesStream

object SubtitleConfigBuilder {
    fun buildConfigs(subtitles: List<SubtitlesStream>?): List<MediaItem.SubtitleConfiguration> {
        return subtitles?.mapNotNull { sub ->
            val subUrl = sub.url ?: return@mapNotNull null
            val fmtParam = Regex("fmt=(\\w+)").find(subUrl)?.groupValues?.get(1)?.lowercase()
            val isTtml = sub.format?.name?.uppercase() == "TTML" || fmtParam == "ttml"
            val mime = if (isTtml) MimeTypes.APPLICATION_TTML else MimeTypes.TEXT_VTT
            val fmt = if (isTtml) "ttml" else "vtt"
            val ensuredUrl = if (subUrl.contains("fmt=")) subUrl else {
                val separator = if (subUrl.contains("?")) "&" else "?"
                "$subUrl${separator}fmt=$fmt"
            }
            MediaItem.SubtitleConfiguration.Builder(ensuredUrl.toUri())
                .setMimeType(mime).setLanguage(sub.languageTag)
                .build()
        } ?: emptyList()
    }
}
