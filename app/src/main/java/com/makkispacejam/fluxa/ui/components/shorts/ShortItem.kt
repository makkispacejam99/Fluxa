@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.ui.components.shorts

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.dialogs.ShortsMoreOptionsDialog
import com.makkispacejam.fluxa.ui.components.player.shorts.LikeHeartOverlay
import com.makkispacejam.fluxa.ui.components.shorts.dialogs.ShortsAudioDialog
import com.makkispacejam.fluxa.ui.components.shorts.dialogs.ShortsQualityDialog
import com.makkispacejam.fluxa.ui.components.shorts.overlays.PlayPauseFeedback
import com.makkispacejam.fluxa.ui.components.shorts.overlays.ShortsInfoBar
import com.makkispacejam.fluxa.ui.components.shorts.overlays.ShortsSlider
import com.makkispacejam.fluxa.ui.components.shorts.overlays.SpeedBanner
import com.makkispacejam.fluxa.video.shorts.ShortsComments
import com.makkispacejam.fluxa.video.shorts.VideoPlayer
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.viewmodels.content.CommentsViewModel
import com.makkispacejam.fluxa.viewmodels.user.InteractionViewModel
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.zIndex
import org.schabi.newpipe.extractor.stream.AudioStream

// Elementos de shorts
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
    videoViewModel: com.makkispacejam.fluxa.viewmodels.content.VideoViewModel = viewModel()
) {
    val commentsViewModel: CommentsViewModel = viewModel()
    val videoInteraction by interactionVM.getInteraction(videoId).collectAsState(initial = null)

    var isVideoPaused by remember { mutableStateOf(false) }
    var showTapFeedback by remember { mutableStateOf(false) }
    var isVideoLoading by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var isDisliked by remember { mutableStateOf(false) }
    var is2xSpeed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var hasRewardedProgress by remember(videoId) { mutableStateOf(false) }

    LaunchedEffect(videoInteraction) {
        isLiked = videoInteraction?.isLiked ?: false
        isDisliked = videoInteraction?.isDisliked ?: false
    }

    val context = LocalContext.current
    var showHeartAnimation by remember { mutableStateOf(false) }
    var showCommentsSheet by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showMoreOptions by remember { mutableStateOf(false) }
    var availableAudioTracks by remember { mutableStateOf<List<AudioStream>>(emptyList()) }
    var selectedAudioTrackDisplay by remember { mutableStateOf("") }

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

    val isVideoReady = isFocused && videoUrl.isNotEmpty()
    val progressPercent = remember(currentVideoPosition, totalVideoDuration) {
        if (totalVideoDuration > 0) currentVideoPosition.toFloat() / totalVideoDuration.toFloat() else 0f
    }

    LaunchedEffect(progressPercent) {
        if ((progressPercent > 0.8f) && !hasRewardedProgress) {
            hasRewardedProgress = true
        }
    }

    var hasCompletedVideo by remember { mutableStateOf(false) }
    LaunchedEffect(progressPercent) {
        if (progressPercent > 0.9f && !hasCompletedVideo && isFocused) {
            hasCompletedVideo = true
            onVideoCompleted()
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

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isVideoReady) {
            Box(
                modifier = Modifier.fillMaxWidth().fillMaxHeight().padding(bottom = bottomNavPadding),
                contentAlignment = Alignment.Center
            ) {
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
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 80.dp)
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
                            showTapFeedback = true
                        },
                        onLongPress = {},
                        onDoubleTap = {
                            if (!isLiked) {
                                interactionVM.toggleLike(videoId, title, channelName)
                                isLiked = true
                                isDisliked = false
                                showHeartAnimation = true
                            }
                        }
                    )
                }
        )

        AnimatedVisibility(visible = !isVideoReady, exit = fadeOut()) {
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }

        PlayPauseFeedback(isVisible = showTapFeedback, isPaused = isVideoPaused)

        AnimatedVisibility(
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
                .padding(top = 48.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.55f),
                        0.18f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.95f)
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = bottomNavPadding + 32.dp)
                .fillMaxWidth()
                .zIndex(1f),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShortsInfoBar(
                channelName = channelName,
                channelAvatarUrl = channelAvatarUrl,
                title = title,
                onChannelClick = onChannelClick,
                modifier = Modifier.weight(1f).padding(end = 16.dp),
                isLoadingAvatar = isLoadingAvatar
            )

            ShortsControlPanel(
                isLiked = isLiked,
                isDisliked = isDisliked,
                onShuffleClick = onShuffleClick,
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
                onMoreClick = { showMoreOptions = true }
            )
        }

        ShortsSlider(
            value = if (isUserDraggingSlider) (currentVideoPosition.toFloat() / totalVideoDuration.coerceAtLeast(1L).toFloat()) else progressPercent,
            bufferedValue = bufferedPercent,
            onValueChange = { newValue -> isUserDraggingSlider = true; currentVideoPosition = (newValue * totalVideoDuration).toLong() },
            onValueChangeFinished = {
                isUserDraggingSlider = false
                seekController?.invoke(currentVideoPosition.toFloat() / totalVideoDuration.coerceAtLeast(1L).toFloat())
            },
            bottomNavPadding = bottomNavPadding,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

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
                hasValidAudioTracks = hasValidAudioTracks
            )
        }
    }
}
