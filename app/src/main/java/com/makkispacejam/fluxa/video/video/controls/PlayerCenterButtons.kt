package com.makkispacejam.fluxa.video.video.controls

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

// Controles centrales del reproductor horizontal
@Composable
fun PlayerCenterButtons(
    modifier: Modifier = Modifier,
    isPlaying: Boolean,
    isFullScreen: Boolean,
    isLoading: Boolean = false,
    queueSize: Int = 0,
    onPlayPause: () -> Unit,
    onRewindClick: () -> Unit,
    onForwardClick: () -> Unit,
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {}

) {
    Row(
        modifier = modifier.heightIn(max = 80.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (isFullScreen) 12.dp else 8.dp)
    ) {
        if (queueSize > 1) {
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "prevScale")

            Box(
                modifier = Modifier
                    .size(
                        width = if (isFullScreen) 64.dp else 58.dp,
                        height = if (isFullScreen) 72.dp else 66.dp
                    )
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onPreviousClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.SkipPrevious,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(if (isFullScreen) 32.dp else 28.dp)
                )
            }
        }

        val rewindInteraction = remember { MutableInteractionSource() }
        val isRewindPressed by rewindInteraction.collectIsPressedAsState()
        val rewindScale by animateFloatAsState(if (isRewindPressed) 0.9f else 1f, label = "rewindScale")

        Box(
            modifier = Modifier
                .size(
                    width = if (isFullScreen) 64.dp else 58.dp,
                    height = if (isFullScreen) 72.dp else 66.dp
                )
                .graphicsLayer { scaleX = rewindScale; scaleY = rewindScale }
                .background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = rewindInteraction,
                    indication = null,
                    onClick = onRewindClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Replay10,
                null,
                tint = Color.White,
                modifier = Modifier.size(if (isFullScreen) 38.dp else 34.dp)
            )
        }

        val playInteraction = remember { MutableInteractionSource() }
        val isPlayPressed by playInteraction.collectIsPressedAsState()
        val playScale by animateFloatAsState(if (isPlayPressed) 0.9f else 1f, label = "playScale")
        val cornerSize by animateDpAsState(targetValue = if (isPlaying) 14.dp else 30.dp, label = "playPauseCorner")

        Box(
            modifier = Modifier
                .size(if (isFullScreen) 72.dp else 66.dp)
                .graphicsLayer { scaleX = playScale; scaleY = playScale }
                .background(
                    Color.Black.copy(alpha = 0.50f),
                    RoundedCornerShape(cornerSize)
                )
                .clickable(
                    interactionSource = playInteraction,
                    indication = null,
                    onClick = onPlayPause
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(if (isFullScreen) 44.dp else 40.dp)
                )
            } else {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(spring(dampingRatio = Spring.DampingRatioLowBouncy))) togetherWith fadeOut(spring()) using SizeTransform(clip = false)
                    },
                    label = "playPauseAnimation"
                ) { playing ->
                    Icon(
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(if (isFullScreen) 46.dp else 42.dp)
                    )
                }
            }
        }

        val forwardInteraction = remember { MutableInteractionSource() }
        val isForwardPressed by forwardInteraction.collectIsPressedAsState()
        val forwardScale by animateFloatAsState(if (isForwardPressed) 0.9f else 1f, label = "forwardScale")

        Box(
            modifier = Modifier
                .size(
                    width = if (isFullScreen) 64.dp else 58.dp,
                    height = if (isFullScreen) 72.dp else 66.dp
                )
                .graphicsLayer { scaleX = forwardScale; scaleY = forwardScale }
                .background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(14.dp))
                .clickable(
                    interactionSource = forwardInteraction,
                    indication = null,
                    onClick = onForwardClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Forward10,
                null,
                tint = Color.White,
                modifier = Modifier.size(if (isFullScreen) 38.dp else 34.dp)
            )
        }

        if (queueSize > 1) {
            val nextInteraction = remember { MutableInteractionSource() }
            val isNextPressed by nextInteraction.collectIsPressedAsState()
            val nextScale by animateFloatAsState(if (isNextPressed) 0.9f else 1f, label = "nextScale")

            Box(
                modifier = Modifier
                    .size(
                        width = if (isFullScreen) 64.dp else 58.dp,
                        height = if (isFullScreen) 72.dp else 66.dp
                    )
                    .graphicsLayer { scaleX = nextScale; scaleY = nextScale }
                    .background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = nextInteraction,
                        indication = null,
                        onClick = onNextClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.SkipNext,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(if (isFullScreen) 32.dp else 28.dp)
                )
            }
        }
    }
}
