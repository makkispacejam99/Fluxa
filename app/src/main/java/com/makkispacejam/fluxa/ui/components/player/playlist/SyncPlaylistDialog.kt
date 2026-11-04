@file:Suppress("FunctionName")

package com.makkispacejam.fluxa.ui.components.player.playlist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R

@Composable
fun SyncPlaylistDialog(
    playlistName: String,
    onDismiss: () -> Unit,
    onSync: suspend (onProgress: (SyncProgress) -> Unit) -> SyncResult
) {
    var isSyncing by remember { mutableStateOf(true) }
    var progress by remember { mutableStateOf(SyncProgress(0, 0)) }
    var result by remember { mutableStateOf<SyncResult?>(null) }

    LaunchedEffect(Unit) {
        result = onSync { progress = it }
        isSyncing = false
    }

    val titleText = stringResource(R.string.sync_playlist_title, playlistName)
    val loadingText = stringResource(R.string.sync_playlist_loading)
    val progressText = if (progress.total > 0) {
        stringResource(
            R.string.sync_playlist_progress,
            (progress.current + 1).coerceAtMost(progress.total),
            progress.total
        )
    } else loadingText
    val upToDateText = stringResource(R.string.sync_playlist_up_to_date)
    val noSourcesText = stringResource(R.string.sync_playlist_no_sources)
    val errorText = stringResource(R.string.sync_playlist_error)
    val doneText = stringResource(R.string.sync_playlist_done)

    AlertDialog(
        onDismissRequest = { if (!isSyncing) onDismiss() },
        icon = {
            Icon(
                imageVector = Icons.Rounded.Sync,
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
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isSyncing) {
                    Text(
                        text = progressText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    if (progress.total > 0) {
                        LinearProgressIndicator(
                            progress = { progress.current.toFloat() / progress.total },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                } else {
                    val current = result
                    val isSuccess = current is SyncResult.Success
                    val color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    val icon = if (isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Error
                    val text = when (current) {
                        is SyncResult.Success -> when {
                            current.failedSources > 0 -> stringResource(
                                R.string.sync_playlist_result_failed,
                                current.added,
                                current.skipped,
                                current.failedSources
                            )
                            current.added == 0 -> upToDateText
                            else -> stringResource(
                                R.string.sync_playlist_result,
                                current.added,
                                current.skipped
                            )
                        }
                        is SyncResult.Error -> when (current.reason) {
                            SyncError.NO_SOURCES -> noSourcesText
                            SyncError.PLAYLIST_MISSING -> errorText
                        }
                        null -> errorText
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                        Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !isSyncing) {
                Text(doneText)
            }
        }
    )
}

data class SyncProgress(val current: Int, val total: Int)

sealed class SyncResult {
    data class Success(val added: Int, val skipped: Int, val failedSources: Int) : SyncResult()
    data class Error(val reason: SyncError) : SyncResult()
}

enum class SyncError {
    NO_SOURCES, PLAYLIST_MISSING
}
