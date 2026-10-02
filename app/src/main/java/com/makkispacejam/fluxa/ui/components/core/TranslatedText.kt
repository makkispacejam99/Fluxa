@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.ui.components.core

import android.app.Application
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makkispacejam.fluxa.utils.stripHtml
import com.makkispacejam.fluxa.viewmodels.user.TranslationViewModel
import android.content.Intent
import androidx.core.net.toUri

// Texto a traducir
@Composable
fun TranslatedText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    color: Color = Color.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    skipTranslation: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val translationViewModel: TranslationViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as Application
        )
    )
    val targetLang = translationViewModel.getTranslationLanguage()

    val fullUrls = remember(text) {
        Regex("href=\"([^\"]+)\"").findAll(text).map { it.groupValues[1] }.toList()
    }

    val cleanText = remember(text) { text.stripHtml() }
    var translatedText by remember(cleanText, targetLang, skipTranslation) { 
        mutableStateOf(
            if (skipTranslation) cleanText
            else translationViewModel.getCachedTranslation(cleanText, targetLang) ?: cleanText
        )
    }

    LaunchedEffect(cleanText, targetLang, skipTranslation) {
        if (!skipTranslation && targetLang != null) {
            val result = translationViewModel.translate(cleanText, targetLang)
            translatedText = result
        } else {
            translatedText = cleanText
        }
    }

    val annotatedString = buildAnnotatedString {
        val displayUrls = Regex("https?://\\S+").findAll(translatedText).toList()
        var lastIndex = 0

        displayUrls.forEachIndexed { index, matchResult ->
            val start = matchResult.range.first
            val end = matchResult.range.last + 1

            append(translatedText.substring(lastIndex, start))

            val realUrl = fullUrls.getOrNull(index) ?: matchResult.value
            pushStringAnnotation(tag = "URL", annotation = realUrl)
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(translatedText.substring(start, end))
            }
            pop()
            lastIndex = end
        }
        append(translatedText.substring(lastIndex))
    }

    val hasUrls = remember(translatedText) { Regex("https?://\\S+").containsMatchIn(translatedText) || fullUrls.isNotEmpty() }

    if (hasUrls) {
        ClickableText(
            text = annotatedString,
            style = style.copy(color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color, fontWeight = fontWeight ?: style.fontWeight),
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier,
            onClick = { offset ->
                val annotation = annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()
                if (annotation != null) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, annotation.item.toUri()))
                    } catch (_: Exception) {}
                } else {
                    onClick?.invoke()
                }
            }
        )
    } else {
        Text(
            text = translatedText,
            style = style,
            fontWeight = fontWeight,
            maxLines = maxLines,
            color = color,
            overflow = overflow,
            modifier = modifier
        )
    }
}

