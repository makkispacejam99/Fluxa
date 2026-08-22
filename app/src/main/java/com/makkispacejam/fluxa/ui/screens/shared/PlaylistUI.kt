package com.makkispacejam.fluxa.ui.screens.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.core.FluxaAvatar
import com.makkispacejam.fluxa.ui.components.core.SkeletonLine
import com.makkispacejam.fluxa.ui.components.core.TranslatedText
import com.makkispacejam.fluxa.data.newpipe.FluxaStreamItem

@Composable
fun PlaylistHeader(
    playlistTitle: String,
    playlistVideos: List<FluxaStreamItem>,
    isUserPlaylist: Boolean,
    isEditMode: Boolean,
    onToggleEditMode: () -> Unit,
    onImportClick: () -> Unit,
    showEditButton: Boolean = isUserPlaylist,
    showShareButton: Boolean = false,
    onShareClick: () -> Unit = {}
) {
    if (playlistTitle.isBlank() && playlistVideos.isEmpty()) {
        SkeletonLine(modifier = Modifier.width(200.dp), height = 28.dp)
        Spacer(modifier = Modifier.height(8.dp))
        SkeletonLine(modifier = Modifier.width(120.dp), height = 14.dp)
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                TranslatedText(text = playlistTitle, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Text(text = stringResource(R.string.playlist_count_format, playlistVideos.size), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (isUserPlaylist || showEditButton || showShareButton) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (showShareButton) {
                        IconButton(onClick = onShareClick) {
                            Icon(
                                imageVector = Icons.Rounded.IosShare,
                                contentDescription = stringResource(R.string.share_short),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (isUserPlaylist) {
                        IconButton(onClick = onImportClick) {
                            Icon(
                                imageVector = Icons.Rounded.CloudDownload,
                                contentDescription = stringResource(R.string.import_playlist),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    if (playlistVideos.isNotEmpty()) {
                        IconButton(onClick = onToggleEditMode) {
                            Icon(
                                imageVector = if (isEditMode) Icons.Rounded.Close else Icons.Rounded.Edit,
                                contentDescription = stringResource(R.string.edit),
                                tint = if (isEditMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlaylistEditModeToolbar(
    selectedVideos: Set<String>,
    playlistVideos: List<FluxaStreamItem>,
    onSelectionChanged: (Set<String>) -> Unit,
    onDeleteSelected: () -> Unit = {}
) {
    Spacer(modifier = Modifier.height(16.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = {
                val newSelection = if (selectedVideos.size == playlistVideos.size) {
                    emptySet()
                } else {
                    playlistVideos.map { video ->
                        if (video.url.contains("v=")) video.url.substringAfter("v=").substringBefore("&")
                        else video.url
                    }.toSet()
                }
                onSelectionChanged(newSelection)
            },
            modifier = Modifier.weight(1f)
        ) {
            Text(if (selectedVideos.size == playlistVideos.size) stringResource(R.string.unselect_all) else stringResource(R.string.select_all))
        }

        Button(
            onClick = { if (selectedVideos.isNotEmpty()) onDeleteSelected() },
            enabled = selectedVideos.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(stringResource(R.string.delete_count, selectedVideos.size))
        }
    }
}

@Composable
fun PlaylistActionButtons(
    playlistVideos: List<FluxaStreamItem>,
    onPlay: (Int, Boolean) -> Unit
) {
    Spacer(modifier = Modifier.height(20.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = { if (playlistVideos.isNotEmpty()) onPlay(0, false) },
            enabled = playlistVideos.isNotEmpty(),
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(stringResource(R.string.play))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Rounded.PlayCircle, contentDescription = null)
        }
        OutlinedButton(
            onClick = {
                if (playlistVideos.isNotEmpty()) {
                    val randomIndex = (playlistVideos.indices).random()
                    onPlay(randomIndex, true)
                }
            },
            enabled = playlistVideos.isNotEmpty(),
            modifier = Modifier.weight(1f).height(48.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text(stringResource(R.string.shuffle))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(imageVector = Icons.Rounded.Shuffle, contentDescription = null)
        }
    }
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
fun PlaylistVideoHorizontalSkeleton() {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(width = 120.dp, height = 68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                SkeletonLine(modifier = Modifier.fillMaxWidth(0.9f), height = 14.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonLine(modifier = Modifier.fillMaxWidth(0.5f), height = 12.dp)
                Spacer(modifier = Modifier.height(4.dp))
                SkeletonLine(modifier = Modifier.fillMaxWidth(0.3f), height = 10.dp)
            }
        }
    }
}

@Composable
fun PlaylistEmptyState() {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp, bottom = 100.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(imageVector = Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        Text(text = stringResource(R.string.no_videos_playlist), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        Text(text = stringResource(R.string.add_videos_first), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PlaylistVideoItemHorizontal(
    video: FluxaStreamItem,
    channelName: String,
    formattedViews: String,
    formattedDuration: String,
    progress: Float,
    isWatched: Boolean,
    isEditMode: Boolean,
    isSelected: Boolean,
    resolvedAvatar: String,
    publishedTime: String,
    onToggleSelection: () -> Unit,
    onChannelClick: () -> Unit,
    onVideoClick: () -> Unit,
    onOptionsClick: () -> Unit
) {
    Surface(
        onClick = { if (isEditMode) onToggleSelection() else onVideoClick() },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min).padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isEditMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Box(modifier = Modifier.size(width = 125.dp, height = 80.dp)) {
                AsyncImage(
                    model = video.thumbnail,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Row(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomEnd).padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isWatched) {
                        Box(
                            modifier = Modifier.background(Color.Black, RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(text = stringResource(R.string.watched_badge), style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    } else Spacer(modifier = Modifier.width(1.dp))
                    if (formattedDuration.isNotEmpty()) {
                        Box(
                            modifier = Modifier.background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(formattedDuration, style = MaterialTheme.typography.labelSmall, color = Color.White)
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                TranslatedText(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FluxaAvatar(
                        avatarUrl = resolvedAvatar.ifEmpty { null },
                        modifier = Modifier.size(20.dp).clickable(enabled = !isEditMode, onClick = onChannelClick),
                        size = 20.dp,
                        iconSize = 14.dp,
                        placeholderName = channelName
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(enabled = !isEditMode, onClick = onChannelClick)
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (formattedViews.isNotEmpty()) {
                        Text(formattedViews, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(" · ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(publishedTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (progress > 0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }

            IconButton(onClick = onOptionsClick, enabled = !isEditMode) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.delete_videos_title)) },
        text = { Text(stringResource(R.string.delete_videos_confirm, count)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(24.dp)
            ) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(24.dp)) { Text(stringResource(R.string.cancel_btn)) }
        }
    )
}
