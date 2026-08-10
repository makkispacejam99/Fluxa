package com.makkispacejam.fluxa.ui.components.player.queue

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.viewmodels.player.PlayerViewModel
import com.makkispacejam.fluxa.viewmodels.player.RepeatMode

// Panel extensible de la cola de reproducción
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueuePanel(
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val state = playerViewModel.playbackState
    val queue = state.playlistQueue
    val currentIndex = state.currentIndex
    var isEditMode by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 14.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.68f)
                .padding(horizontal = 14.dp)
        ) {
            // Encabezado
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditMode) stringResource(R.string.editing_queue) else stringResource(R.string.playback_queue),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isEditMode) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botón de edición
                    IconButton(onClick = { isEditMode = !isEditMode }, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Rounded.Check else Icons.Rounded.Edit,
                            contentDescription = stringResource(R.string.edit),
                            tint = if (isEditMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    if (!isEditMode) {
                        // Botón de shuffle
                        IconButton(onClick = { playerViewModel.toggleShuffle() }, modifier = Modifier.size(40.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = stringResource(R.string.shuffle),
                                tint = if (state.isShuffled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        
                        // Botón de repetir
                        IconButton(onClick = { playerViewModel.toggleRepeatMode() }, modifier = Modifier.size(40.dp)) {
                            val icon = when (state.repeatMode) {
                                RepeatMode.OFF -> Icons.Rounded.Repeat
                                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                RepeatMode.ALL -> Icons.Rounded.Repeat
                            }
                            val tint = if (state.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(R.string.repeat),
                                tint = tint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Botón de cerrar
                    IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.close_btn),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            if (queue.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.queue_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.92f to Color.Black,
                                    1f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        },
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(queue, key = { _, item -> System.identityHashCode(item) }) { index, item ->
                        QueueItemRow(
                            modifier = Modifier.animateItem(),
                            item = item,
                            isPlaying = index == currentIndex,
                            isEditMode = isEditMode,
                            onRemove = { playerViewModel.removeFromQueue(index) },
                            onMoveUp = if (index > 0) { { playerViewModel.moveQueueItem(index, index - 1) } } else null,
                            onMoveDown = if (index < queue.size - 1) { { playerViewModel.moveQueueItem(index, index + 1) } } else null,
                            onClick = { playerViewModel.playFromQueue(index) }
                        )
                    }
                }
            }
        }
    }
}
