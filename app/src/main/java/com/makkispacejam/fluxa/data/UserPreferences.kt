package com.makkispacejam.fluxa.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import com.makkispacejam.fluxa.ThemeMode

// Preferencias de usuario
class UserPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("fluxa_prefs", Context.MODE_PRIVATE)

    companion object {
        @Volatile
        var incognitoActive = false
            private set

        fun setIncognito(value: Boolean) { incognitoActive = value }
    }

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString("theme_mode", ThemeMode.System.name)
            return try { ThemeMode.valueOf(name!!) } catch (_: Exception) { ThemeMode.System }
        }
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putString("theme_mode", value.name).apply()

    var contentRegion: String
        get() = prefs.getString("content_region", "")?.ifBlank { null }
            ?: java.util.Locale.getDefault().country.ifBlank { "US" }
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putString("content_region", value).apply()

    var contentLanguage: String
        get() = prefs.getString("content_language", "")?.ifBlank { null }
            ?: java.util.Locale.getDefault().language.ifBlank { "en" }
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putString("content_language", value).apply()

    var videoQuality: String
        get() = prefs.getString("video_quality", "480p") ?: "480p"
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putString("video_quality", value).apply()

    var shortsVideoQuality: String
        get() = prefs.getString("shorts_video_quality", "480p") ?: "480p"
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putString("shorts_video_quality", value).apply()

    var audioNormalizerEnabled: Boolean
        get() = prefs.getBoolean("audio_normalizer_enabled", false)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putBoolean("audio_normalizer_enabled", value).apply()

    var translationLanguage: String?
        get() = prefs.getString("translation_language", null)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putString("translation_language", value).apply()

    var downloadedTranslationLanguages: Set<String>
        get() = prefs.getStringSet("downloaded_translation_languages", emptySet()) ?: emptySet()
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putStringSet("downloaded_translation_languages", value).apply()

    var isFirstRun: Boolean
        get() = prefs.getBoolean("is_first_run", true)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putBoolean("is_first_run", value).apply()

    var amoledMode: Boolean
        get() = prefs.getBoolean("amoled_mode", false)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putBoolean("amoled_mode", value).apply()

    var incognitoMode: Boolean
        get() = prefs.getBoolean("incognito_mode", false)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putBoolean("incognito_mode", value).apply()

    var shortsGesturesHintShownAt: Long
        get() = prefs.getLong("shorts_gestures_hint_shown_at", 0L)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putLong("shorts_gestures_hint_shown_at", value).apply()

    var shortsGesturesTutorialEnabled: Boolean
        get() = prefs.getBoolean("shorts_gestures_tutorial_enabled", true)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putBoolean("shorts_gestures_tutorial_enabled", value).apply()

    var subtitleFont: String
        get() = prefs.getString("subtitle_font", "default") ?: "default"
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putString("subtitle_font", value).apply()

    var subtitleTextSize: Int
        get() = prefs.getInt("subtitle_text_size", 16)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putInt("subtitle_text_size", value).apply()

    var subtitlesEnabled: Boolean
        get() = prefs.getBoolean("subtitles_enabled", false)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putBoolean("subtitles_enabled", value).apply()

    var shortsControlsAlwaysVisible: Boolean
        get() = prefs.getBoolean("shorts_controls_always_visible", false)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putBoolean("shorts_controls_always_visible", value).apply()

    var breakReminderEnabled: Boolean
        get() = prefs.getBoolean("break_reminder_enabled", false)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putBoolean("break_reminder_enabled", value).apply()

    var breakReminderIntervalMinutes: Int
        get() = prefs.getInt("break_reminder_interval_minutes", 45)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putInt("break_reminder_interval_minutes", value).apply()

    var breakReminderAccumulatedMs: Long
        get() = prefs.getLong("break_reminder_accumulated_ms", 0L)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putLong("break_reminder_accumulated_ms", value).apply()

    var breakReminderSnoozeUntil: Long
        get() = prefs.getLong("break_reminder_snooze_until", 0L)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putLong("break_reminder_snooze_until", value).apply()

    var bedtimeReminderEnabled: Boolean
        get() = prefs.getBoolean("bedtime_reminder_enabled", false)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putBoolean("bedtime_reminder_enabled", value).apply()

    var bedtimeReminderHour: Int
        get() = prefs.getInt("bedtime_reminder_hour", 23)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putInt("bedtime_reminder_hour", value).apply()

    var bedtimeReminderMinute: Int
        get() = prefs.getInt("bedtime_reminder_minute", 0)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putInt("bedtime_reminder_minute", value).apply()

    var bedtimeReminderLastShownDate: String
        get() = prefs.getString("bedtime_reminder_last_shown_date", "") ?: ""
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putString("bedtime_reminder_last_shown_date", value).apply()

    var bedtimeReminderSnoozeUntil: Long
        get() = prefs.getLong("bedtime_reminder_snooze_until", 0L)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putLong("bedtime_reminder_snooze_until", value).apply()

    var updateNoticeEnabled: Boolean
        get() = prefs.getBoolean("update_notice_enabled", true)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putBoolean("update_notice_enabled", value).apply()

    var lastUpdateCheckMs: Long
        get() = prefs.getLong("last_update_check_ms", 0L)
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putLong("last_update_check_ms", value).apply()

    var lastNotifiedUpdateVersion: String
        get() = prefs.getString("last_notified_update_version", "") ?: ""
        @SuppressLint("UseKtx")
        set(value) = prefs.edit().putString("last_notified_update_version", value).apply()
}
