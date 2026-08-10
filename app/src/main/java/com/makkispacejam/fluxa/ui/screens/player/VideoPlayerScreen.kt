@file:Suppress("DEPRECATION", "AssignedValueIsNeverRead")

package com.makkispacejam.fluxa.ui.screens.player

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.util.Log
import android.util.Rational
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makkispacejam.fluxa.viewmodels.user.InteractionViewModel
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.viewmodels.content.CommentsViewModel
import com.makkispacejam.fluxa.viewmodels.content.VideoViewModel
import com.makkispacejam.fluxa.viewmodels.player.PlayerViewModel
import com.makkispacejam.fluxa.MainActivity
import com.makkispacejam.fluxa.ui.components.player.notifications.PlayerNotificationBanner
import com.makkispacejam.fluxa.ui.components.player.queue.QueueFloatingButton
import com.makkispacejam.fluxa.video.video.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.video.video.sections.VideoPlayerArea
import com.makkispacejam.fluxa.video.video.sections.videoContentSection

// Reproductor de video horizontal
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerScreen(
    videoId: String,
    videoTitle: String,
    channelName: String,
    onBackClick: () -> Unit,
    onChannelClick: (String) -> Unit = {},
    commentsViewModel: CommentsViewModel = viewModel(),
    videoVM: VideoViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    interactionVM: InteractionViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as MainActivity
    val colorScheme = MaterialTheme.colorScheme
    val view = LocalView.current
    val playbackState = playerViewModel.playbackState
    val prefs = remember { com.makkispacejam.fluxa.data.UserPreferences(context) }
    val currentVideoId = playbackState.currentVideoId.ifEmpty { videoId }
    val currentTitle = playbackState.title.ifEmpty { videoTitle }
    val currentChannel = playbackState.channel.ifEmpty { channelName }
    val videoInteraction by interactionVM.getInteraction(currentVideoId).collectAsState(initial = null)
    val isSubscribed by interactionVM.isSubscribed(videoVM.channelIdState).collectAsState(initial = false)

    // Estados Like y Dislike
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    
    LaunchedEffect(videoInteraction) {
        isLiked = videoInteraction?.isLiked ?: false
        isDisliked = videoInteraction?.isDisliked ?: false
    }

    var isExpanded by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var isFullScreen by remember { mutableStateOf(false) }
    var videoLoadError by remember { mutableStateOf(false) }

    // Estados de Diálogos
    var showFullComments by remember { mutableStateOf(false) }
    var showQueuePanel by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSubtitlesDialog by remember { mutableStateOf(false) }
    var showAudioTracksDialog by remember { mutableStateOf(false) }
    var selectedQuality by remember { mutableStateOf(prefs.videoQuality) }
    
    val defaultSpeed = stringResource(R.string.speed_normal)
    val defaultSubtitles = stringResource(R.string.subtitles_off)
    val defaultAudio = stringResource(R.string.audio_default)

    var selectedSpeed by remember { mutableStateOf(defaultSpeed) }
    var selectedSubtitleDisplay by remember { mutableStateOf(defaultSubtitles) }
    var selectedAudioTrackDisplay by remember { mutableStateOf(defaultAudio) }
    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var draggingProgressMs by remember { mutableLongStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()

    val targetHeight = remember(selectedQuality) {
        if (selectedQuality == "Seleccionar calidad por defecto") {
            com.makkispacejam.fluxa.utils.NetworkUtils.getTargetVideoQuality(context)
        } else {
            selectedQuality.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 720
        }
    }

    val handleBack = { if (isFullScreen) isFullScreen = false else onBackClick() }
    val resolvedUrl = playbackState.videoUrl ?: ""

    LaunchedEffect(playbackState.videoUrl, playbackState.availableAudioTracks) {
        val audioUrl = playbackState.videoUrl?.substringAfter("|", "") ?: ""
        if (audioUrl.isNotEmpty()) {
            val track = playbackState.availableAudioTracks.find { it.url == audioUrl }
            if (track != null) {
                val lang = track.audioLocale?.let { 
                    it.getDisplayName(java.util.Locale.getDefault())
                        .replaceFirstChar { char -> char.uppercase() }
                } ?: track.audioTrackName ?: "Audio"
                selectedAudioTrackDisplay = lang
            }
        }
    }

    LaunchedEffect(currentVideoId) {
        Log.d("FluxaPlayer", "videoId cambiado a: '$currentVideoId'")
        if (currentVideoId.isNotEmpty()) {
            videoLoadError = false
            isExpanded = false
            selectedSubtitleDisplay = defaultSubtitles
            videoVM.loadDetailedMetadata(currentVideoId)
            commentsViewModel.fetchVideoComments(currentVideoId)
        }
    }

    LaunchedEffect(controlsVisible, playbackState.isPlaying, isUserDraggingSlider) {
        if (controlsVisible && playbackState.isPlaying && !isUserDraggingSlider) {
            delay(2500)
            controlsVisible = false
        }
    }

    LaunchedEffect(playbackState.isPlaying) {
        view.keepScreenOn = playbackState.isPlaying
    }

    LaunchedEffect(activity.isInPipMode) {
        if (activity.isInPipMode) {
            showFullComments = false
            showQueuePanel = false
            showQualityDialog = false
            showSpeedDialog = false
            showSubtitlesDialog = false
            showAudioTracksDialog = false
            controlsVisible = false
        } else {
            controlsVisible = true
        }
    }

    LaunchedEffect(isFullScreen) {
        val window = activity.window
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        if (isFullScreen) {
            controlsVisible = true
            controller.hide(WindowInsetsCompat.Type.systemBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            controlsVisible = true
            controller.show(WindowInsetsCompat.Type.systemBars())
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
            }
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            delay(50)
            val isLight = colorScheme.background.luminance() > 0.5f
            controller.isAppearanceLightStatusBars = isLight
            controller.isAppearanceLightNavigationBars = isLight
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val window = activity.window
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            view.keepScreenOn = false
        }
    }

    BackHandler(enabled = playbackState.isFullyExpanded) { handleBack() }

    val effectiveFullScreen = isFullScreen || activity.isInPipMode
    val effectiveControlsVisible = controlsVisible && !activity.isInPipMode

    // Interfaz del reproductor
    Surface(modifier = Modifier.fillMaxSize().clipToBounds(), color = Color.Black) {
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            Column(
                modifier = Modifier.fillMaxSize()
                    .background(if (effectiveFullScreen) Color.Black else MaterialTheme.colorScheme.background)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .then(if (!effectiveFullScreen && !activity.isInPipMode) Modifier.statusBarsPadding() else Modifier)
                ) {
                    VideoPlayerArea(
                        resolvedUrl = resolvedUrl,
                        isFullScreen = effectiveFullScreen,
                        isPlaying = playbackState.isPlaying,
                        controlsVisible = effectiveControlsVisible,
                        currentPositionMs = if (isUserDraggingSlider) draggingProgressMs else playbackState.progressMs,
                        totalDurationMs = playbackState.durationMs,
                        isUserDraggingSlider = isUserDraggingSlider,
                        videoLoadError = videoLoadError || playbackState.error != null || playbackState.ageRestricted,
                        ageRestricted = playbackState.ageRestricted,
                        ageRestrictedVideoId = currentVideoId,
                        availableSubtitles = playbackState.availableSubtitles,
                        availableAudioTracks = playbackState.availableAudioTracks,
                        onProgressUpdate = { pos, dur, buf ->
                            if (!isUserDraggingSlider) {
                                playerViewModel.updateProgress(pos, dur, buf)
                            }
                        },
                        onSeekControllerReady = { },
                        onControlsToggle = { controlsVisible = !controlsVisible },
                        onPlayPause = {
                            playerViewModel.togglePlayback()
                            controlsVisible = true
                        },
                        onRewind = { playerViewModel.seekOffset(-10000L) },
                        onForward = { playerViewModel.seekOffset(10000L) },
                        onNext = { playerViewModel.skipToNext() },
                        onPrevious = { playerViewModel.skipToPrevious() },
                        onBack = { handleBack() },
                        onFullScreen = { isFullScreen = !isFullScreen },
                        onSubtitles = { showSubtitlesDialog = true },
                        onAudioTracks = { showAudioTracksDialog = true },
                        onSpeed = { showSpeedDialog = true },
                        onSettings = { showQualityDialog = true },
                        onResizeMode = { playerViewModel.toggleResizeMode() },
                        onRetry = { playerViewModel.retryLoad() },
                        onSpeedChange = { speed ->
                            playerViewModel.setPlaybackSpeed(if (speed >= 2f) "2.0x" else "Normal")
                        },
                        onPip = {
                            val aspectRatio = Rational(16, 9)
                            activity.enterPictureInPictureMode(
                                PictureInPictureParams.Builder()
                                    .setAspectRatio(aspectRatio)
                                    .build()
                            )
                        },
                        resizeMode = playerViewModel.resizeMode,
                        onSliderChange = { newValue ->
                            isUserDraggingSlider = true
                            draggingProgressMs = (newValue * playbackState.durationMs).toLong()
                        },
                        onSliderFinished = {
                            val targetFraction =
                                draggingProgressMs.toFloat() / playbackState.durationMs.coerceAtLeast(
                                    1L
                                ).toFloat()
                            playerViewModel.seekTo(targetFraction)
                            coroutineScope.launch {
                                delay(1000)
                                isUserDraggingSlider = false
                            }
                        },
                        isLoading = playbackState.isLoading,
                        queueSize = playbackState.playlistQueue.size,
                        subtitleText = playbackState.currentSubtitleText,
                        videoTitle = currentTitle,
                        channelName = currentChannel,
                        bufferedPositionMs = playbackState.bufferedMs
                    )
                }

                if (!effectiveFullScreen) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
                    ) {
                        videoContentSection(
                            videoTitle = currentTitle,
                            videoVM = videoVM,
                            channelName = currentChannel,
                            isLiked = isLiked,
                            isDisliked = isDisliked,
                            isSubscribed = isSubscribed,
                            isExpanded = isExpanded,
                            commentsViewModel = commentsViewModel,
                            onLike = { interactionVM.toggleLike(currentVideoId, currentTitle, currentChannel) },
                            onDislike = { interactionVM.toggleDislike(currentVideoId, currentTitle, currentChannel) },
                            onSubscribe = {
                                if (videoVM.channelIdState.isNotEmpty()) {
                                    interactionVM.toggleSubscription(
                                        videoVM.channelIdState,
                                        currentChannel,
                                        videoVM.channelAvatarState,
                                        isSubscribed
                                    )
                                }
                            },
                            onExpand = { isExpanded = !isExpanded },
                            onShowComments = { showFullComments = true },
                            currentVideoId = currentVideoId,
                            onVideoClick = { title, id, channel, thumb ->
                                coroutineScope.launch {
                                    playerViewModel.loadAndPlayVideo(id, title, channel, thumb)
                                }
                            },
                            onChannelClick = onChannelClick,
                            hideRelatedVideos = playerViewModel.playbackState.resetProgress
                        )
                    }
                }
            }

            // Botón flotante de cola de reproducción
            if (!effectiveFullScreen && playbackState.isActive) {
                QueueFloatingButton(
                    onClick = { showQueuePanel = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 16.dp, end = 16.dp)
                )
            }

            // Banner flotante para notificaciones
            PlayerNotificationBanner(
                isVisible = videoVM.showNotificationBanner && !activity.isInPipMode,
                text = videoVM.notificationBannerText,
                onAutoDismiss = { videoVM.showNotificationBanner = false },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp, start = 24.dp, end = 24.dp)
            )
        }
    }

    VideoPlayerDialogs(
        commentsViewModel = commentsViewModel,
        playerViewModel = playerViewModel,
        ownerChannelId = videoVM.channelIdState,
        ownerAvatarUrl = videoVM.channelAvatarState,
        showFullComments = showFullComments,
        showQueuePanel = showQueuePanel,
        showQualityDialog = showQualityDialog,
        showSpeedDialog = showSpeedDialog,
        showSubtitlesDialog = showSubtitlesDialog,
        showAudioTracksDialog = showAudioTracksDialog,
        targetHeight = targetHeight,
        availableResolutions = playbackState.availableResolutions,
        availableSubtitles = playbackState.availableSubtitles,
        availableAudioTracks = playbackState.availableAudioTracks,
        selectedSpeed = selectedSpeed,
        selectedSubtitle = selectedSubtitleDisplay,
        selectedAudioTrack = selectedAudioTrackDisplay,
        onQualitySelected = { 
            if (selectedQuality != "${it}p") {
                selectedQuality = "${it}p"
                playerViewModel.changeQuality(it)
            }
        },
        onSpeedSelected = { 
            selectedSpeed = it
            playerViewModel.setPlaybackSpeed(it)
        },
        onSubtitleSelected = { tag ->
            val displayName = if (tag.isEmpty()) defaultSubtitles
            else java.util.Locale.forLanguageTag(tag).getDisplayName(java.util.Locale.getDefault()).replaceFirstChar { it.uppercase() }
            selectedSubtitleDisplay = displayName
            playerViewModel.setSubtitleTrack(tag)
        },
        onAudioTrackSelected = { url ->
            val track = playbackState.availableAudioTracks.find { it.url == url }
            val lang = track?.audioLocale?.let {
                it.getDisplayName(java.util.Locale.getDefault())
                    .replaceFirstChar { char -> char.uppercase() }
            } ?: track?.audioTrackName ?: "Audio"
            selectedAudioTrackDisplay = lang
            playerViewModel.setAudioTrack(url)
        },
        onDismissComments = { showFullComments = false },
        onDismissQueue = { showQueuePanel = false },
        onDismissQuality = { showQualityDialog = false },
        onDismissSpeed = { showSpeedDialog = false },
        onDismissSubtitles = { showSubtitlesDialog = false },
        onDismissAudioTracks = { showAudioTracksDialog = false }
    )
}
