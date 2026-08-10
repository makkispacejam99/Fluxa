package com.makkispacejam.fluxa.video.video.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.ui.components.core.TranslatedText
import com.makkispacejam.fluxa.utils.MusicChannelUtils
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.makkispacejam.fluxa.video.video.controls.PlayerBottomBar
import com.makkispacejam.fluxa.video.video.controls.PlayerCenterButtons
import com.makkispacejam.fluxa.video.video.controls.PlayerTopButtons
import com.makkispacejam.fluxa.video.video.controls.stripEmojis
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.SubtitlesStream

// Controles del reproductor horizontal
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
    queueSize: Int = 0,
    isLoading: Boolean = false
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
                .background(Color.Black.copy(alpha = 0.4f))
        ) {
            if (isFullScreen && videoTitle.isNotEmpty()) {
                val cleanTitle = remember(videoTitle) { stripEmojis(videoTitle) }
                val cleanChannel = remember(channelName) { stripEmojis(channelName) }

                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 8.dp, start = 60.dp)
                        .background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .widthIn(max = 280.dp)
                ) {
                    TranslatedText(
                        text = cleanTitle,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        skipTranslation = MusicChannelUtils.isMusicContent(cleanChannel, cleanTitle)
                    )
                    if (cleanChannel.isNotEmpty()) {
                        TranslatedText(
                            text = cleanChannel,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            PlayerTopButtons(
                onBackClick = onBackClick,
                onResizeModeClick = onResizeModeClick,
                onPipClick = onPipClick,
                availableSubtitles = availableSubtitles,
                availableAudioTracks = availableAudioTracks,
                onSubtitlesClick = onSubtitlesClick,
                onAudioTracksClick = onAudioTracksClick,
                onSpeedClick = onSpeedClick,
                onSettingsClick = onSettingsClick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp, start = 8.dp, end = 8.dp)
            )

            PlayerCenterButtons(
                isPlaying = isPlaying,
                isFullScreen = isFullScreen,
                isLoading = isLoading,
                queueSize = queueSize,
                onPlayPause = onPlayPause,
                onRewindClick = onRewindClick,
                onForwardClick = onForwardClick,
                onNextClick = onNextClick,
                onPreviousClick = onPreviousClick,
                modifier = Modifier.align(Alignment.Center)
            )

            PlayerBottomBar(
                currentPositionMs = currentPositionMs,
                totalDurationMs = totalDurationMs,
                progressPercent = progressPercent,
                bufferedPercent = bufferedPercent,
                isFullScreen = isFullScreen,
                isUserDraggingSlider = isUserDraggingSlider,
                onSliderValueChange = onSliderValueChange,
                onSliderValueFinished = onSliderValueFinished,
                onFullScreenClick = onFullScreenClick,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp)
                    .padding(bottom = if (isFullScreen) 16.dp else 10.dp)
            )
        }
    }
}
