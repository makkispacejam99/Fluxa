package com.makkispacejam.fluxa.ui.components.core

import android.app.Application
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makkispacejam.fluxa.utils.stripHtml
import com.makkispacejam.fluxa.viewmodels.user.TranslationViewModel

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
    skipTranslation: Boolean = false
) {
    val context = LocalContext.current
    val translationViewModel: TranslationViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as Application
        )
    )
    val targetLang = translationViewModel.getTranslationLanguage()
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
