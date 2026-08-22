package com.makkispacejam.fluxa.ui.screens.main

import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.models.home.HomeFeedItem
import com.makkispacejam.fluxa.ui.animations.FluxaAnimations
import com.makkispacejam.fluxa.ui.components.core.*
import com.makkispacejam.fluxa.ui.components.home.HomeActionMenus
import com.makkispacejam.fluxa.ui.components.home.HomeFeedList
import com.makkispacejam.fluxa.ui.components.home.topbar.recents.RecentsOverlay
import com.makkispacejam.fluxa.ui.components.home.homeMessages
import com.makkispacejam.fluxa.ui.components.home.topbar.HomeTopBar
import com.makkispacejam.fluxa.ui.components.home.RecentSearchHistory
import com.makkispacejam.fluxa.ui.components.home.topbar.search.SearchOverlay
import com.makkispacejam.fluxa.ui.components.home.SearchResultsList
import com.makkispacejam.fluxa.ui.components.system.ErrorScreen
import com.makkispacejam.fluxa.ui.components.system.NotificationBanner
import com.makkispacejam.fluxa.viewmodels.content.HomeViewModel
import com.makkispacejam.fluxa.viewmodels.user.InteractionViewModel
import com.makkispacejam.fluxa.viewmodels.player.PlayerViewModel
import com.makkispacejam.fluxa.viewmodels.content.VideoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Pantalla de inicio
@androidx.annotation.OptIn(UnstableApi::class)
@SuppressLint("LocalContextGetResourceValueCall")
@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    mainPadding: PaddingValues,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isShowingResults: Boolean,
    onResultsStateChange: (Boolean) -> Unit,
    onChannelClick: (String) -> Unit,
    onVideoClick: (String, String, String, String) -> Unit,
    onPlaylistClick: (String, String) -> Unit,
    onCheckConnection: () -> Unit,
    onProfileClick: () -> Unit = {},
    isIncognito: Boolean = false,
    homeViewModel: HomeViewModel = viewModel(),
    interactionVM: InteractionViewModel = viewModel(),
    videoVM: VideoViewModel = viewModel(),
    playerVM: PlayerViewModel
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    
    val prefs = remember(context) { context.getSharedPreferences("fluxa_prefs", Context.MODE_PRIVATE) }
    val userAvatarPath = remember { mutableStateOf(prefs.getString("user_avatar_path", null)) }
    val userName = remember { mutableStateOf(prefs.getString("user_name", "Fluxa") ?: "Fluxa") }

    var selectedFilter by remember { mutableStateOf("Todo") }
    var isRefreshing by remember { mutableStateOf(false) }
    var isOverlaySearching by remember { mutableStateOf(false) }
    var isOverlayNotifications by remember { mutableStateOf(false) }

    var selectedVideoForOptions by remember { mutableStateOf<HomeFeedItem?>(null) }
    var videoForPlaylist by remember { mutableStateOf<HomeFeedItem?>(null) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    val userPlaylists by videoVM.getUserPlaylistsFlow().collectAsState(initial = emptyList())
    
    val messages = homeMessages()
    val allInteractions by interactionVM.allInteractions.collectAsState(initial = emptyList())
    val watchedVideoIds by interactionVM.watchedVideos.collectAsState(initial = emptyList())
    val interactionMap = remember(allInteractions) { allInteractions.associateBy { it.videoId } }
    val watchedSet = remember(watchedVideoIds) { watchedVideoIds.toSet() }

    val closeKeyboard = { focusManager.clearFocus(); keyboardController?.hide() }

    LaunchedEffect(Unit) {
        onCheckConnection()
        if (homeViewModel.homeFeed.isEmpty()) homeViewModel.loadHomeFeed()
    }

    LaunchedEffect(isShowingResults, selectedFilter) {
        if (isShowingResults && searchQuery.isNotBlank()) homeViewModel.search(searchQuery, selectedFilter)
        if (!isShowingResults) homeViewModel.clearSearch()
    }

    LaunchedEffect(isOverlaySearching) {
        if (isOverlaySearching) { delay(100); focusRequester.requestFocus() }
    }

    Column(modifier = Modifier.fillMaxSize().padding(mainPadding)) {
        if (homeViewModel.errorType == null && !isOverlaySearching && !isOverlayNotifications) {
            HomeTopBar(
                isShowingResults = isShowingResults,
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onSearchClick = { isOverlaySearching = true },
                onNotificationsClick = { isOverlayNotifications = true; homeViewModel.loadNotifications() },
                onSearch = { homeViewModel.search(searchQuery, selectedFilter) },
                onCloseResults = { onResultsStateChange(false); onSearchQueryChange("") },
                onProfileClick = onProfileClick,
                userAvatarPath = userAvatarPath.value,
                userName = userName.value,
                isIncognito = isIncognito
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                homeViewModel.errorType != null -> ErrorScreen(
                    errorType = homeViewModel.errorType!!,
                    errorMessage = homeViewModel.errorMessage,
                    onRetry = { homeViewModel.refreshFeed() }
                )
                homeViewModel.isLoadingFeed && homeViewModel.homeFeed.isEmpty() -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) { items(3) { VideoCardSkeleton() } }
                }
                else -> {
                    Crossfade(
                        targetState = isShowingResults,
                        animationSpec = FluxaAnimations.premiumFadeSpec(),
                        label = "SearchTransition",
                        modifier = Modifier.fillMaxSize()
                    ) { resultsActive ->
                        if (resultsActive) {
                            SearchResultsList(
                                isLoading = homeViewModel.isLoadingSearch,
                                results = homeViewModel.searchResults,
                                selectedFilter = selectedFilter,
                                onFilterSelected = { selectedFilter = it },
                                interactionMap = interactionMap,
                                watchedSet = watchedSet,
                                getAvatar = { id, url -> homeViewModel.getAvatar(id, url ?: "") },
                                onChannelClick = { closeKeyboard(); onChannelClick(it) },
                                onVideoClick = { t, id, c, th ->
                                    closeKeyboard(); onVideoClick(
                                    t,
                                    id,
                                    c,
                                    th
                                )
                                },
                                onPlaylistClick = onPlaylistClick,
                                onOptionsClick = { selectedVideoForOptions = it },
                                onLoadMore = { homeViewModel.loadMoreSearch() },
                                isLoadingMore = homeViewModel.isLoadingMoreSearch,
                                hasMore = homeViewModel.hasMoreSearch
                            )
                        } else {
                            val miniPlayerVisible = playerVM.playbackState.isActive && !playerVM.playbackState.isFullyExpanded
                            val visibleFeed = remember(homeViewModel.homeFeed, watchedSet) {
                                homeViewModel.homeFeed.filterNot { it.videoId in watchedSet }
                            }
                            HomeFeedList(
                                homeFeed = visibleFeed,
                                isRefreshing = isRefreshing,
                                onRefresh = {
                                    scope.launch {
                                        isRefreshing = true
                                        homeViewModel.refreshFeed()
                                        isRefreshing = false
                                    }
                                },
                                interactionMap = interactionMap,
                                watchedSet = watchedSet,
                                getAvatar = { id, url -> homeViewModel.getAvatar(id, url ?: "") },
                                onChannelClick = onChannelClick,
                                onVideoClick = { t, id, c, th ->
                                    closeKeyboard(); onVideoClick(
                                    t,
                                    id,
                                    c,
                                    th
                                )
                                },
                                onOptionsClick = { selectedVideoForOptions = it },
                                showMiniPlayer = miniPlayerVisible
                            )
                        }
                    }

                    if (searchQuery.isNotEmpty() && !isShowingResults && !isOverlaySearching) {
                        RecentSearchHistory(
                            recentSearches = homeViewModel.recentSearches,
                            onSearchClick = { onSearchQueryChange(it); onResultsStateChange(true); closeKeyboard() },
                            onRemoveSearch = { homeViewModel.removeRecentSearch(it) },
                            onClearHistory = { homeViewModel.clearRecentSearches() }
                        )
                    }
                }
            }

            NotificationBanner(
                visible = videoVM.showNotificationBanner,
                text = videoVM.notificationBannerText,
                onDismiss = { videoVM.showNotificationBanner = false },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }

    BackHandler(enabled = isOverlaySearching) { isOverlaySearching = false }
    BackHandler(enabled = isOverlayNotifications) { isOverlayNotifications = false }

    LaunchedEffect(playerVM.playbackState.currentVideoId, playerVM.playbackState.isPlaying) {
        val notifId = homeViewModel.lastPlayedNotificationId ?: return@LaunchedEffect
        val currentId = playerVM.playbackState.currentVideoId
        if (currentId == notifId && !playerVM.playbackState.isPlaying && playerVM.playbackState.progressMs > 0) {
            homeViewModel.dismissNotification(notifId)
            homeViewModel.clearNotificationPlaying()
        }
    }

    SearchOverlay(
        visible = isOverlaySearching,
        searchQuery = searchQuery,
        onSearchQueryChange = onSearchQueryChange,
        onDismiss = { isOverlaySearching = false; onSearchQueryChange("") },
        onSearch = { onResultsStateChange(true); isOverlaySearching = false; closeKeyboard() },
        recentSearches = homeViewModel.recentSearches,
        onRemoveRecentSearch = { homeViewModel.removeRecentSearch(it) },
        onClearRecentSearches = { homeViewModel.clearRecentSearches() },
        focusRequester = focusRequester
    )

    RecentsOverlay(
        visible = isOverlayNotifications,
        notifications = homeViewModel.notifications,
        isLoading = homeViewModel.isLoadingNotifications,
        isRefreshing = homeViewModel.isRefreshingNotifications,
        hasSubscriptions = homeViewModel.hasSubscriptions,
        onDismiss = { isOverlayNotifications = false },
        onRefresh = { homeViewModel.refreshNotifications() },
        onVideoClick = { title, channelName, videoId, thumbnailUrl ->
            isOverlayNotifications = false
            homeViewModel.markNotificationPlaying(videoId)
            onVideoClick(title, videoId, channelName, thumbnailUrl)
        },
        onChannelClick = { channelName -> isOverlayNotifications = false; onChannelClick(channelName) }
    )

    HomeActionMenus(
        selectedVideoForOptions = selectedVideoForOptions,
        videoForPlaylist = videoForPlaylist,
        onDismissOptions = { selectedVideoForOptions = null },
        showPlaylistDialog = showPlaylistDialog,
        onDismissPlaylist = { showPlaylistDialog = false; videoForPlaylist = null },
        userPlaylists = userPlaylists,
        videoVM = videoVM,
        interactionVM = interactionVM,
        homeViewModel = homeViewModel,
        context = context,
        messages = messages,
        onAddToPlaylistTrigger = { videoForPlaylist = it; showPlaylistDialog = true }
    )
}
