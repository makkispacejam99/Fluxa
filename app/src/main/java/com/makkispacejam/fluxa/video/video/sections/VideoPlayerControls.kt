package com.makkispacejam.fluxa.video.video.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.core.TranslatedText
import com.makkispacejam.fluxa.utils.MusicChannelUtils
import com.makkispacejam.fluxa.video.video.controls.PlayerBottomBar
import com.makkispacejam.fluxa.video.video.controls.PlayerCenterButtons
import com.makkispacejam.fluxa.video.video.controls.PlayerTopButtons
import com.makkispacejam.fluxa.video.video.controls.stripEmojis
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamSegment
import org.schabi.newpipe.extractor.stream.SubtitlesStream

@Composable
fun PlayerControls(
    isVisible: Boolean,
    isPlaying: Boolean,
    isFullScreen: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    bufferedPositionMs: Long = 0L,
    isUserDraggingSlider: Boolean,
    availableSubtitles: List<SubtitlesStream>,
    availableAudioTracks: List<AudioStream>,
    videoTitle: String = "",
    channelName: String = "",
    onSliderValueChange: (Float) -> Unit,
    onSliderValueFinished: () -> Unit,
    onPlayPause: () -> Unit,
    onRewindClick: () -> Unit,
    onForwardClick: () -> Unit,
    onNextClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onBackClick: () -> Unit,
    onFullScreenClick: () -> Unit,
    onSubtitlesClick: () -> Unit,
    onAudioTracksClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onResizeModeClick: () -> Unit,
    onPipClick: () -> Unit = {},
    hasTimeline: Boolean = false,
    onTimelineClick: () -> Unit = {},
    onAudioNormalizeClick: () -> Unit,
    showMoreOptionsBottomSheet: Boolean = false,
    onMoreOptionsClick: () -> Unit = {},
    onDismissMoreOptions: () -> Unit = {},
    queueSize: Int = 0,
    isLoading: Boolean = false,
    segments: List<StreamSegment> = emptyList(),
    hoverSegmentTitle: String? = null
) {
    val progressPercent = remember(currentPositionMs, totalDurationMs) {
        if (totalDurationMs > 0) currentPositionMs.toFloat() / totalDurationMs.toFloat() else 0f
    }

    val bufferedPercent = remember(bufferedPositionMs, totalDurationMs) {
        if (totalDurationMs > 0) bufferedPositionMs.toFloat() / totalDurationMs.toFloat() else 0f
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.70f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isFullScreen) {
                            Modifier
                                .displayCutoutPadding()
                                .padding(horizontal = 32.dp, vertical = 20.dp)
                        } else {
                            Modifier
                                .padding(horizontal = 16.dp)
                                .padding(top = 12.dp, bottom = 18.dp)
                        }
                    )
            ) {
                PlayerTopButtons(
                    onBackClick = onBackClick,
                    onResizeModeClick = onResizeModeClick,
                    onPipClick = onPipClick,
                    availableSubtitles = availableSubtitles,
                    onSubtitlesClick = onSubtitlesClick,
                    onSettingsClick = onSettingsClick,
                    onMoreOptionsClick = onMoreOptionsClick
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    PlayerCenterButtons(
                        isPlaying = isPlaying,
                        isFullScreen = isFullScreen,
                        isLoading = isLoading,
                        onPlayPause = onPlayPause
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isFullScreen && videoTitle.isNotEmpty()) {
                        val cleanTitle = remember(videoTitle) { stripEmojis(videoTitle) }
                        val cleanChannel = remember(channelName) { stripEmojis(channelName) }

                        Column(
                            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TranslatedText(
                                    text = cleanTitle,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    skipTranslation = MusicChannelUtils.isMusicContent(cleanChannel, cleanTitle),
                                    modifier = Modifier.weight(1f)
                                )
                                if (hasTimeline) {
                                    Spacer(modifier = Modifier.width(10.dp))
                                    TimelineButton(onClick = onTimelineClick)
                                }
                            }
                            if (cleanChannel.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                TranslatedText(
                                    text = cleanChannel,
                                    color = Color.White.copy(alpha = 0.75f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    PlayerBottomBar(
                        currentPositionMs = currentPositionMs,
                        totalDurationMs = totalDurationMs,
                        progressPercent = progressPercent,
                        bufferedPercent = bufferedPercent,
                        isFullScreen = isFullScreen,
                        isUserDraggingSlider = isUserDraggingSlider,
                        segments = segments,
                        hoverSegmentTitle = hoverSegmentTitle,
                        onSliderValueChange = onSliderValueChange,
                        onSliderValueFinished = onSliderValueFinished,
                        onFullScreenClick = onFullScreenClick,
                        isPlaying = isPlaying,
                        isLoading = isLoading,
                        queueSize = queueSize,
                        onPlayPause = onPlayPause,
                        onRewindClick = onRewindClick,
                        onForwardClick = onForwardClick,
                        onNextClick = onNextClick,
                        onPreviousClick = onPreviousClick,
                        availableAudioTracks = availableAudioTracks,
                        onAudioTracksClick = onAudioTracksClick,
                        onSpeedClick = onSpeedClick,
                        onAudioNormalizeClick = onAudioNormalizeClick,
                        showMoreOptionsBottomSheet = showMoreOptionsBottomSheet,
                        onDismissMoreOptions = onDismissMoreOptions
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineButton(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = Color.Black.copy(alpha = 0.50f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Schedule,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.timeline),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}
