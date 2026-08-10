package com.makkispacejam.fluxa.ui.screens.shared

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.data.newpipe.ChannelViewModel
import com.makkispacejam.fluxa.data.newpipe.FluxaStreamItem
import com.makkispacejam.fluxa.ui.components.core.*
import com.makkispacejam.fluxa.ui.components.dialogs.VideoOptionsMenu
import com.makkispacejam.fluxa.ui.components.player.playlist.PlaylistSelectionDialog
import com.makkispacejam.fluxa.ui.components.system.NotificationBanner
import com.makkispacejam.fluxa.viewmodels.user.InteractionViewModel
import com.makkispacejam.fluxa.viewmodels.content.VideoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelProfileScreen(
    channelName: String,
    onPlaylistClick: (String, String) -> Unit,
    onVideoClick: (String, String, String) -> Unit,
    channelViewModel: ChannelViewModel = viewModel(),
    interactionVM: InteractionViewModel = viewModel(),
    videoVM: VideoViewModel = viewModel()
) {
    val context = LocalContext.current
    val isSubscribed by interactionVM.isSubscribed(channelViewModel.channelId ?: "").collectAsState(initial = false)
    val watchedVideoIds by interactionVM.watchedVideos.collectAsState(initial = emptyList())
    val watchedSet = remember(watchedVideoIds) { watchedVideoIds.toSet() }
    var selectedFilter by remember { mutableStateOf("Videos") }
    val isHeaderLoading = channelViewModel.channelAvatarUrl.isNullOrEmpty()
    val filterLabels = mapOf("Videos" to stringResource(R.string.filter_videos), "Playlist" to stringResource(R.string.filter_playlists), "En vivo" to stringResource(R.string.filter_live))
    val filters = listOf("Videos", "Playlist", "En vivo")

    var selectedVideoForOptions by remember { mutableStateOf<FluxaStreamItem?>(null) }
    var videoForPlaylist by remember { mutableStateOf<FluxaStreamItem?>(null) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    val userPlaylists by videoVM.getUserPlaylistsFlow().collectAsState(initial = emptyList())
    val alreadyWatchedMsg = stringResource(R.string.already_watched_msg)
    val savedToLaterMsg = stringResource(R.string.saved_to_later)
    val alreadyInPlaylistMsg = stringResource(R.string.already_in_playlist)
    val linkCopiedMsg = stringResource(R.string.link_copied)
    val noRecommendMsg = stringResource(R.string.msg_no_recommend)
    val channelBlockedMsg = stringResource(R.string.msg_channel_blocked)
    val savedToPlaylistTemplate = stringResource(R.string.saved_to_playlist)

    LaunchedEffect(channelName) { channelViewModel.loadChannelHeader(channelName) }

    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item { ChannelBanner(bannerUrl = channelViewModel.channelBannerUrl, isLoading = isHeaderLoading) }
            item {
                ChannelInfo(channelName = channelName, subscriberCount = channelViewModel.subscriberCount,
                    avatarUrl = channelViewModel.channelAvatarUrl, isSubscribed = isSubscribed, isLoading = isHeaderLoading,
                    onSubscribeClick = { channelViewModel.channelId?.let { id -> interactionVM.toggleSubscription(id, channelName, channelViewModel.channelAvatarUrl, isSubscribed) } })
            }
            item { ChannelFilterChips(selectedFilter = selectedFilter, filters = filters, filterLabels = filterLabels, onFilterSelected = { selectedFilter = it }) }

            if (channelViewModel.isLoadingContent) {
                when (selectedFilter) { "Playlist" -> items(2) { PlaylistCardSkeleton() }; else -> items(3) { VideoCardSkeleton() } }
            } else {
                val isEmpty = when (selectedFilter) {
                    "Videos" -> channelViewModel.videoList.isEmpty(); "Playlist" -> channelViewModel.playlistList.isEmpty(); "En vivo" -> channelViewModel.liveList.isEmpty(); else -> false
                }
                if (isEmpty) { item { ChannelEmptyContent(filterLabels = filterLabels, selectedFilter = selectedFilter) } }
                else {
                    when (selectedFilter) {
                        "Videos" -> {
                            items(channelViewModel.videoList.size) { index ->
                                val video = channelViewModel.videoList[index]
                                val videoId = remember(video.url) {
                                    when { video.url.contains("v=") -> video.url.substringAfter("v=").substringBefore("&"); video.url.contains("/shorts/") -> video.url.substringAfter("/shorts/").substringBefore("?"); else -> video.url.substringAfterLast("/") }
                                }
                                if (videoId.isBlank()) return@items
                                val interaction by interactionVM.getInteraction(videoId).collectAsState(initial = null)
                                val progress = remember(interaction) {
                                    if (interaction == null || video.duration <= 0L) 0f
                                    else (interaction!!.progressMs.toFloat() / (video.duration * 1000f)).let { if (it >= 0.95f) 0f else it.coerceIn(0f, 1f) }
                                }
                                val formattedViews = when {
                                    video.views >= 1_000_000 -> stringResource(R.string.views_format_m, video.views / 1_000_000f)
                                    video.views >= 1_000 -> stringResource(R.string.views_format_k, video.views / 1_000f)
                                    else -> stringResource(R.string.views_format, video.views)
                                }
                                val formattedDuration = remember(video.duration) {
                                    if (video.duration <= 0) "" else { val min = video.duration / 60; val sec = video.duration % 60
                                        if (min >= 60) String.format(java.util.Locale.US, "%d:%02d:%02d", min / 60, min % 60, sec) else String.format(java.util.Locale.US, "%02d:%02d", min, sec) }
                                }
                                VideoCard(title = video.title, channel = channelName, views = formattedViews, duration = formattedDuration, progress = progress,
                                    publishedTime = video.uploadDate, thumbnailUrl = video.url, uploaderAvatarUrl = channelViewModel.channelAvatarUrl ?: "",
                                    isWatched = watchedSet.contains(videoId), onChannelClick = { }, onVideoClick = { _, _ -> onVideoClick(video.title, videoId, channelName) },
                                    onOptionsClick = { selectedVideoForOptions = video })
                            }
                        }
                        "Playlist" -> {
                            items(channelViewModel.playlistList.size) { index ->
                                val playlist = channelViewModel.playlistList[index]
                                PlaylistCard(title = playlist.name, videoCount = playlist.videoCount.toString(), thumbnailUrl = playlist.thumbnail, onPlaylistClick = { onPlaylistClick(playlist.name, playlist.url) })
                            }
                        }
                        "En vivo" -> {
                            items(channelViewModel.liveList.size) { index ->
                                val live = channelViewModel.liveList[index]
                                val videoId = remember(live.url) { live.url.substringAfter("v=", "") }
                                val interaction by interactionVM.getInteraction(videoId).collectAsState(initial = null)
                                val progress = remember(interaction) {
                                    if (interaction == null || live.duration <= 0L) 0f
                                    else (interaction!!.progressMs.toFloat() / (live.duration * 1000f)).let { if (it >= 0.95f) 0f else it.coerceIn(0f, 1f) }
                                }
                                VideoCard(title = live.title, channel = channelName, views = stringResource(R.string.live_status), progress = progress, duration = "LIVE",
                                    publishedTime = live.uploadDate, thumbnailUrl = live.url, uploaderAvatarUrl = channelViewModel.channelAvatarUrl ?: "",
                                    isWatched = watchedSet.contains(videoId), onChannelClick = { }, onVideoClick = { _, _ -> onVideoClick(live.title, videoId, channelName) },
                                    onOptionsClick = { selectedVideoForOptions = live })
                            }
                        }
                    }
                }
            }
        }

        if (selectedVideoForOptions != null) {
            val videoId = selectedVideoForOptions!!.url.substringAfter("v=", "")
            VideoOptionsMenu(title = selectedVideoForOptions!!.title, channelName = selectedVideoForOptions!!.uploaderName, onDismiss = { selectedVideoForOptions = null },
                onSaveLater = {
                    videoVM.saveToPlaylist("Ver más tarde", videoId, selectedVideoForOptions!!.title, channelName, selectedVideoForOptions!!.url, channelViewModel.channelAvatarUrl ?: "", selectedVideoForOptions!!.duration, selectedVideoForOptions!!.channelId) { success ->
                        videoVM.notificationBannerText = if (success) savedToLaterMsg else alreadyInPlaylistMsg; videoVM.showNotificationBanner = true
                    }; selectedVideoForOptions = null
                },
                onAddToPlaylist = { videoForPlaylist = selectedVideoForOptions; showPlaylistDialog = true; selectedVideoForOptions = null },
                onShare = {
                    val shareLink = "https://youtu.be/$videoId"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Fluxa Video", shareLink))
                    Toast.makeText(context, linkCopiedMsg, Toast.LENGTH_SHORT).show(); selectedVideoForOptions = null
                },
                onMarkAsWatched = { interactionVM.markAsWatched(videoId); videoVM.notificationBannerText = alreadyWatchedMsg; videoVM.showNotificationBanner = true; selectedVideoForOptions = null },
                onNoRecommend = { interactionVM.toggleDislike(videoId, selectedVideoForOptions!!.title, channelName); Toast.makeText(context, noRecommendMsg, Toast.LENGTH_SHORT).show(); selectedVideoForOptions = null },
                onBlockChannel = { channelViewModel.channelId?.let { id -> interactionVM.blockChannel(id, channelName, channelViewModel.channelAvatarUrl) }; Toast.makeText(context, channelBlockedMsg, Toast.LENGTH_SHORT).show(); selectedVideoForOptions = null },
                showExtraOptions = false)
        }

        if (showPlaylistDialog && videoForPlaylist != null) {
            val videoId = videoForPlaylist!!.url.substringAfter("v=", "")
            PlaylistSelectionDialog(userPlaylists = userPlaylists, onDismiss = { showPlaylistDialog = false; videoForPlaylist = null },
                onPlaylistSelected = { playlistName ->
                    videoVM.saveToPlaylist(playlistName, videoId, videoForPlaylist!!.title, channelName, videoForPlaylist!!.url, channelViewModel.channelAvatarUrl ?: "", videoForPlaylist!!.duration, videoForPlaylist!!.channelId) { success ->
                        videoVM.notificationBannerText = if (success) savedToPlaylistTemplate.format(playlistName) else alreadyInPlaylistMsg; videoVM.showNotificationBanner = true
                    }; showPlaylistDialog = false; videoForPlaylist = null
                })
        }

        NotificationBanner(visible = videoVM.showNotificationBanner, text = videoVM.notificationBannerText, onDismiss = { videoVM.showNotificationBanner = false }, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding())
    }
}
