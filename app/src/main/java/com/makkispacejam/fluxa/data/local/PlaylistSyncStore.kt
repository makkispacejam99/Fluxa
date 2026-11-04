package com.makkispacejam.fluxa.data.local

import android.content.Context

object PlaylistSyncStore {
    private const val PREFS_NAME = "playlist_sync_sources"
    private const val KEY_PREFIX = "sources_"
    private const val SEPARATOR = "\n"

    fun addSource(context: Context, playlistName: String, url: String) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return
        val sources = getSources(context, playlistName)
        if (sources.any { it.equals(cleanUrl, ignoreCase = true) }) return
        save(context, playlistName, sources + cleanUrl)
    }

    fun getSources(context: Context, playlistName: String): List<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PREFIX + playlistName, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()

    private fun save(context: Context, playlistName: String, sources: List<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + playlistName, sources.joinToString(SEPARATOR))
            .apply()
    }
}
