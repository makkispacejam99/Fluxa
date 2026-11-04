@file:Suppress("SuppressedCancellationException")

package com.makkispacejam.fluxa.data

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.languageid.LanguageIdentification
import kotlin.text.RegexOption
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/* Lógica e instrucciones de los modelos
de traducción de Google ML Kit */

class TranslationManager {
    private val modelManager: RemoteModelManager? = try { RemoteModelManager.getInstance() } catch (_: Exception) { null }

    private val _downloadingModels = MutableStateFlow<Set<String>>(emptySet())
    val downloadingModels: StateFlow<Set<String>> = _downloadingModels

    // Descarga de modelo de traducción
    suspend fun downloadModel(langCode: String) {
        val model = TranslateRemoteModel.Builder(langCode).build()
        val conditions = DownloadConditions.Builder()
            .build()
        
        _downloadingModels.value += langCode
        try {
            modelManager?.download(model, conditions)?.await()
        } finally {
            _downloadingModels.value -= langCode
        }
    }

    // Borrar modelo
    suspend fun deleteModel(langCode: String) {
        val model = TranslateRemoteModel.Builder(langCode).build()
        modelManager?.deleteDownloadedModel(model)?.await()
    }

    // Detección de idioma
    suspend fun detectLanguage(text: String): String? {
        val languageIdentifier = LanguageIdentification.getClient()
        return try {
            val cleanText = text.replace(Regex("[\\p{So}\\p{Cn}]"), "")
            if (cleanText.isBlank()) return null
            
            val possibleLanguages = languageIdentifier.identifyPossibleLanguages(cleanText).await()
            val bestMatch = possibleLanguages.firstOrNull()

            if (bestMatch == null || bestMatch.languageTag == "und" || bestMatch.confidence < 0.4f) {
                null
            } else {
                bestMatch.languageTag
            }
        } catch (_: Exception) {
            null
        } finally {
            languageIdentifier.close()
        }
    }

    // Traducción
    suspend fun translate(text: String, targetLang: String): String {
        if (text.isBlank()) return text

        val detectedLang = detectLanguage(text)
        if (detectedLang == targetLang) return text
        if (detectedLang == null && text.length < 30) return text
        
        val sourceLang = detectedLang ?: TranslateLanguage.ENGLISH

        return try {
            performTranslation(text, sourceLang, targetLang)
        } catch (_: Exception) {
            if (sourceLang != TranslateLanguage.ENGLISH) {
                try {
                    performTranslation(text, TranslateLanguage.ENGLISH, targetLang)
                } catch (_: Exception) {
                    text
                }
            } else {
                text
            }
        }
    }

    // Instrucciones de traducción
    private suspend fun performTranslation(
        text: String,
        sourceLang: String,
        targetLang: String
    ): String {
        if (sourceLang == targetLang) return text
        
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLang)
            .setTargetLanguage(targetLang)
            .build()
        val translator = Translation.getClient(options)
        
        return translator.use { translator ->
            translator.downloadModelIfNeeded().await()

            val normalizedText = text.replace("\r\n", "\n").replace('\r', '\n')
            val translatedLines = normalizedText.split('\n').map { line ->
                if (line.isBlank()) {
                    line
                } else {
                    val links = mutableListOf<String>()
                    val placeholderText = Regex("https?://\\S+").replace(line) { matchResult ->
                        links.add(matchResult.value)
                        " <TKN_${links.size - 1}> "
                    }

                    var translatedLine = translator.translate(placeholderText).await() ?: placeholderText

                    links.forEachIndexed { index, link ->
                        val recoveryRegex = Regex("<\\s*TKN\\s*_\\s*$index\\s*>", RegexOption.IGNORE_CASE)
                        translatedLine = recoveryRegex.replace(translatedLine, link)
                    }

                    translatedLine
                }
            }

            translatedLines
                .joinToString("\n")
                .replace(Regex(" +"), " ")
                .replace(Regex(" *\n+ *")) { it.value.replace(" ", "") }
                .trim()
        }
    }
}
