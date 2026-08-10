package com.makkispacejam.fluxa.video.video.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.ui.components.player.shared.WavySlider

// Línea de tiempo y slider del reproductor horizontal
@Composable
fun PlayerBottomBar(
    currentPositionMs: Long,
    totalDurationMs: Long,
    progressPercent: Float,
    bufferedPercent: Float,
    isFullScreen: Boolean,
    isUserDraggingSlider: Boolean,
    onSliderValueChange: (Float) -> Unit,
    onSliderValueFinished: () -> Unit,
    onFullScreenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRemainingTime by remember { mutableStateOf(false) }

    val timeLabel = remember(currentPositionMs, totalDurationMs, showRemainingTime) {
        if (showRemainingTime) {
            val remaining = (totalDurationMs - currentPositionMs).coerceAtLeast(0L)
            "-${formatTime(remaining)}"
        } else {
            formatTime(currentPositionMs)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { showRemainingTime = !showRemainingTime }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = timeLabel,
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            WavySlider(
                value = if (isUserDraggingSlider) (currentPositionMs.toFloat() / totalDurationMs.coerceAtLeast(1L).toFloat()) else progressPercent,
                bufferedValue = bufferedPercent,
                onValueChange = onSliderValueChange,
                onValueChangeFinished = onSliderValueFinished,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = onFullScreenClick,
                modifier = Modifier
                    .padding(start = 2.dp)
                    .size(38.dp)
            ) {
                Icon(
                    imageVector = if (isFullScreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
