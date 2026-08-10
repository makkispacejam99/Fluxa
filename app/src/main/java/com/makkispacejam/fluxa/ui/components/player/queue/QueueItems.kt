package com.makkispacejam.fluxa.ui.components.player.queue

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.core.TranslatedText
import com.makkispacejam.fluxa.utils.MusicChannelUtils
import com.makkispacejam.fluxa.viewmodels.player.QueueItem

// Elementos de la cola de reproducción
@Composable
fun QueueItemRow(
    modifier: Modifier = Modifier,
    item: QueueItem,
    isPlaying: Boolean,
    isEditMode: Boolean = false,
    onRemove: () -> Unit = {},
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent
    val indicatorColor = if (isPlaying) MaterialTheme.colorScheme.primary else Color.Transparent
    var isDragging by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    val elevation by animateDpAsState(if (isDragging) 8.dp else 0.dp, label = "elevation")
    val scale by animateFloatAsState(if (isDragging) 1.04f else 1f, label = "scale")

    val currentOnMoveUp by rememberUpdatedState(onMoveUp)
    val currentOnMoveDown by rememberUpdatedState(onMoveDown)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                shadowElevation = elevation.toPx()
                scaleX = scale
                scaleY = scale
                shape = RoundedCornerShape(11.dp)
                clip = true
            }
            .background(if (isDragging) MaterialTheme.colorScheme.surfaceVariant else backgroundColor)
            .clickable(enabled = !isEditMode) { onClick() }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isEditMode) {
            Icon(
                imageVector = Icons.Rounded.DragHandle,
                contentDescription = stringResource(R.string.reorder),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(22.dp)
                    .pointerInput(Unit) {
                        var dragAmount = 0f
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                dragAmount = 0f
                            },
                            onDragEnd = { isDragging = false },
                            onDragCancel = { isDragging = false },
                            onDrag = { change, dragAmountDelta ->
                                change.consume()
                                dragAmount += dragAmountDelta.y
                                val threshold = 52.dp.toPx()
                                if (dragAmount > threshold) {
                                    currentOnMoveDown?.invoke()
                                    dragAmount = 0f
                                } else if (dragAmount < -threshold) {
                                    currentOnMoveUp?.invoke()
                                    dragAmount = 0f
                                }
                            }
                        )
                    },
                tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        } else {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(indicatorColor)
            )
        }

        AsyncImage(
            model = item.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(width = 90.dp, height = 50.dp)
                .clip(RoundedCornerShape(7.dp)),
            contentScale = ContentScale.Crop
        )

        Column(modifier = Modifier.weight(1f)) {
            TranslatedText(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                skipTranslation = MusicChannelUtils.isMusicContent(item.channel, item.title)
            )
            Text(
                text = item.channel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (isEditMode) {
            IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(22.dp))
            }
        }
    }
}
