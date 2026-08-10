package com.makkispacejam.fluxa.ui.screens.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.ThemeMode
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.data.newpipe.ChannelViewModel
import com.makkispacejam.fluxa.data.newpipe.FluxaStreamItem
import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.models.*
import com.makkispacejam.fluxa.pendingOpenPlayer
import com.makkispacejam.fluxa.ui.animations.FluxaAnimations
import com.makkispacejam.fluxa.ui.components.layout.MainNavigationShell
import com.makkispacejam.fluxa.ui.components.player.shared.ScreenOverlay
import com.makkispacejam.fluxa.ui.components.system.SystemAppearanceEffect
import com.makkispacejam.fluxa.ui.screens.main.FluxaWelcome
import com.makkispacejam.fluxa.ui.screens.main.FluxaIntro
import com.makkispacejam.fluxa.ui.screens.main.FluxaSubs
import com.makkispacejam.fluxa.ui.theme.FluxaTheme
import com.makkispacejam.fluxa.viewmodels.player.PlayerViewModel
import com.makkispacejam.fluxa.viewmodels.content.VideoViewModel
import kotlinx.coroutines.launch
import kotlin.collections.getOrNull

/* Control de navegación y
flujo de la app entre menus */

@OptIn(UnstableApi::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val videoViewModel: VideoViewModel = viewModel()
    val channelViewModel: ChannelViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()

    // Pantalla de bienvenida
    val forceShowWelcome = false

    val tabHistory = rememberSaveable(saver = listSaver(
        save = { it.toList() },
        restore = { mutableStateListOf(*it.toTypedArray()) }
    )) { mutableStateListOf<Screen>() }

    var currentScreen by rememberSaveable {
        mutableIntStateOf(if (forceShowWelcome || prefs.isFirstRun) 0 else 3)
    }
    var activeDetailView by rememberSaveable { mutableStateOf(DetailView.None) }
    var selectedTab by rememberSaveable { mutableStateOf(Screen.Home) }
    var previousDetailView by rememberSaveable { mutableStateOf(DetailView.None) }
    var previousChannelName by rememberSaveable { mutableStateOf("") }

    // Estados de Metadatos
    var detailVideoId by rememberSaveable { mutableStateOf("") }
    var detailVideoTitle by rememberSaveable { mutableStateOf("") }
    var detailChannel by rememberSaveable { mutableStateOf("") }
    var playlistTitle by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isShowingResults by rememberSaveable { mutableStateOf(false) }
    var themeMode by remember { mutableStateOf(prefs.themeMode) }
    var amoledMode by remember { mutableStateOf(prefs.amoledMode) }
    var openedPlaylistFromCollections by rememberSaveable { mutableStateOf(false) }

    // Ciclo de vida del Reproductor
    val playbackState = playerViewModel.playbackState
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                playerViewModel.resyncFromService()
                if (pendingOpenPlayer.value && playbackState.isActive && !playbackState.isFullyExpanded) {
                    activeDetailView = DetailView.Player
                    detailVideoId = playbackState.currentVideoId
                    detailVideoTitle = playbackState.title
                    detailChannel = playbackState.channel
                    playerViewModel.expandPlayer()
                    pendingOpenPlayer.value = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onPlaylistFromCollections: (String) -> Unit = { title ->
        openedPlaylistFromCollections = true
        playlistTitle = title
        activeDetailView = DetailView.Playlist
    }

    val onPlaylistFromChannel: (String, String) -> Unit = { title, url ->
        openedPlaylistFromCollections = false
        previousDetailView = activeDetailView
        playlistTitle = title
        channelViewModel.loadPlaylistVideos(url)
        activeDetailView = DetailView.Playlist
    }

    val onChannelClickLambda: (String) -> Unit = { name ->
        detailChannel = name
        detailVideoTitle = name
        activeDetailView = DetailView.Channel
        if (playerViewModel.playbackState.isFullyExpanded) {
            playerViewModel.minimizePlayer()
        }
    }

    val onPlayPlaylistLambda: (List<FluxaStreamItem>, Int, Boolean, Boolean) -> Unit = { videos, index, shuffle, resetProgress ->
        val video = videos.getOrNull(index)
        if (video != null) {
            val videoId = VideoExtractor.cleanVideoId(video.url)
            detailVideoId = videoId
            detailVideoTitle = video.title
            detailChannel = video.uploaderName
        }
        activeDetailView = DetailView.Player
        playerViewModel.expandPlayer()
        playerViewModel.setCurrentPlaylistTitle(playlistTitle)
        playerViewModel.playPlaylist(videos, index, shuffle, resetProgress)
    }

    // Efectos y temas
    val isDark = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }

    SystemAppearanceEffect(isDark, amoledMode && isDark)

    // Gesto de volver atrás
    BackHandler(enabled = true) {
        when {
            playbackState.isActive && playbackState.isFullyExpanded -> {
                playerViewModel.minimizePlayer()
                activeDetailView = DetailView.None
            }
            activeDetailView == DetailView.Playlist -> {
                activeDetailView = if (openedPlaylistFromCollections) DetailView.None else previousDetailView.takeIf { it != DetailView.None } ?: DetailView.None
                previousDetailView = DetailView.None
            }
            activeDetailView == DetailView.History -> {
                activeDetailView = DetailView.None
            }
            activeDetailView == DetailView.Player -> {
                playerViewModel.minimizePlayer()
                activeDetailView = DetailView.None
            }
            activeDetailView != DetailView.None -> {
                activeDetailView = DetailView.None
            }
            isShowingResults -> {
                isShowingResults = false
                searchQuery = ""
            }
            tabHistory.isNotEmpty() -> {
                selectedTab = tabHistory.removeAt(tabHistory.size - 1)
            }
            currentScreen == 3 && selectedTab == Screen.Home -> activity?.finish()
            else -> activity?.finish()
        }
    }

    FluxaTheme(darkTheme = isDark, amoledMode = amoledMode && isDark) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Box(modifier = Modifier.fillMaxSize()) {

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { FluxaAnimations.introTransition() },
                    label = "IntroFlowTransition"
                ) { screenIndex ->
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        when (screenIndex) {
                            0 -> FluxaWelcome(Modifier.padding(innerPadding)) { currentScreen = 1 }
                            1 -> FluxaIntro(Modifier.padding(innerPadding)) { currentScreen = 2 }
                            2 -> FluxaSubs(Modifier.padding(innerPadding)) {
                                prefs.isFirstRun = false
                                currentScreen = 3
                            }
                            3 -> {
                                val snackbarHostState = remember { SnackbarHostState() }

                                Box(modifier = Modifier.fillMaxSize()) {
                                    MainNavigationShell(
                                        selectedTab = selectedTab,
                                        tabHistory = tabHistory,
                                        onTabSelected = { selectedTab = it },
                                        videoViewModel = videoViewModel,
                                        onExpandPlayer = {
                                            activeDetailView = DetailView.Player
                                            playerViewModel.expandPlayer()
                                        },
                                        searchQuery = searchQuery,
                                        onSearchQueryChange = {
                                            searchQuery = it
                                            if (it.isEmpty()) isShowingResults = false
                                        },
                                        isShowingResults = isShowingResults,
                                        onResultsStateChange = { isShowingResults = it },
                                        themeMode = themeMode,
                                        onThemeChange = {
                                            themeMode = it
                                            prefs.themeMode = it
                                        },
                                        amoledMode = amoledMode,
                                        onAmoledModeChange = {
                                            amoledMode = it
                                            prefs.amoledMode = it
                                        },
                                        snackbarHostState = snackbarHostState,
                                        onVideoClick = { title, videoId, channelName, thumbnailUrl ->
                                            previousDetailView = activeDetailView
                                            previousChannelName = detailVideoTitle
                                            detailVideoId = videoId
                                            detailVideoTitle = title
                                            detailChannel = channelName
                                            activeDetailView = DetailView.Player
                                            playerViewModel.expandPlayer()
                                            coroutineScope.launch {
                                                playerViewModel.loadAndPlayVideo(videoId, title, channelName, thumbnailUrl)
                                            }
                                        },
                                        onChannelClick = onChannelClickLambda,
                                        onPlaylistClick = onPlaylistFromCollections,
                                        onPlaylistRemoteClick = onPlaylistFromChannel,
                                        onShowSubscriptions = {
                                            activeDetailView = DetailView.Subscriptions
                                        },
                                        onShowHistory = {
                                            activeDetailView = DetailView.History
                                        },
                                        activeDetailView = activeDetailView,
                                        playerViewModel = playerViewModel
                                    )
                                }
                            }
                        }
                    }
                }

                ScreenOverlay(
                    activeDetailView = activeDetailView,
                    detailVideoId = detailVideoId,
                    detailTitle = detailVideoTitle,
                    detailChannel = detailChannel,
                    playlistTitle = playlistTitle,
                    channelViewModel = channelViewModel,
                    interactionViewModel = viewModel(),
                    innerPadding = innerPadding,
                    openedFromCollections = openedPlaylistFromCollections,
                    onPlaylistClick = onPlaylistFromChannel,
                    onVideoClick = { title, videoId, channelName ->
                        val matchedThumb = videoViewModel.videoList.find { it.id == videoId }?.imageUrl ?: ""
                        previousDetailView = activeDetailView
                        previousChannelName = detailChannel
                        detailVideoId = videoId
                        detailVideoTitle = title
                        detailChannel = channelName
                        activeDetailView = DetailView.Player
                        playerViewModel.expandPlayer()
                        coroutineScope.launch {
                            playerViewModel.loadAndPlayVideo(videoId, title, channelName, matchedThumb)
                        }
                    },
                    onPlayPlaylist = onPlayPlaylistLambda,
                    onChannelClick = onChannelClickLambda,
                    playerViewModel = playerViewModel,
                    onExpandPlayer = {
                        activeDetailView = DetailView.Player
                        playerViewModel.expandPlayer()
                    },
                    onClose = {
                        if (activeDetailView == DetailView.Player) {
                            playerViewModel.minimizePlayer()
                        }

                        activeDetailView = if (previousDetailView != DetailView.None) {
                            previousDetailView
                        } else {
                            DetailView.None
                        }
                        if (previousDetailView == DetailView.Channel && previousChannelName.isNotEmpty()) {
                            detailChannel = previousChannelName
                            detailVideoTitle = previousChannelName
                            previousChannelName = ""
                        }
                        previousDetailView = DetailView.None
                    }
                )
            }
        }
    }
}
