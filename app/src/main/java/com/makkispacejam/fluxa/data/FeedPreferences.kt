package com.makkispacejam.fluxa.data

import android.content.Context
import androidx.core.content.edit

object FeedPreferences {

    private const val SHOWN_VIDEOS_PREF = "shown_feed_videos"
    private const val MAX_SHOWN_IDS = 80

    fun loadShownVideoIds(context: Context?): Set<String> {
        if (context == null) return emptySet()
        val prefs = context.getSharedPreferences("fluxa_prefs", Context.MODE_PRIVATE)
        val raw = prefs.getString(SHOWN_VIDEOS_PREF, "") ?: ""
        if (raw.isEmpty()) return emptySet()
        val ids = raw.split(",").toMutableList()
        if (ids.size > MAX_SHOWN_IDS * 2) {
            val trimmed = ids.takeLast(MAX_SHOWN_IDS)
            prefs.edit { putString(SHOWN_VIDEOS_PREF, trimmed.joinToString(",")) }
            return trimmed.toSet()
        }
        return ids.toSet()
    }

    fun saveShownVideoIds(context: Context?, ids: List<String>) {
        if (context == null) return
        val prefs = context.getSharedPreferences("fluxa_prefs", Context.MODE_PRIVATE)
        val existing = loadShownVideoIds(context).toMutableList()
        existing.addAll(ids)
        val trimmed = existing.distinct().takeLast(MAX_SHOWN_IDS)
        prefs.edit { putString(SHOWN_VIDEOS_PREF, trimmed.joinToString(",")) }
    }
}
