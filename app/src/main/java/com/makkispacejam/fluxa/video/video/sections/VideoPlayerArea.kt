package com.makkispacejam.fluxa.video.video.sections

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.video.video.HorizontalVideoPlayer
import com.makkispacejam.fluxa.video.video.fallbacks.AgeRestrictedPlaceholder
import com.makkispacejam.fluxa.video.video.fallbacks.VideoLoadErrorPlaceholder
import com.makkispacejam.fluxa.video.video.feedback.SeekFeedbackOverlay
import com.makkispacejam.fluxa.video.video.notifications.SpeedBanner
import com.makkispacejam.fluxa.video.video.notifications.SubtitleDisplay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.StreamSegment
import org.schabi.newpipe.extractor.stream.SubtitlesStream

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerArea(
    resolvedUrl: String,
    isFullScreen: Boolean,
    isPlaying: Boolean,
    controlsVisible: Boolean,
    currentPositionMs: Long,
    totalDurationMs: Long,
    isUserDraggingSlider: Boolean,
    videoLoadError: Boolean,
    ageRestricted: Boolean = false,
    ageRestrictedVideoId: String = "",
    availableSubtitles: List<SubtitlesStream>,
    availableAudioTracks: List<AudioStream>,
    onProgressUpdate: (Long, Long, Long) -> Unit,
    onSeekControllerReady: ((Float) -> Unit) -> Unit,
    onControlsToggle: () -> Unit,
    onPlayPause: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onBack: () -> Unit,
    onFullScreen: () -> Unit,
    onSubtitles: () -> Unit,
    onAudioTracks: () -> Unit,
    onSpeed: () -> Unit,
    onSettings: () -> Unit,
    showMoreOptions: Boolean = false,
    onMoreOptionsClick: () -> Unit = {},
    onDismissMoreOptions: () -> Unit = {},
    onResizeMode: () -> Unit,
    onPip: () -> Unit = {},
    onAudioNormalize: () -> Unit,
    hasTimeline: Boolean = false,
    onTimelineClick: () -> Unit = {},
    resizeMode: Int,
    onSliderChange: (Float) -> Unit,
    onSliderFinished: () -> Unit,
    onRetry: () -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    isLoading: Boolean = false,
    queueSize: Int = 0,
    subtitleText: String = "",
    videoTitle: String = "",
    channelName: String = "",
    bufferedPositionMs: Long = 0L,
    segments: List<StreamSegment> = emptyList(),
    hoverSegmentTitle: String? = null
) {
    val animatedScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(150),
        label = "swipeScale"
    )
    var totalDrag by remember { mutableFloatStateOf(0f) }
    var dragStartY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val isFullScreenRef = rememberUpdatedState(isFullScreen)
    val isDraggingRef = rememberUpdatedState(isUserDraggingSlider)
    val controlsVisibleRef = rememberUpdatedState(controlsVisible)

    var showRewindAnimation by remember { mutableStateOf(false) }
    var showForwardAnimation by remember { mutableStateOf(false) }
    var rewindIncrement by remember { mutableIntStateOf(0) }
    var forwardIncrement by remember { mutableIntStateOf(0) }
    var rewindKey by remember { mutableIntStateOf(0) }
    var forwardKey by remember { mutableIntStateOf(0) }
    var is2xSpeed by remember { mutableStateOf(false) }

    LaunchedEffect(rewindKey) {
        if (rewindKey > 0) {
            showRewindAnimation = true
            delay(800)
            showRewindAnimation = false
            rewindIncrement = 0
        }
    }
    LaunchedEffect(forwardKey) {
        if (forwardKey > 0) {
            showForwardAnimation = true
            delay(800)
            showForwardAnimation = false
            forwardIncrement = 0
        }
    }

    Box(
        modifier = Modifier
            .then(
                if (isFullScreen) Modifier.fillMaxSize()
                else Modifier
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .fillMaxWidth()
                    .aspectRatio(16 / 9f)
            )
            .background(Color.Black)
            .graphicsLayer { scaleX = animatedScale; scaleY = animatedScale }
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        val safeZonePx = 80.dp.toPx()
                        val isInSafeZone = dragStartY <= safeZonePx
                        if (totalDrag < -45f && !isFullScreenRef.value && !isInSafeZone) onFullScreen()
                        else if (totalDrag > 45f && isFullScreenRef.value && !isInSafeZone) onFullScreen()
                        totalDrag = 0f
                    },
                    onDragCancel = { totalDrag = 0f },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        if (!isDraggingRef.value) {
                            if (totalDrag == 0f) dragStartY = change.position.y
                            totalDrag += dragAmount
                        }
                    }
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        val job = scope.launch {
                            delay(500)
                            if (!isUserDraggingSlider && !controlsVisibleRef.value) {
                                is2xSpeed = true
                                onSpeedChange(2f)
                            }
                        }
                        try {
                            awaitRelease()
                        } finally {
                            job.cancel()
                            if (is2xSpeed) {
                                is2xSpeed = false
                                onSpeedChange(1f)
                            }
                        }
                    },
                    onTap = { onControlsToggle() },
                    onDoubleTap = { offset ->
                        val w = size.width
                        if (offset.x < w * 0.35f) {
                            rewindIncrement += 10
                            rewindKey++
                            onRewind()
                        } else if (offset.x > w * 0.65f) {
                            forwardIncrement += 10
                            forwardKey++
                            onForward()
                        } else {
                            onControlsToggle()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (videoLoadError) {
            if (ageRestricted) AgeRestrictedPlaceholder(onBack = onBack, videoId = ageRestrictedVideoId)
            else VideoLoadErrorPlaceholder(onBack = onBack, onRetry = onRetry)
        } else {
            if (resolvedUrl.isNotEmpty()) {
                HorizontalVideoPlayer(
                    resizeMode = resizeMode,
                    onProgressUpdate = onProgressUpdate,
                    onSeekControllerReady = onSeekControllerReady
                )
            }

            if (isLoading && !controlsVisible) {
                CircularProgressIndicator(
                    color = Color.White.copy(alpha = 0.7f),
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(48.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                SeekFeedbackOverlay(
                    visible = showRewindAnimation,
                    isForward = false,
                    amount = rewindIncrement,
                    isFullScreen = isFullScreen
                )
                SeekFeedbackOverlay(
                    visible = showForwardAnimation,
                    isForward = true,
                    amount = forwardIncrement,
                    isFullScreen = isFullScreen
                )
            }

            SpeedBanner(isVisible = is2xSpeed, isFullScreen = isFullScreen)
            SubtitleDisplay(subtitleText = subtitleText, controlsVisible = controlsVisible)

            PlayerControls(
                isVisible = controlsVisible,
                isPlaying = isPlaying,
                isFullScreen = isFullScreen,
                currentPositionMs = currentPositionMs,
                totalDurationMs = totalDurationMs,
                bufferedPositionMs = bufferedPositionMs,
                isUserDraggingSlider = isUserDraggingSlider,
                segments = segments,
                hoverSegmentTitle = hoverSegmentTitle,
                availableSubtitles = availableSubtitles,
                availableAudioTracks = availableAudioTracks,
                videoTitle = videoTitle,
                channelName = channelName,
                onSliderValueChange = onSliderChange,
                onSliderValueFinished = onSliderFinished,
                onPlayPause = onPlayPause,
                onRewindClick = onRewind,
                onForwardClick = onForward,
                onNextClick = onNext,
                onPreviousClick = onPrevious,
                onBackClick = onBack,
                onFullScreenClick = onFullScreen,
                onSubtitlesClick = onSubtitles,
                onAudioTracksClick = onAudioTracks,
                onSpeedClick = onSpeed,
                onSettingsClick = onSettings,
                showMoreOptionsBottomSheet = showMoreOptions,
                onMoreOptionsClick = onMoreOptionsClick,
                onDismissMoreOptions = onDismissMoreOptions,
                onResizeModeClick = onResizeMode,
                onPipClick = onPip,
                onAudioNormalizeClick = onAudioNormalize,
                queueSize = queueSize,
                isLoading = isLoading,
                hasTimeline = hasTimeline,
                onTimelineClick = onTimelineClick
            )
        }
    }
}


