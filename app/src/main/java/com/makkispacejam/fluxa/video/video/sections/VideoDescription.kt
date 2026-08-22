@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.video.video.sections

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makkispacejam.fluxa.viewmodels.user.TranslationViewModel
import android.app.Application
import com.makkispacejam.fluxa.utils.stripHtml
import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R

import androidx.compose.ui.unit.sp

@Composable
fun VideoDescription(
    description: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit
) {
    val context = LocalContext.current
    val translationViewModel: TranslationViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as Application
        )
    )
    val targetLang = translationViewModel.getTranslationLanguage()
    
    val cleanDescription = remember(description) { description.stripHtml() }
    var translatedDescription by remember(cleanDescription, targetLang) { mutableStateOf(cleanDescription) }

    LaunchedEffect(cleanDescription, targetLang) {
        if (targetLang != null) {
            val result = translationViewModel.translate(cleanDescription, targetLang)
            translatedDescription = result.replace(Regex("\\n{3,}"), "\n\n")
        } else {
            translatedDescription = cleanDescription
        }
    }

    val displayedText = if (isExpanded) translatedDescription else {
        if (translatedDescription.length > 100) {
            translatedDescription.take(100).substringBeforeLast(" ") + "..."
        } else {
            translatedDescription
        }
    }

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        val annotatedString = buildAnnotatedString {
            val urls = extractUrls(displayedText)
            var lastIndex = 0

            urls.forEach { range ->
                val start = range.first
                val end = range.second

                append(displayedText.substring(lastIndex, start))

                pushStringAnnotation(tag = "URL", annotation = displayedText.substring(start, end))
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(displayedText.substring(start, end))
                }
                pop()
                lastIndex = end
            }
            append(displayedText.substring(lastIndex))
        }

        ClickableText(
            text = annotatedString,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            ),
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, annotation.item.toUri())
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
            }
        )

        Text(
            text = if (isExpanded) stringResource(R.string.show_less) else stringResource(R.string.show_more),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable { onExpandClick() }
        )
    }
}

fun extractUrls(text: String): List<Pair<Int, Int>> {
    val urlPattern = Regex("https?://\\S+")
    return urlPattern.findAll(text).map { it.range.first to it.range.last + 1 }.toList()
}
