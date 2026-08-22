@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.ui.screens.main

import android.app.Activity
import android.app.Application
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.FluxaPlaybackService
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.ui.components.shorts.ShortItem
import com.makkispacejam.fluxa.ui.components.shorts.ShortSkeleton
import com.makkispacejam.fluxa.ui.components.system.ErrorScreen
import com.makkispacejam.fluxa.viewmodels.content.VideoViewModel
import com.makkispacejam.fluxa.data.shorts.ShortsCacheManager
import com.makkispacejam.fluxa.data.local.FluxaDatabase
import com.makkispacejam.fluxa.data.local.WatchedVideoEntity
import com.makkispacejam.fluxa.utils.NetworkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R
import kotlin.math.abs

// Pantalla de shorts
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    innerPadding: PaddingValues,
    onChannelClick: (String) -> Unit,
    isActiveTab: Boolean = true
) {
    val backgroundColor = MaterialTheme.colorScheme.surface
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val view = LocalView.current
    val prefs = remember { UserPreferences(context) }
    var shortsQuality by remember { mutableStateOf(prefs.shortsVideoQuality) }

    val videoViewModel: VideoViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            context.applicationContext as Application
        )
    )

    val shortsList = videoViewModel.videoList
    val pagerState = rememberPagerState(
        initialPage = videoViewModel.currentPageIndex,
        pageCount = { shortsList.size }
    )

    var isAppInForeground by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    val playbackStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(pagerState.currentPage) {
        videoViewModel.currentPageIndex = pagerState.currentPage
    }

    // Ciclo de vida de shorts
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isAppInForeground = true
                Lifecycle.Event.ON_PAUSE -> isAppInForeground = false
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        if (shortsList.isEmpty()) {
            videoViewModel.fetchYoutubeShorts()
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            ShortsCacheManager.markLeaveTime()
            (context as? Activity)?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Scroll Infinito
    LaunchedEffect(pagerState.currentPage, shortsList.size) {
        if (shortsList.isNotEmpty() &&
            pagerState.currentPage >= (shortsList.size - 5).coerceAtLeast(0) &&
            !videoViewModel.isLoading
        ) {
            if (NetworkUtils.isInternetAvailable(context)) {
                videoViewModel.loadMoreShorts()
            }
        }
    }

    // Gestion de chunks
    LaunchedEffect(videoViewModel.shouldUpdatePagerIndex) {
        if (videoViewModel.shouldUpdatePagerIndex) {
            pagerState.scrollToPage(videoViewModel.currentPageIndex)
            videoViewModel.shouldUpdatePagerIndex = false
        }
    }

    // Refresh y shuffle
    val onRefreshFeed: () -> Unit = {
        if (!isRefreshing) {
            scope.launch {
                isRefreshing = true
                ShortsCacheManager.clearCache()
                playbackStates.clear()
                
                videoViewModel.fetchYoutubeShorts(forceRefresh = true)
                
                while (videoViewModel.isLoading) { 
                    delay(50) 
                }
                
                videoViewModel.currentPageIndex = 0
                pagerState.scrollToPage(0)
                isRefreshing = false
            }
        }
    }

    val onDismissShort: (String) -> Unit = { videoId ->
        videoViewModel.hideVideo(videoId)
        scope.launch {
            try {
                if (!UserPreferences.incognitoActive) {
                    val dao = FluxaDatabase.getDatabase(context).fluxaDao()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        dao.insertWatchedVideo(WatchedVideoEntity(videoId))
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val onBlockChannelShort: (String) -> Unit = { channelId ->
        videoViewModel.removeChannelVideos(channelId)
        scope.launch {
            delay(200)
            onRefreshFeed()
        }
    }

    val currentFocusedVideoId = remember(pagerState.currentPage, shortsList) {
        if (shortsList.isNotEmpty() && pagerState.currentPage < shortsList.size) {
            shortsList[pagerState.currentPage].id
        } else null
    }

    val isCurrentVideoPlaying = currentFocusedVideoId?.let { playbackStates[it] } ?: false

    LaunchedEffect(isCurrentVideoPlaying, isAppInForeground) {
        view.keepScreenOn = isCurrentVideoPlaying && isAppInForeground
    }

    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefreshFeed,
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            val errorType = videoViewModel.errorTypeState

            if (errorType != null) {
                ErrorScreen(
                    errorType = errorType,
                    errorMessage = videoViewModel.errorMessageState,
                    onRetry = { videoViewModel.fetchYoutubeShorts(forceRefresh = true) }
                )
            } else if (shortsList.isEmpty() && videoViewModel.isLoading) {
                ShortSkeleton(bottomNavPadding = innerPadding.calculateBottomPadding())
            } else {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    key = { index -> if (index < shortsList.size) shortsList[index].id else "loading" },
                    beyondViewportPageCount = 1
                ) { page ->
                    if (page >= shortsList.size) return@VerticalPager

                    val video = shortsList[page]
                    val isFocused = pagerState.currentPage == page

                    var localStreamingUrl by remember(video.id) { mutableStateOf("") }

                    LaunchedEffect(isFocused, shortsQuality, page, pagerState.currentPage) {
                        val isNearby = abs(pagerState.currentPage - page) <= 1
                        if (isNearby) {
                            try {
                                val targetQ = NetworkUtils.getTargetVideoQuality(context, shortsQuality)
                                val url = videoViewModel.extractUrlForVideo(video.id, targetQ)
                                if (isFocused) localStreamingUrl = url ?: "ERROR"
                            } catch (_: Exception) {
                                if (isFocused) localStreamingUrl = "ERROR"
                            }
                        }
                    }

                    LaunchedEffect(Unit) {
                        videoViewModel.getAvatarUrl(video)
                    }

                    LaunchedEffect(isActiveTab) {
                        if (isActiveTab) {
                            FluxaPlaybackService.instance?.getPlayer()?.pause()
                        }
                    }

                    val channelIdForAvatar = video.channelId ?: ""
                    val isLoadingAvatar = channelIdForAvatar.isNotEmpty()
                            && videoViewModel.avatarCache[channelIdForAvatar] == null
                            && video.channelAvatarUrl.isNullOrEmpty()

                    val liveAvatarUrl = videoViewModel.avatarCache[channelIdForAvatar] ?: video.channelAvatarUrl

                    ShortItem(
                        title = video.title,
                        channelName = video.channelName,
                        channelId = video.channelId,
                        channelAvatarUrl = liveAvatarUrl,
                        imageUrl = video.imageUrl,
                        videoUrl = if (localStreamingUrl == "ERROR") "" else localStreamingUrl,
                        videoId = video.id,
                        bottomNavPadding = innerPadding.calculateBottomPadding(),
                        onChannelClick = onChannelClick,
                        isFocused = isFocused && isAppInForeground && localStreamingUrl.isNotEmpty() && localStreamingUrl != "ERROR" && isActiveTab,
                        isLoadingAvatar = isLoadingAvatar,
                        onShuffleClick = onRefreshFeed,
                        onQualityChanged = { shortsQuality = it },
                        onAudioTrackChanged = { newUrl -> localStreamingUrl = newUrl },
                        onDismissShort = onDismissShort,
                        onBlockChannel = onBlockChannelShort,
                        onPlaybackStateChanged = { playing ->
                            playbackStates[video.id] = playing
                        },
                        onVideoCompleted = {
                            ShortsCacheManager.markAsSeen(listOf(video))
                            com.makkispacejam.fluxa.data.filters.RecentVideosTracker.markAsSeen(listOf(video.id))
                            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                                try {
                                    if (!UserPreferences.incognitoActive) {
                                        val dao = FluxaDatabase.getDatabase(context).fluxaDao()
                                        dao.insertWatchedVideo(WatchedVideoEntity(video.id))
                                    }
                                } catch (_: Exception) {}
                            }
                        },
                        isIncognito = UserPreferences.incognitoActive
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = videoViewModel.showCooldownBanner,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = innerPadding.calculateTopPadding() + 20.dp, start = 24.dp, end = 24.dp)
                .statusBarsPadding()
        ) {
            Surface(
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.HourglassTop,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.shorts_cooldown_msg),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            LaunchedEffect(videoViewModel.showCooldownBanner) {
                if (videoViewModel.showCooldownBanner) {
                    delay(4000)
                    videoViewModel.showCooldownBanner = false
                }
            }
        }
    }
}