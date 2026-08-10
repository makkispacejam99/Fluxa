package com.makkispacejam.fluxa.ui.screens.shared

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.viewmodels.user.InteractionViewModel
import com.makkispacejam.fluxa.R
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.data.local.PlaylistPersistenceManager
import com.makkispacejam.fluxa.data.newpipe.FluxaStreamItem
import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.ui.components.dialogs.VideoOptionsMenu
import com.makkispacejam.fluxa.ui.components.player.playlist.PlaylistSelectionDialog
import com.makkispacejam.fluxa.ui.components.player.playlist.ImportPlaylistDialog
import com.makkispacejam.fluxa.ui.components.system.NotificationBanner
import com.makkispacejam.fluxa.viewmodels.content.VideoViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay

@SuppressLint("LocalContextGetResourceValueCall")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    playlistTitle: String,
    playlistVideos: List<FluxaStreamItem>,
    fallbackChannel: String,
    isLoading: Boolean = false,
    onPlayPlaylist: (List<FluxaStreamItem>, Int, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
    onChannelClick: (String) -> Unit = {},
    interactionVM: InteractionViewModel = viewModel(),
    videoVM: VideoViewModel = viewModel(),
    currentVideoId: String = "",
    isMiniPlayerActive: Boolean = false
) {
    val context = LocalContext.current

    var savedProgress by remember(playlistTitle) {
        mutableStateOf(PlaylistPersistenceManager.getProgress(context, playlistTitle))
    }
    var showResumeButton by remember(savedProgress, playlistVideos) {
        mutableStateOf(savedProgress.first != -1 && playlistVideos.any { v ->
            VideoExtractor.cleanVideoId(v.url) == savedProgress.second
        })
    }

    LaunchedEffect(currentVideoId, playlistVideos.size) {
        if (currentVideoId.isNotEmpty() && playlistVideos.isNotEmpty()) {
            val index = playlistVideos.indexOfFirst { v ->
                VideoExtractor.cleanVideoId(v.url) == currentVideoId
            }
            if (index != -1) {
                PlaylistPersistenceManager.saveProgress(context, playlistTitle, index, currentVideoId)
                savedProgress = Pair(index, currentVideoId)
                showResumeButton = false
            }
        }
    }
    val subscriptions by interactionVM.getAllSubscriptions().collectAsState(initial = emptyList())
    val userPlaylists by videoVM.getUserPlaylistsFlow().collectAsState(initial = emptyList())
    val watchedVideoIds by interactionVM.watchedVideos.collectAsState(initial = emptyList())
    val watchedSet = remember(watchedVideoIds) { watchedVideoIds.toSet() }
    val alreadyWatchedMsg = stringResource(R.string.already_watched_msg)
    val savedToLaterMsg = stringResource(R.string.saved_to_later)
    val alreadyInPlaylistMsg = stringResource(R.string.already_in_playlist)
    val linkCopiedMsg = stringResource(R.string.link_copied)
    val viewsFormatMPattern = stringResource(R.string.views_format_m)
    val viewsFormatKPattern = stringResource(R.string.views_format_k)
    val viewsFormatPattern = stringResource(R.string.views_format)

    val subAvatarMap = remember(subscriptions, playlistVideos, videoVM.videoList.size) {
        val map = mutableMapOf<String, String>()
        subscriptions.forEach { sub ->
            sub.avatarUrl?.takeIf { it.isNotEmpty() }?.let { map[sub.channelName.lowercase()] = it }
        }
        playlistVideos.forEach { video ->
            if (video.uploaderAvatar.isNotEmpty()) map[video.uploaderName.lowercase()] = video.uploaderAvatar
        }
        videoVM.videoList.forEach { v ->
            v.channelAvatarUrl?.takeIf { it.isNotEmpty() }?.let { map[v.channelName.lowercase()] = it }
        }
        map
    }

    var selectedVideoForOptions by remember { mutableStateOf<FluxaStreamItem?>(null) }
    var videoForPlaylist by remember { mutableStateOf<FluxaStreamItem?>(null) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    val isLocal = remember(playlistTitle, userPlaylists) {
        playlistTitle == "Historial" ||
        playlistTitle == "Ver más tarde" ||
        playlistTitle == "Favoritos" ||
        playlistTitle.contains("Lista privada", ignoreCase = true) ||
        userPlaylists.any { it.name == playlistTitle }
    }

    val isUserPlaylist = remember(playlistTitle, userPlaylists) {
        userPlaylists.any { it.name == playlistTitle }
    }

    var showImportDialog by remember { mutableStateOf(false) }

    val shouldResetProgress = remember(playlistTitle, userPlaylists) {
        val isPreserveProgressPlaylist = playlistTitle == "Historial" || playlistTitle == "Ver más tarde"
        val isCustomPlaylist = playlistTitle == "Favoritos" ||
            playlistTitle.contains("Lista privada", ignoreCase = true) ||
            userPlaylists.any { it.name == playlistTitle }
        isCustomPlaylist && !isPreserveProgressPlaylist
    }

    var isEditMode by remember { mutableStateOf(false) }
    var selectedVideos by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var showEmptyState by remember { mutableStateOf(false) }
    LaunchedEffect(playlistVideos.isEmpty(), isLoading) {
        if (playlistVideos.isEmpty() && !isLoading) { delay(500); showEmptyState = true }
        else showEmptyState = false
    }

    LaunchedEffect(playlistVideos) {
        if (playlistVideos.isEmpty()) return@LaunchedEffect
        try {
            val uniqueChannels = playlistVideos.mapNotNull { video ->
                try { video.uploaderName to (video.channelId.ifEmpty { "" }) }
                catch (_: Exception) { null }
            }.filter { it.first.isNotEmpty() }.distinctBy { it.second.ifEmpty { it.first.lowercase() } }
            val toFetch = uniqueChannels.filter { (name, id) ->
                subAvatarMap[name.lowercase()] == null && videoVM.avatarCache[id.ifEmpty { name.lowercase() }] == null
            }
            if (toFetch.isEmpty()) return@LaunchedEffect
            toFetch.map { (name, id) -> async { videoVM.fetchAvatarForChannel(name, id) } }.awaitAll()
        } catch (_: Exception) {}
    }

    Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 72.dp)) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(modifier = Modifier.height(24.dp))
                    PlaylistHeader(
                        playlistTitle = playlistTitle,
                        playlistVideos = playlistVideos,
                        isUserPlaylist = isUserPlaylist,
                        isEditMode = isEditMode,
                        onToggleEditMode = {
                            isEditMode = !isEditMode
                            if (!isEditMode) selectedVideos = emptySet()
                        },
                        onImportClick = { showImportDialog = true },
                        showEditButton = isLocal
                    )
                    PlaylistActionButtons(
                        playlistVideos = playlistVideos,
                        onPlay = { index, shuffle -> onPlayPlaylist(playlistVideos, index, shuffle, shouldResetProgress) }
                    )
                    if (isEditMode && playlistVideos.isNotEmpty()) {
                        PlaylistEditModeToolbar(
                            selectedVideos = selectedVideos,
                            playlistVideos = playlistVideos,
                            onSelectionChanged = { selectedVideos = it },
                            onDeleteSelected = { showDeleteConfirm = true }
                        )
                    }
                }
            }

            if ((playlistVideos.isEmpty() && playlistTitle.isBlank()) || isLoading || (playlistVideos.isEmpty() && !showEmptyState)) {
                items(5) { PlaylistVideoHorizontalSkeleton() }
            } else if (playlistVideos.isEmpty()) {
                item { PlaylistEmptyState() }
            } else {
                itemsIndexed(playlistVideos, key = { index, video -> video.url + index }) { index, video ->
                    val videoId = remember(video.url) { VideoExtractor.cleanVideoId(video.url) }
                    if (videoId.isBlank()) return@itemsIndexed

                    val interaction by interactionVM.getInteraction(videoId).collectAsState(initial = null)
                    val progress = remember(interaction, video.duration) {
                        if (interaction == null || video.duration <= 0L) 0f
                        else { val p = interaction!!.progressMs.toFloat() / (video.duration * 1000f); if (p >= 0.95f) 0f else p.coerceIn(0f, 1f) }
                    }
                    val formattedViews = remember(video.views, isLocal) {
                        when {
                            isLocal || video.views == 0L -> ""
                            video.views >= 1_000_000 -> java.lang.String.format(java.util.Locale.US, viewsFormatMPattern, video.views / 1_000_000f)
                            video.views >= 1_000 -> java.lang.String.format(java.util.Locale.US, viewsFormatKPattern, video.views / 1_000f)
                            else -> java.lang.String.format(java.util.Locale.US, viewsFormatPattern, video.views)
                        }
                    }
                    val formattedDuration = remember(video.duration) {
                        if (video.duration <= 0) ""
                        else { val min = video.duration / 60; val sec = video.duration % 60
                            if (min >= 60) String.format(java.util.Locale.US, "%d:%02d:%02d", min / 60, min % 60, sec)
                            else String.format(java.util.Locale.US, "%02d:%02d", min, sec) }
                    }
                    val channelName = remember(video.uploaderName, fallbackChannel) {
                        video.uploaderName.ifEmpty { fallbackChannel }
                    }
                    val resolvedAvatar = remember(video.uploaderAvatar, channelName, subAvatarMap, video.channelId) {
                        video.uploaderAvatar.ifEmpty {
                            subAvatarMap[channelName.lowercase()] ?: videoVM.avatarCache[video.channelId.ifEmpty { channelName.lowercase() }] ?: ""
                        }
                    }
                    LaunchedEffect(video.channelId, channelName) {
                        if (resolvedAvatar.isEmpty() && channelName.isNotEmpty()) videoVM.getAvatarForChannel(channelName, video.channelId)
                    }

                    PlaylistVideoItemHorizontal(
                        video = video,
                        channelName = channelName,
                        formattedViews = formattedViews,
                        formattedDuration = formattedDuration,
                        progress = progress,
                        isWatched = watchedSet.contains(videoId),
                        isEditMode = isEditMode,
                        isSelected = selectedVideos.contains(videoId),
                        resolvedAvatar = resolvedAvatar,
                        publishedTime = video.uploadDate,
                        onToggleSelection = { selectedVideos = if (selectedVideos.contains(videoId)) selectedVideos - videoId else selectedVideos + videoId },
                        onChannelClick = { onChannelClick(channelName) },
                            onVideoClick = { onPlayPlaylist(playlistVideos, index, false, shouldResetProgress) },
                        onOptionsClick = { selectedVideoForOptions = video }
                    )
                }
            }
        }

        if (selectedVideoForOptions != null) {
            val vId = VideoExtractor.cleanVideoId(selectedVideoForOptions!!.url)
            val resolvedAvatar = selectedVideoForOptions!!.uploaderAvatar.ifEmpty { subAvatarMap[selectedVideoForOptions!!.uploaderName.lowercase()] ?: "" }
            VideoOptionsMenu(
                title = selectedVideoForOptions!!.title, channelName = selectedVideoForOptions!!.uploaderName,
                onDismiss = { selectedVideoForOptions = null },
                onSaveLater = {
                    videoVM.saveToPlaylist("Ver más tarde", vId, selectedVideoForOptions!!.title, selectedVideoForOptions!!.uploaderName, selectedVideoForOptions!!.thumbnail, resolvedAvatar, selectedVideoForOptions!!.duration, selectedVideoForOptions!!.channelId) { success ->
                        videoVM.notificationBannerText = if (success) savedToLaterMsg else alreadyInPlaylistMsg; videoVM.showNotificationBanner = true
                    }; selectedVideoForOptions = null
                },
                onAddToPlaylist = { videoForPlaylist = selectedVideoForOptions; showPlaylistDialog = true; selectedVideoForOptions = null },
                onShare = {
                    val shareLink = "https://youtu.be/$vId"
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Fluxa Video", shareLink))
                    Toast.makeText(context, linkCopiedMsg, Toast.LENGTH_SHORT).show(); selectedVideoForOptions = null
                },
                onMarkAsWatched = { interactionVM.markAsWatched(vId); videoVM.notificationBannerText = alreadyWatchedMsg; videoVM.showNotificationBanner = true; selectedVideoForOptions = null },
                showExtraOptions = false,
                showDeleteOption = true,
                onDeleteFromPlaylist = {
                    interactionVM.removeVideosFromPlaylist(playlistTitle, listOf(vId))
                    selectedVideoForOptions = null
                }
            )
        }

        if (showPlaylistDialog && videoForPlaylist != null) {
            val vId = VideoExtractor.cleanVideoId(videoForPlaylist!!.url)
            val resolvedAvatar = videoForPlaylist!!.uploaderAvatar.ifEmpty { subAvatarMap[videoForPlaylist!!.uploaderName.lowercase()] ?: "" }
            PlaylistSelectionDialog(
                userPlaylists = userPlaylists,
                onDismiss = { showPlaylistDialog = false; videoForPlaylist = null },
                onPlaylistSelected = { playlistName ->
                    videoVM.saveToPlaylist(playlistName, vId, videoForPlaylist!!.title, videoForPlaylist!!.uploaderName, videoForPlaylist!!.thumbnail, resolvedAvatar, videoForPlaylist!!.duration, videoForPlaylist!!.channelId) { success ->
                        videoVM.notificationBannerText = if (success) context.getString(R.string.saved_to_playlist, playlistName) else alreadyInPlaylistMsg; videoVM.showNotificationBanner = true
                    }; showPlaylistDialog = false; videoForPlaylist = null
                }
            )
        }

        if (showImportDialog) {
            ImportPlaylistDialog(
                playlistName = playlistTitle,
                onDismiss = { showImportDialog = false },
                onImport = { url -> interactionVM.importFromYouTubePlaylist(playlistTitle, url) }
            )
        }

        AnimatedVisibility(
            visible = isEditMode,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    if (selectedVideos.isNotEmpty()) showDeleteConfirm = true
                    else { isEditMode = false; selectedVideos = emptySet() }
                },
                containerColor = if (selectedVideos.isNotEmpty()) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Icon(
                    imageVector = if (selectedVideos.isNotEmpty()) Icons.Rounded.Delete else Icons.Rounded.Close,
                    contentDescription = if (selectedVideos.isNotEmpty()) stringResource(R.string.delete)
                        else stringResource(R.string.cancel_btn),
                    tint = if (selectedVideos.isNotEmpty()) MaterialTheme.colorScheme.onError
                        else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showDeleteConfirm) {
            DeleteConfirmationDialog(
                count = selectedVideos.size,
                onConfirm = {
                    interactionVM.removeVideosFromPlaylist(playlistTitle, selectedVideos.toList())
                    isEditMode = false; selectedVideos = emptySet(); showDeleteConfirm = false
                },
                onDismiss = { showDeleteConfirm = false }
            )
        }

        AnimatedVisibility(
            visible = showResumeButton && !isEditMode,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = if (isMiniPlayerActive) 116.dp else 32.dp, end = 16.dp)
        ) {
            ExtendedFloatingActionButton(
                onClick = {
                    val targetId = currentVideoId.ifEmpty { savedProgress.second }
                    val foundIndex = playlistVideos.indexOfFirst { VideoExtractor.cleanVideoId(it.url) == targetId }
                    val startIndex = if (foundIndex != -1) foundIndex else {
                        if (savedProgress.first in playlistVideos.indices) savedProgress.first else -1
                    }
                    if (startIndex != -1) {
                        val remaining = playlistVideos.filterIndexed { index, _ -> index >= startIndex }
                        onPlayPlaylist(remaining, 0, false, true)
                    } else if (playlistVideos.isNotEmpty()) {
                        onPlayPlaylist(playlistVideos, 0, false, true)
                    }
                    showResumeButton = false
                },
                icon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                text = { Text(stringResource(R.string.resume_playlist)) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }

        NotificationBanner(
            visible = videoVM.showNotificationBanner, text = videoVM.notificationBannerText,
            onDismiss = { videoVM.showNotificationBanner = false },
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding()
        )
    }
}
