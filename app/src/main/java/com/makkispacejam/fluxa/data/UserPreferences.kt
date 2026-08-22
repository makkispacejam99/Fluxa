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
}
