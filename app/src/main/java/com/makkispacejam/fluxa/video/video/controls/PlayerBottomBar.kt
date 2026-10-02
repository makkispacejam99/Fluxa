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
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.player.shared.WavySlider
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamSegment

@Composable
fun PlayerBottomBar(
    modifier: Modifier = Modifier,
    currentPositionMs: Long,
    totalDurationMs: Long,
    progressPercent: Float,
    bufferedPercent: Float,
    isFullScreen: Boolean,
    isUserDraggingSlider: Boolean,
    onSliderValueChange: (Float) -> Unit,
    onSliderValueFinished: () -> Unit,
    onFullScreenClick: () -> Unit,
    segments: List<StreamSegment> = emptyList(),
    hoverSegmentTitle: String? = null,
    isPlaying: Boolean = false,
    isLoading: Boolean = false,
    queueSize: Int = 0,
    onPlayPause: () -> Unit = {},
    onRewindClick: () -> Unit = {},
    onForwardClick: () -> Unit = {},
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    availableAudioTracks: List<AudioStream> = emptyList(),
    onAudioTracksClick: () -> Unit = {},
    onSpeedClick: () -> Unit = {},
    onAudioNormalizeClick: () -> Unit = {},
    onPipClick: () -> Unit = {},
    showMoreOptionsBottomSheet: Boolean = false,
    onDismissMoreOptions: () -> Unit = {}
) {
    var showRemainingTime by remember { mutableStateOf(false) }

    val markerFractions = remember(segments, totalDurationMs) {
        if (totalDurationMs > 0) {
            segments.map { (it.startTimeSeconds * 1000L).toFloat() / totalDurationMs.toFloat() }
        } else emptyList()
    }

    val timeLabel = remember(currentPositionMs, totalDurationMs, showRemainingTime) {
        if (showRemainingTime) {
            val remaining = (totalDurationMs - currentPositionMs).coerceAtLeast(0L)
            "-${formatTime(remaining)}"
        } else {
            formatTime(currentPositionMs)
        }
    }

    val hasValidAudioTracks = remember(availableAudioTracks) {
        availableAudioTracks.any { it.audioLocale != null || !it.audioTrackName.isNullOrBlank() }
    }

    val accentContainerColor = MaterialTheme.colorScheme.primaryContainer
    val onAccentContainerColor = MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        WavySlider(
            value = if (isUserDraggingSlider) (currentPositionMs.toFloat() / totalDurationMs.coerceAtLeast(1L).toFloat()) else progressPercent,
            bufferedValue = bufferedPercent,
            onValueChange = onSliderValueChange,
            onValueChangeFinished = onSliderValueFinished,
            markers = markerFractions,
            hoverTitle = hoverSegmentTitle,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 380.dp
            val btnHeight = if (compact) 36.dp else 42.dp
            val btnIconSize = if (compact) 18.dp else 22.dp
            val actionBtnSize = if (compact) 36.dp else 42.dp

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else 6.dp)
                ) {
                    val playInteraction = remember { MutableInteractionSource() }
                    val isPlayPressed by playInteraction.collectIsPressedAsState()
                    val playScale by animateFloatAsState(
                        targetValue = if (isPlayPressed) 0.88f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                        label = "playScale"
                    )
                    val playCorner by animateDpAsState(
                        targetValue = if (isPlayPressed) 14.dp else 100.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
                        label = "playCorner"
                    )

                    Box(
                        modifier = Modifier
                            .heightIn(max = 40.dp)
                            .height(btnHeight)
                            .graphicsLayer { scaleX = playScale; scaleY = playScale }
                            .background(accentContainerColor, RoundedCornerShape(playCorner))
                            .clickable(
                                interactionSource = playInteraction,
                                indication = null,
                                onClick = onPlayPause
                            )
                            .padding(horizontal = if (compact) 10.dp else 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = onAccentContainerColor,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(btnIconSize)
                            )
                        } else {
                            AnimatedContent(
                                targetState = isPlaying,
                                transitionSpec = {
                                    (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))) togetherWith (fadeOut(spring(stiffness = Spring.StiffnessMediumLow)) + scaleOut(spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow))) using SizeTransform(clip = false)
                                },
                                label = "playPauseAnimation"
                            ) { playing ->
                                Icon(
                                    imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = onAccentContainerColor,
                                    modifier = Modifier.size(if (compact) 20.dp else 22.dp)
                                )
                            }
                        }
                    }

                    if (queueSize > 1) {
                        val prevInteraction = remember { MutableInteractionSource() }
                        val isPrevPressed by prevInteraction.collectIsPressedAsState()
                        val prevScale by animateFloatAsState(if (isPrevPressed) 0.88f else 1f, label = "prevScale")

                        Box(
                            modifier = Modifier
                                .sizeIn(maxWidth = 48.dp, maxHeight = 48.dp)
                                .size(actionBtnSize)
                                .graphicsLayer { scaleX = prevScale; scaleY = prevScale }
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                .clickable(
                                    interactionSource = prevInteraction,
                                    indication = null,
                                    onClick = onPreviousClick
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.previous),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (compact) 16.dp else 18.dp)
                            )
                        }
                    }

                    val rewindInteraction = remember { MutableInteractionSource() }
                    val isRewindPressed by rewindInteraction.collectIsPressedAsState()
                    val rewindScale by animateFloatAsState(if (isRewindPressed) 0.88f else 1f, label = "rewindScale")

                    Box(
                        modifier = Modifier
                            .sizeIn(maxWidth = 48.dp, maxHeight = 48.dp)
                            .size(actionBtnSize)
                            .graphicsLayer { scaleX = rewindScale; scaleY = rewindScale }
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .clickable(
                                interactionSource = rewindInteraction,
                                indication = null,
                                onClick = onRewindClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.rewind10seg),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(btnIconSize)
                        )
                    }

                    val forwardInteraction = remember { MutableInteractionSource() }
                    val isForwardPressed by forwardInteraction.collectIsPressedAsState()
                    val forwardScale by animateFloatAsState(if (isForwardPressed) 0.88f else 1f, label = "forwardScale")

                    Box(
                        modifier = Modifier
                            .sizeIn(maxWidth = 48.dp, maxHeight = 48.dp)
                            .size(actionBtnSize)
                            .graphicsLayer { scaleX = forwardScale; scaleY = forwardScale }
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                            .clickable(
                                interactionSource = forwardInteraction,
                                indication = null,
                                onClick = onForwardClick
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.forward10seg),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(btnIconSize)
                        )
                    }

                    if (queueSize > 1) {
                        val nextInteraction = remember { MutableInteractionSource() }
                        val isNextPressed by nextInteraction.collectIsPressedAsState()
                        val nextScale by animateFloatAsState(if (isNextPressed) 0.88f else 1f, label = "nextScale")

                        Box(
                            modifier = Modifier
                                .sizeIn(maxWidth = 48.dp, maxHeight = 48.dp)
                                .size(actionBtnSize)
                                .graphicsLayer { scaleX = nextScale; scaleY = nextScale }
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                                .clickable(
                                    interactionSource = nextInteraction,
                                    indication = null,
                                    onClick = onNextClick
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.next),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(if (compact) 16.dp else 18.dp)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .heightIn(max = 40.dp)
                        .height(btnHeight)
                        .background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(100.dp))
                        .padding(horizontal = if (compact) 8.dp else 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onFullScreenClick
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFullScreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(btnIconSize)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(14.dp)
                                .background(Color.White.copy(alpha = 0.35f))
                        )

                        Box(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { showRemainingTime = !showRemainingTime }
                                )
                                .padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = timeLabel,
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showMoreOptionsBottomSheet) {
        PlayerMoreOptionsBottomSheet(
            onDismiss = onDismissMoreOptions,
            hasValidAudioTracks = hasValidAudioTracks,
            onSpeedClick = onSpeedClick,
            onAudioTracksClick = onAudioTracksClick,
            onAudioNormalizeClick = onAudioNormalizeClick,
            onPipClick = onPipClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerMoreOptionsBottomSheet(
    onDismiss: () -> Unit,
    hasValidAudioTracks: Boolean,
    onSpeedClick: () -> Unit,
    onAudioTracksClick: () -> Unit,
    onAudioNormalizeClick: () -> Unit,
    onPipClick: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.more_options_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PlayerMenuOption(
                    icon = Icons.Rounded.Speed,
                    label = stringResource(R.string.playback_speed),
                    onClick = { onSpeedClick(); onDismiss() }
                )

                if (hasValidAudioTracks) {
                    PlayerMenuOption(
                        icon = Icons.Rounded.Audiotrack,
                        label = stringResource(R.string.audio_quality_short),
                        onClick = { onAudioTracksClick(); onDismiss() }
                    )
                }

                PlayerMenuOption(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    label = stringResource(R.string.normalize_audio),
                    onClick = { onAudioNormalizeClick(); onDismiss() }
                )

                PlayerMenuOption(
                    icon = Icons.Rounded.PictureInPicture,
                    label = "PiP",
                    onClick = { onPipClick(); onDismiss() }
                )
            }
        }
    }
}

@Composable
private fun PlayerMenuOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
