package com.makkispacejam.fluxa.data.local

import android.content.Context

object PlaylistPersistenceManager {
    private const val PREFS_NAME = "playlist_progress_prefs"
    private const val KEY_INDEX = "index_"
    private const val KEY_VIDEO_ID = "videoId_"

    fun saveProgress(context: Context, playlistTitle: String, index: Int, videoId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt(KEY_INDEX + playlistTitle, index)
            putString(KEY_VIDEO_ID + playlistTitle, videoId)
            apply()
        }
    }

    fun getProgress(context: Context, playlistTitle: String): Pair<Int, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val index = prefs.getInt(KEY_INDEX + playlistTitle, -1)
        val videoId = prefs.getString(KEY_VIDEO_ID + playlistTitle, null)
        return Pair(index, videoId)
    }
}