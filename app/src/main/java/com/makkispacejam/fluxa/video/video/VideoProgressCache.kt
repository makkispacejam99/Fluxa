package com.makkispacejam.fluxa.video.video

import android.content.Context
import android.content.SharedPreferences

// Cache del progreso de video
object VideoProgressCache {

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("fluxa_video_progress", Context.MODE_PRIVATE)
    }

    fun get(videoId: String): Long = prefs?.getLong(videoId, 0L) ?: 0L
}