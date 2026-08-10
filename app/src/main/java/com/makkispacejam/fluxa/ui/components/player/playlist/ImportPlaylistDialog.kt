package com.makkispacejam.fluxa.ui.components.player.playlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import kotlinx.coroutines.launch

@Composable
fun ImportPlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onImport: suspend (String) -> ImportResult
) {
    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<ImportResult?>(null) }

    val scope = rememberCoroutineScope()
    val titleText = stringResource(R.string.import_playlist_title, playlistName)
    val hintUrlText = stringResource(R.string.import_playlist_hint)
    val instructionsText = stringResource(R.string.import_playlist_instructions)
    val noteText = stringResource(R.string.import_playlist_note)
    val cancelText = stringResource(R.string.cancel_btn)
    val acceptText = stringResource(R.string.accept_btn)
    val importingText = stringResource(R.string.import_playlist_loading)
    val successText = stringResource(R.string.import_playlist_success)
    val errorText = stringResource(R.string.import_playlist_error)
    val emptyErrorText = stringResource(R.string.import_playlist_empty)
    val noUrlText = stringResource(R.string.import_playlist_no_url)

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Rounded.CloudDownload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = instructionsText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; result = null },
                    label = { Text(hintUrlText) },
                    singleLine = true,
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )

                Text(
                    text = noteText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                AnimatedVisibility(visible = isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(importingText, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                result?.let { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    ) {
                        val icon = when {
                            r is ImportResult.Success -> Icons.Rounded.CheckCircle
                            else -> Icons.Rounded.Error
                        }
                        val color = when {
                            r is ImportResult.Success -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                        val text = when {
                            r is ImportResult.Success -> {
                                if (r.added == 0 && r.skipped == 0) emptyErrorText
                                else stringResource(R.string.import_playlist_result, r.added, r.skipped)
                            }
                            r is ImportResult.Error -> when (r.reason) {
                                ImportError.NO_URL -> noUrlText
                                ImportError.FETCH_FAILED -> errorText
                                ImportError.EMPTY -> emptyErrorText
                            }
                            else -> ""
                        }
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    result = null
                    isLoading = true
                    scope.launch {
                        val trimmed = url.trim()
                        val res = onImport(trimmed)
                        result = res
                        isLoading = false
                        if (res is ImportResult.Success && res.added > 0) {
                            kotlinx.coroutines.delay(800)
                            onDismiss()
                        }
                    }
                },
                enabled = !isLoading && url.isNotBlank(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(acceptText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isLoading,
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(cancelText)
            }
        }
    )
}

sealed class ImportResult {
    data class Success(val added: Int, val skipped: Int) : ImportResult()
    data class Error(val reason: ImportError) : ImportResult()
}

enum class ImportError {
    NO_URL, FETCH_FAILED, EMPTY
}
