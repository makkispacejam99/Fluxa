package com.makkispacejam.fluxa.viewmodels.user

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makkispacejam.fluxa.data.TranslationManager
import com.makkispacejam.fluxa.data.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class TranslationViewModel(application: Application) : AndroidViewModel(application) {
    private val translationManager = TranslationManager()
    private val prefs = UserPreferences(application)

    private val _downloadedLanguages = MutableStateFlow<Set<String>>(emptySet())
    val downloadedLanguages: StateFlow<Set<String>> = _downloadedLanguages

    val downloadingModels = translationManager.downloadingModels

    private val translationCache = mutableMapOf<String, String>()

    init {
        refreshDownloadedModels()
    }

    fun refreshDownloadedModels() {
        _downloadedLanguages.value = prefs.downloadedTranslationLanguages
    }

    fun downloadModel(langCode: String) {
        viewModelScope.launch {
            translationManager.downloadModel(langCode)
            prefs.downloadedTranslationLanguages += langCode
            _downloadedLanguages.value = prefs.downloadedTranslationLanguages
        }
    }

    fun deleteModel(langCode: String) {
        viewModelScope.launch {
            translationManager.deleteModel(langCode)
            prefs.downloadedTranslationLanguages -= langCode
            _downloadedLanguages.value = prefs.downloadedTranslationLanguages
            if (prefs.translationLanguage == langCode) {
                prefs.translationLanguage = null
            }
        }
    }

    fun setTranslationLanguage(langCode: String?) {
        prefs.translationLanguage = langCode
    }

    fun getTranslationLanguage(): String? = prefs.translationLanguage

    fun getCachedTranslation(text: String, targetLang: String?): String? {
        if (targetLang == null) return null
        return translationCache["${text}_${targetLang}"]
    }

    suspend fun translate(text: String, targetLang: String): String {
        val cacheKey = "${text}_${targetLang}"
        translationCache[cacheKey]?.let { return it }

        val result = translationManager.translate(text, targetLang = targetLang)
        translationCache[cacheKey] = result
        return result
    }
}
