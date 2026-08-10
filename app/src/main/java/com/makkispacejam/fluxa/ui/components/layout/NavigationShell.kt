package com.makkispacejam.fluxa.ui.components.layout

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.ThemeMode
import com.makkispacejam.fluxa.ui.animations.FluxaAnimations
import com.makkispacejam.fluxa.viewmodels.content.HomeViewModel
import com.makkispacejam.fluxa.models.Screen
import com.makkispacejam.fluxa.viewmodels.content.VideoViewModel
import com.makkispacejam.fluxa.viewmodels.player.PlayerViewModel
import com.makkispacejam.fluxa.models.DetailView
import com.makkispacejam.fluxa.ui.components.player.miniplayer.MiniPlayer
import com.makkispacejam.fluxa.ui.screens.main.FluxaCollections
import com.makkispacejam.fluxa.ui.screens.main.HomeScreen
import com.makkispacejam.fluxa.ui.screens.main.SettingsScreen
import com.makkispacejam.fluxa.ui.screens.main.ShortsScreen

/* Lógica y comportamiento de
la barra de navegación inferior */

@OptIn(UnstableApi::class)
@Composable
fun MainNavigationShell(
    selectedTab: Screen,
    tabHistory: MutableList<Screen>,
    onTabSelected: (Screen) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isShowingResults: Boolean,
    onResultsStateChange: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    amoledMode: Boolean,
    onAmoledModeChange: (Boolean) -> Unit,
    snackbarHostState: SnackbarHostState,
    videoViewModel: VideoViewModel,
    onExpandPlayer: () -> Unit,
    onVideoClick: (String, String, String, String) -> Unit,
    onChannelClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    onPlaylistRemoteClick: (String, String) -> Unit = { _, _ -> },
    onShowSubscriptions: () -> Unit = {},
    onShowHistory: () -> Unit = {},
    activeDetailView: DetailView,
    playerViewModel: PlayerViewModel
) {
    val playbackState = playerViewModel.playbackState

    val homeViewModel: HomeViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                BottomBar(
                    selectedTab = selectedTab,
                    onTabClick = { screen ->
                        if (selectedTab != screen) {
                            tabHistory.add(selectedTab)
                            onTabSelected(screen)
                        }
                    }
                )
            }
        ) { mainPadding ->
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    FluxaAnimations.enterTransition() togetherWith FluxaAnimations.exitTransition()
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                Box(modifier = Modifier.fillMaxSize()) {
                    when (targetScreen) {

                        // Pantalla de inicio
                        Screen.Home -> HomeScreen(
                            mainPadding = mainPadding,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            isShowingResults = isShowingResults,
                            onResultsStateChange = onResultsStateChange,
                            onChannelClick = onChannelClick,
                            onVideoClick = onVideoClick,
                            onPlaylistClick = onPlaylistRemoteClick,
                            onCheckConnection = { videoViewModel.checkHomeConnection() },
                            onProfileClick = {
                                if (selectedTab != Screen.Library) {
                                    tabHistory.add(selectedTab)
                                    onTabSelected(Screen.Library)
                                }
                            },
                            homeViewModel = homeViewModel,
                            playerVM = playerViewModel
                        )

                        // Shorts
                        Screen.Shorts -> ShortsScreen(
                            innerPadding = mainPadding,
                            onChannelClick = onChannelClick,
                            isActiveTab = selectedTab == Screen.Shorts && activeDetailView == DetailView.None
                        )

                        // Colecciones
                        Screen.Library -> FluxaCollections(
                            modifier = Modifier.fillMaxSize().padding(mainPadding),
                            onChannelClick = onChannelClick,
                            onPlaylistClick = onPlaylistClick,
                            onShowSubscriptions = onShowSubscriptions,
                            onShowHistory = onShowHistory,
                            onVideoClick = onVideoClick,
                            errorType = videoViewModel.errorTypeState,
                            errorMessage = videoViewModel.errorMessageState,
                            onRetry = { videoViewModel.checkHomeConnection() },
                            isLoading = videoViewModel.isLoading,
                        )

                        // Ajustes
                        Screen.Settings -> SettingsScreen(
                            mainPadding = mainPadding,
                            themeMode = themeMode,
                            onThemeChange = onThemeChange,
                            amoledMode = amoledMode,
                            onAmoledModeChange = onAmoledModeChange,
                            snackbarHostState = snackbarHostState,
                            showMiniPlayer = playbackState.isActive && !playbackState.isFullyExpanded
                        )
                    }
                }
            }
        }

        // Mini Player
        if (playbackState.isActive && !playbackState.isFullyExpanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 82.dp, start = 8.dp, end = 8.dp)
            ) {
                MiniPlayer(
                    videoTitle = playbackState.title,
                    channelName = playbackState.channel,
                    thumbnailUrl = playbackState.thumbnailUrl,
                    isPlaying = playbackState.isPlaying,
                    progress = playbackState.progress,
                    onPlayPauseClick = { playerViewModel.togglePlayback() },
                    onCloseClick = { playerViewModel.closePlayer() },
                    onPlayerClick = { onExpandPlayer() }
                )
            }
        }
    }
}