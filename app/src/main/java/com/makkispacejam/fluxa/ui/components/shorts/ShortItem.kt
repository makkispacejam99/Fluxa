@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.ui.components.shorts

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.height
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.audio.NormalizeAudio
import com.makkispacejam.fluxa.ui.components.dialogs.ShortsMoreOptionsDialog
import com.makkispacejam.fluxa.ui.components.player.shorts.LikeHeartOverlay
import com.makkispacejam.fluxa.ui.components.shorts.dialogs.ShortsAudioDialog
import com.makkispacejam.fluxa.ui.components.shorts.dialogs.ShortsQualityDialog
import com.makkispacejam.fluxa.ui.components.system.AudioNormalizerDialog
import com.makkispacejam.fluxa.ui.components.shorts.overlays.PlayPauseFeedback
import com.makkispacejam.fluxa.ui.components.shorts.overlays.ShortsInfoBar
import com.makkispacejam.fluxa.ui.components.shorts.overlays.ShortsSlider
import com.makkispacejam.fluxa.ui.components.shorts.overlays.SpeedBanner
import com.makkispacejam.fluxa.ui.components.system.NotificationBanner
import com.makkispacejam.fluxa.video.shorts.ShortsComments
import com.makkispacejam.fluxa.video.shorts.VideoPlayer
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.viewmodels.content.CommentsViewModel
import com.makkispacejam.fluxa.viewmodels.user.InteractionViewModel
import org.schabi.newpipe.extractor.stream.AudioStream

@SuppressLint("UnrememberedMutableState", "LocalContextGetResourceValueCall")
@Composable
fun ShortItem(
    modifier: Modifier = Modifier,
    title: String,
    channelName: String,
    channelId: String? = null,
    channelAvatarUrl: String?,
    bottomNavPadding: Dp,
    onChannelClick: (String) -> Unit,
    onPlaybackStateChanged: (Boolean) -> Unit,
    onVideoCompleted: () -> Unit = {},
    onShuffleClick: () -> Unit,
    onQualityChanged: (String) -> Unit = {},
    onAudioTrackChanged: (String) -> Unit = {},
    onDismissShort: (String) -> Unit = {},
    onBlockChannel: (String) -> Unit = {},
    imageUrl: String,
    videoUrl: String,
    videoId: String,
    isFocused: Boolean,
    isLoadingAvatar: Boolean = false,
    interactionVM: InteractionViewModel = viewModel(),
    videoViewModel: com.makkispacejam.fluxa.viewmodels.content.VideoViewModel = viewModel(),
    isIncognito: Boolean = false
) {
    val commentsViewModel: CommentsViewModel = viewModel()
    val videoInteraction by interactionVM.getInteraction(videoId).collectAsState(initial = null)
    val isSubscribed by interactionVM.isSubscribed(channelId ?: "").collectAsState(initial = false)

    var isVideoPaused by remember { mutableStateOf(false) }
    var showTapFeedback by remember { mutableStateOf(false) }
    var isVideoLoading by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var is2xSpeed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val bottomGradient = remember {
        Brush.verticalGradient(
            0.0f to Color.Transparent,
            0.42f to Color.Transparent,
            0.60f to Color.Black.copy(alpha = 0.20f),
            0.72f to Color.Black.copy(alpha = 0.50f),
            0.82f to Color.Black.copy(alpha = 0.70f),
            0.90f to Color.Black.copy(alpha = 0.80f),
            0.96f to Color.Black.copy(alpha = 0.83f),
            1.0f to Color.Black.copy(alpha = 0.78f)
        )
    }
    val reloadLabel = stringResource(R.string.reload_feed)

    LaunchedEffect(videoInteraction) {
        isLiked = videoInteraction?.isLiked ?: false
        isDisliked = videoInteraction?.isDisliked ?: false
    }

    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    var audioNormalized by remember { mutableStateOf(prefs.audioNormalizerEnabled) }
    var showHeartAnimation by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var showAudioNormalizerDialog by remember { mutableStateOf(false) }
    var availableAudioTracks by remember { mutableStateOf<List<AudioStream>>(emptyList()) }
    var selectedAudioTrackDisplay by remember { mutableStateOf("") }

    var showGuestBanner by remember { mutableStateOf(false) }
    var guestBannerText by remember { mutableStateOf("") }

    val hasValidAudioTracks = remember(availableAudioTracks) {
        availableAudioTracks.any { it.audioLocale != null || !it.audioTrackName.isNullOrBlank() }
    }

    LaunchedEffect(showTapFeedback) {
        if (showTapFeedback) {
            delay(1200)
            showTapFeedback = false
        }
    }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            interactionVM.incrementViewCount(videoId, title, channelName)
            val tracks = videoViewModel.getAudioTracksForVideo(videoId)
            availableAudioTracks = tracks
        }
    }

    var currentVideoPosition by remember { mutableLongStateOf(0L) }
    var totalVideoDuration by remember { mutableLongStateOf(0L) }
    var bufferedVideoPosition by remember { mutableLongStateOf(0L) }
    var isUserDraggingSlider by remember { mutableStateOf(false) }
    var seekController by remember { mutableStateOf<((Float) -> Unit)?>(null) }
    var controlsVisible by remember(videoId) { mutableStateOf(true) }
    var controlsZoneHeight by remember { mutableStateOf(240.dp) }
    var controlsTick by remember { mutableIntStateOf(0) }

    val isVideoReady = isFocused && videoUrl.isNotEmpty()

    val density = LocalDensity.current

    LaunchedEffect(isFocused) {
        if (isFocused) controlsVisible = true
    }

    LaunchedEffect(isVideoPaused, isVideoReady, controlsVisible, controlsTick) {
        if (isVideoPaused || !isVideoReady) {
            controlsVisible = true
        } else {
            delay(2800)
            if (!isVideoPaused) controlsVisible = false
        }
    }
    val progressPercent = remember(currentVideoPosition, totalVideoDuration) {
        if (totalVideoDuration > 0) currentVideoPosition.toFloat() / totalVideoDuration.toFloat() else 0f
    }

    var hasCompletedVideo by remember { mutableStateOf(false) }
    LaunchedEffect(isFocused, totalVideoDuration) {
        if (totalVideoDuration <= 0L) return@LaunchedEffect
        val target = totalVideoDuration * 900L / 1000L
        snapshotFlow { currentVideoPosition }
            .collect { pos ->
                if (pos >= target) {
                    if (!hasCompletedVideo && isFocused) {
                        hasCompletedVideo = true
                        onVideoCompleted()
                    }
                    return@collect
                }
            }
    }
    LaunchedEffect(isFocused) {
        if (!isFocused) hasCompletedVideo = false
    }

    val bufferedPercent = remember(bufferedVideoPosition, totalVideoDuration) {
        if (totalVideoDuration > 0) bufferedVideoPosition.toFloat() / totalVideoDuration.toFloat() else 0f
    }

    LaunchedEffect(isFocused, isVideoPaused, videoUrl) {
        onPlaybackStateChanged(isFocused && !isVideoPaused && videoUrl.isNotEmpty())
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp, bottom = bottomNavPadding + 10.dp)
        ) {
            ShortsInfoBar(
                channelName = channelName,
                channelAvatarUrl = channelAvatarUrl,
                title = title,
                onChannelClick = onChannelClick,
                isSubscribed = isSubscribed,
                onSubscribeClick = {
                    if (!channelId.isNullOrEmpty()) {
                        interactionVM.toggleSubscription(channelId, channelName, channelAvatarUrl, isSubscribed)
                    }
                },
                isIncognito = isIncognito,
                onDisabledClick = {
                    guestBannerText = context.getString(R.string.incognito_action_blocked)
                    showGuestBanner = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                isLoadingAvatar = isLoadingAvatar,
                textColor = MaterialTheme.colorScheme.onSurface
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (isVideoReady) {
                    VideoPlayer(
                        videoUrl = videoUrl,
                        thumbnailUrl = imageUrl,
                        isPaused = isVideoPaused,
                        playbackSpeed = if (is2xSpeed) 2f else 1f,
                        onProgressUpdate = { current, total, buffered ->
                            if (!isUserDraggingSlider) {
                                currentVideoPosition = current
                                totalVideoDuration = total
                                bufferedVideoPosition = buffered
                            }
                        },
                        onSeekControllerReady = { seekController = it },
                        onLoadingChanged = { isVideoLoading = it }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = {
                                    val job = coroutineScope.launch {
                                        delay(500)
                                        is2xSpeed = true
                                    }
                                    tryAwaitRelease()
                                    job.cancel()
                                    is2xSpeed = false
                                },
                                onTap = {
                                    isVideoPaused = !isVideoPaused
                                    controlsVisible = true
                                    showTapFeedback = true
                                },
                                onDoubleTap = {
                                    if (!isIncognito && !isLiked) {
                                        interactionVM.toggleLike(videoId, title, channelName)
                                        isLiked = true
                                        isDisliked = false
                                        showHeartAnimation = true
                                    }
                                }
                            )
                        }
                )

                androidx.compose.animation.AnimatedVisibility(visible = !isVideoReady, exit = fadeOut()) {
                    AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }

                PlayPauseFeedback(isVisible = showTapFeedback, isPaused = isVideoPaused)

                androidx.compose.animation.AnimatedVisibility(
                    visible = isVideoLoading && !isVideoPaused,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    CircularProgressIndicator(
                        color = Color.White.copy(alpha = 0.7f),
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(48.dp)
                    )
                }

                LikeHeartOverlay(isVisible = showHeartAnimation, onAnimationFinished = { showHeartAnimation = false })

                SpeedBanner(
                    isVisible = is2xSpeed,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(controlsZoneHeight)
                        .pointerInput(Unit) {
                            detectTapGestures {
                                controlsTick++
                                controlsVisible = if (isVideoPaused) true else !controlsVisible
                            }
                        }
                )

                androidx.compose.animation.AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(260)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                        .onSizeChanged { controlsZoneHeight = with(density) { it.height.toDp() } }
                        .background(bottomGradient)
                        .padding(start = 8.dp, end = 8.dp, top = 76.dp, bottom = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                    ShufflePill(
                        label = reloadLabel,
                        onClick = onShuffleClick,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTimecode(currentVideoPosition),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(42.dp)
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        ShortsControlPanel(
                            isLiked = isLiked,
                            isDisliked = isDisliked,
                            onCommentsClick = {
                                commentsViewModel.fetchVideoComments(videoId)
                                showCommentsSheet = true
                            },
                            onLikeClick = {
                                interactionVM.toggleLike(videoId, title, channelName)
                                if (!isLiked) {
                                    showHeartAnimation = true
                                }
                            },
                            onDislikeClick = {
                                interactionVM.toggleDislike(videoId, title, channelName)
                                onDismissShort(videoId)
                            },
                            onMoreClick = { showMoreOptions = true },
                            isIncognito = isIncognito,
                            onDisabledClick = {
                                guestBannerText = context.getString(R.string.incognito_action_blocked)
                                showGuestBanner = true
                            }
                        )

                        Spacer(modifier = Modifier.weight(1f))

                        Text(
                            text = formatTimecode(totalVideoDuration),
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(42.dp)
                        )
                    }

                    ShortsSlider(
                        value = if (isUserDraggingSlider) (currentVideoPosition.toFloat() / totalVideoDuration.coerceAtLeast(1L).toFloat()) else progressPercent,
                        bufferedValue = bufferedPercent,
                    onValueChange = { newValue ->
                        isUserDraggingSlider = true
                        controlsVisible = true
                        controlsTick++
                        currentVideoPosition = (newValue * totalVideoDuration).toLong()
                    },
                    onValueChangeFinished = {
                        isUserDraggingSlider = false
                        seekController?.invoke(currentVideoPosition.toFloat() / totalVideoDuration.coerceAtLeast(1L).toFloat())
                    },
                        bottomNavPadding = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                    }
                }
            }
        }

        if (showCommentsSheet) {
            ShortsComments(
                comments = commentsViewModel.commentList,
                isLoading = commentsViewModel.isCommentsLoading,
                getAvatar = { id, url -> commentsViewModel.getAvatar(id, url) },
                ownerChannelId = channelId ?: "",
                ownerAvatarUrl = channelAvatarUrl,
                onDismiss = { showCommentsSheet = false }
            )
        }

        if (showQualityDialog) {
            val prefs = remember { UserPreferences(context) }
            ShortsQualityDialog(
                userPreferences = prefs,
                onQualityChanged = onQualityChanged,
                onDismiss = { showQualityDialog = false }
            )
        }

        if (showAudioDialog) {
            ShortsAudioDialog(
                availableAudioTracks = availableAudioTracks,
                selectedAudioTrackDisplay = selectedAudioTrackDisplay,
                videoUrl = videoUrl,
                onAudioTrackChanged = onAudioTrackChanged,
                onDismiss = { showAudioDialog = false }
            )
        }

        if (showMoreOptions) {
            ShortsMoreOptionsDialog(
                onDismiss = { showMoreOptions = false },
                onQualityClick = { showQualityDialog = true },
                onAudioClick = { showAudioDialog = true },
                onShareClick = {
                    val shareLink = "https://youtu.be/$videoId"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Fluxa Video", shareLink))
                    Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
                },
                onMarkAsWatchedClick = {
                    interactionVM.markAsWatched(videoId)
                    onDismissShort(videoId)
                    Toast.makeText(context, context.getString(R.string.already_watched_msg), Toast.LENGTH_SHORT).show()
                },
                onBlockClick = {
                    interactionVM.blockChannel(channelId ?: "", channelName, channelAvatarUrl)
                    onBlockChannel(channelId ?: "")
                    Toast.makeText(context, context.getString(R.string.msg_channel_blocked), Toast.LENGTH_SHORT).show()
                },
                hasValidAudioTracks = hasValidAudioTracks,
                onAudioNormalizeClick = { showAudioNormalizerDialog = true },
                isIncognito = isIncognito
            )
        }

        if (showAudioNormalizerDialog) {
            AudioNormalizerDialog(
                isEnabled = audioNormalized,
                onToggle = { on ->
                    audioNormalized = on
                    prefs.audioNormalizerEnabled = on
                    NormalizeAudio.setEnabled(on)
                },
                onDismissRequest = { showAudioNormalizerDialog = false }
            )
        }

        NotificationBanner(
            visible = showGuestBanner,
            text = guestBannerText,
            onDismiss = { showGuestBanner = false },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

private fun formatTimecode(millis: Long): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
