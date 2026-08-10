package com.makkispacejam.fluxa.viewmodels.content

import android.content.SharedPreferences
import androidx.core.content.edit

class RefreshRateLimiter(private val prefs: SharedPreferences) {

    fun canRefresh(): Boolean {
        val refreshCount = prefs.getInt("refresh_count", 0)
        val lastResetTime = prefs.getLong("last_refresh_reset_time", 0L)
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastResetTime > 5 * 60 * 1000) {
            prefs.edit {
                putInt("refresh_count", 1)
                putLong("last_refresh_reset_time", currentTime)
            }
            return true
        }

        if (refreshCount < 30) {
            prefs.edit { putInt("refresh_count", refreshCount + 1) }
            return true
        }

        return false
    }
}
