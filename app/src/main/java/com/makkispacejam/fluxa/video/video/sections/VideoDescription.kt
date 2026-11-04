@file:Suppress("FunctionName", "DEPRECATION")

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
import com.makkispacejam.fluxa.utils.restoreLineBreaks
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
    
    val fullUrls = remember(description) {
        Regex("href=\"([^\"]+)\"").findAll(description).map { it.groupValues[1] }.toList()
    }

    val cleanDescription = remember(description) { description.stripHtml().restoreLineBreaks() }
    var translatedDescription by remember(cleanDescription, targetLang) { mutableStateOf(cleanDescription) }

    LaunchedEffect(cleanDescription, targetLang) {
        if (targetLang != null) {
            val result = translationViewModel.translate(cleanDescription, targetLang)
            translatedDescription = result.replace(Regex("\\n{3,}"), "\n\n")
        } else {
            translatedDescription = cleanDescription
        }
    }

    val textToDisplay = if (isExpanded) translatedDescription else {
        if (translatedDescription.length > 100) {
            val preview = buildString {
                for (line in translatedDescription.lineSequence()) {
                    if (length + line.length > 100) break
                    append(line).append('\n')
                }
            }.trimEnd()
            if (preview.isEmpty() || preview.length >= translatedDescription.length) {
                translatedDescription.take(100).substringBeforeLast(" ").trimEnd()
            } else {
                "$preview..."
            }
        } else {
            translatedDescription
        }
    }

    val displayLines = textToDisplay.split('\n')
    val chapterLinePattern = Regex("^\\s*\\d{1,2}:\\d{2}(?::\\d{2})?(?=\\s|$)")
    val primary = MaterialTheme.colorScheme.primary

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        val annotatedString = buildAnnotatedString {
            val urlRegex = Regex("https?://\\S+")
            var urlIndex = 0

            displayLines.forEachIndexed { lineIndex, line ->
                val isChapter = chapterLinePattern.containsMatchIn(line)

                fun appendSegment(segment: String) {
                    if (isChapter) {
                        withStyle(
                            style = SpanStyle(
                                color = primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(segment)
                        }
                    } else {
                        append(segment)
                    }
                }

                var lastIndex = 0
                urlRegex.findAll(line).forEach { matchResult ->
                    val start = matchResult.range.first
                    val end = matchResult.range.last + 1

                    appendSegment(line.substring(lastIndex, start))

                    val realUrl = fullUrls.getOrNull(urlIndex) ?: matchResult.value
                    urlIndex++
                    pushStringAnnotation(tag = "URL", annotation = realUrl)
                    withStyle(
                        style = SpanStyle(
                            color = primary,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        append(line.substring(start, end))
                    }
                    pop()
                    lastIndex = end
                }
                appendSegment(line.substring(lastIndex))

                if (lineIndex < displayLines.size - 1) append('\n')
            }
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
