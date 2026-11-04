package com.makkispacejam.fluxa.utils

import androidx.annotation.FontRes
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.makkispacejam.fluxa.R

enum class SubtitleFont(val key: String, val displayName: String, @FontRes val fontRes: Int?) {
    DEFAULT("default", "Default", null),
    BAGEL_FAT_ONE("bagel_fat_one", "Bagel Fat One", R.font.bagel_fat_one),
    MOZILLA_HEADLINE("mozilla_headline", "Mozilla Headline", R.font.mozilla_headline),
    MOMO_TRUST_DISPLAY("momo_trust_display", "Momo Trust Display", R.font.momo_trust_display),
    DOT_GOTHIC_16("dot_gothic_16", "DotGothic16", R.font.dot_gothic_16),
    FREEMAN("freeman", "Freeman", R.font.freeman),
    DYNA_PUFF("dyna_puff", "DynaPuff", R.font.dyna_puff);

    fun fontFamily(): FontFamily = fontRes?.let { FontFamily(Font(it)) } ?: FontFamily.Default

    companion object {
        fun fromKey(key: String?): SubtitleFont = entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

object SubtitleAppearance {
    const val MIN_SIZE = 12
    const val MAX_SIZE = 30
}
