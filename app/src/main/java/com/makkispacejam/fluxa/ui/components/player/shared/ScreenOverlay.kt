package com.makkispacejam.fluxa.ui.components.player.shared

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.makkispacejam.fluxa.ui.animations.FluxaAnimations
import com.makkispacejam.fluxa.data.newpipe.ChannelViewModel
import com.makkispacejam.fluxa.models.DetailView
import com.makkispacejam.fluxa.ui.screens.player.VideoPlayerScreen
import com.makkispacejam.fluxa.ui.screens.shared.ChannelProfileScreen
import com.makkispacejam.fluxa.ui.screens.shared.PlaylistScreen
import com.makkispacejam.fluxa.ui.screens.library.SubscriptionsScreen
import com.makkispacejam.fluxa.viewmodels.user.InteractionViewModel
import com.makkispacejam.fluxa.data.newpipe.FluxaStreamItem
import com.makkispacejam.fluxa.utils.ThumbnailUtils
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.ui.components.player.miniplayer.MiniPlayer
import com.makkispacejam.fluxa.viewmodels.player.PlayerViewModel

@OptIn(UnstableApi::class)
@Composable

// Ruta de pantallas compartidas
fun ScreenOverlay(
    activeDetailView: DetailView,
    detailTitle: String,
    detailVideoId: String,
    detailChannel: String,
    playlistTitle: String,
    channelViewModel: ChannelViewModel,
    interactionViewModel: InteractionViewModel,
    innerPadding: PaddingValues,
    openedFromCollections: Boolean = false,
    onPlaylistClick: (String, String) -> Unit,
    onVideoClick: (String, String, String) -> Unit,
    onPlayPlaylist: (List<FluxaStreamItem>, Int, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
    onChannelClick: (String) -> Unit = {},
    playerViewModel: PlayerViewModel? = null,
    onExpandPlayer: () -> Unit = {},
    onClose: () -> Unit
) {
    val playbackState = playerViewModel?.playbackState

    AnimatedVisibility(
        visible = activeDetailView != DetailView.None,
        enter = FluxaAnimations.detailEnter(),
        exit = FluxaAnimations.detailExit(),
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            val contentModifier = if (activeDetailView == DetailView.Player) {
                Modifier.fillMaxSize()
            } else {
                Modifier.fillMaxSize().padding(innerPadding)
            }

            Box(modifier = contentModifier) {
                AnimatedContent(
                    targetState = activeDetailView,
                    transitionSpec = {
                        FluxaAnimations.detailEnter() togetherWith FluxaAnimations.detailExit()
                    },
                    label = "DetailViewTransition"
                ) { currentView ->
                    when (currentView) {
                        DetailView.Channel -> ChannelProfileScreen(
                            channelName = detailChannel,
                            onPlaylistClick = onPlaylistClick,
                            onVideoClick = { title, videoId, _ ->
                                onVideoClick(title, videoId, detailChannel)
                            },
                            channelViewModel = channelViewModel
                        )
                        DetailView.Playlist -> {
                            val localVideos by interactionViewModel.getVideosFromPlaylist(playlistTitle).collectAsState(initial = emptyList())
                            val isLocal = openedFromCollections || playlistTitle == "Ver más tarde" || playlistTitle == "Favoritos"

                            PlaylistScreen(
                                playlistTitle = playlistTitle,
                                playlistVideos = if (isLocal) localVideos else channelViewModel.selectedPlaylistVideos,
                                fallbackChannel = detailChannel,
                                isLoading = if (!isLocal) channelViewModel.isLoadingContent else false,
                                onPlayPlaylist = onPlayPlaylist,
                                onChannelClick = onChannelClick,
                                currentVideoId = playbackState?.currentVideoId ?: "",
                                isMiniPlayerActive = playbackState != null && playbackState.isActive && !playbackState.isFullyExpanded
                            )
                        }
                        DetailView.Player -> VideoPlayerScreen(
                            videoId = detailVideoId,
                            videoTitle = detailTitle,
                            channelName = detailChannel,
                            onBackClick = onClose,
                            onChannelClick = onChannelClick
                        )
                        DetailView.Subscriptions -> SubscriptionsScreen(
                            interactionViewModel = interactionViewModel,
                            onChannelClick = onChannelClick
                        )
                        DetailView.History -> {
                            val historyEntities by interactionViewModel.getHistory().collectAsState(initial = emptyList())
                            val subscriptions by interactionViewModel.getAllSubscriptions().collectAsState(initial = emptyList())
                            val subAvatarMap = remember(subscriptions) {
                                subscriptions.associate { it.channelName.lowercase() to (it.avatarUrl ?: "") }
                            }
                            val historyItems = remember(historyEntities, subAvatarMap) {
                                historyEntities.map { entity ->
                                    FluxaStreamItem(
                                        url = entity.videoId,
                                        title = entity.title ?: "Sin título",
                                        thumbnail = ThumbnailUtils.getBestThumbnailUrl(entity.videoId, null),
                                        uploaderName = entity.channelName ?: "",
                                        views = 0L,
                                        duration = entity.durationMs / 1000,
                                        isLiveStream = false,
                                        uploaderAvatar = subAvatarMap[entity.channelName?.lowercase() ?: ""] ?: "",
                                        uploadDate = ""
                                    )
                                }
                            }
                            PlaylistScreen(
                                playlistTitle = "Historial",
                                playlistVideos = historyItems,
                                fallbackChannel = "",
                                onPlayPlaylist = onPlayPlaylist,
                                onChannelClick = onChannelClick,
                                currentVideoId = playbackState?.currentVideoId ?: "",
                                isMiniPlayerActive = playbackState != null && playbackState.isActive && !playbackState.isFullyExpanded
                            )
                        }
                        else -> {}
                    }
                }
            }
            
            if (playbackState != null && playbackState.isActive && !playbackState.isFullyExpanded && activeDetailView != DetailView.Player) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 12.dp, start = 8.dp, end = 8.dp)
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
}