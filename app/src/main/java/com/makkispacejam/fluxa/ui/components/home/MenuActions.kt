package com.makkispacejam.fluxa.ui.components.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.data.local.PlaylistEntity
import com.makkispacejam.fluxa.models.home.HomeFeedItem
import com.makkispacejam.fluxa.ui.components.dialogs.VideoOptionsMenu
import com.makkispacejam.fluxa.ui.components.player.playlist.PlaylistSelectionDialog
import com.makkispacejam.fluxa.viewmodels.content.HomeViewModel
import com.makkispacejam.fluxa.viewmodels.user.InteractionViewModel
import com.makkispacejam.fluxa.viewmodels.content.VideoViewModel

/* Acciones del menu de inicio
ubicados en los 3 puntos */

@Composable
fun HomeActionMenus(
    selectedVideoForOptions: HomeFeedItem?,
    videoForPlaylist: HomeFeedItem?,
    onDismissOptions: () -> Unit,
    showPlaylistDialog: Boolean,
    onDismissPlaylist: () -> Unit,
    userPlaylists: List<PlaylistEntity>,
    videoVM: VideoViewModel,
    interactionVM: InteractionViewModel,
    homeViewModel: HomeViewModel,
    context: Context,
    messages: Map<String, String>,
    onAddToPlaylistTrigger: (HomeFeedItem) -> Unit
) {
    if (selectedVideoForOptions != null) {

        // Opciones del menu
        VideoOptionsMenu(
            title = selectedVideoForOptions.title,
            channelName = selectedVideoForOptions.channelName,
            onDismiss = onDismissOptions,
            onSaveLater = {
                val watchLaterText = messages["watchLater"] ?: "Watch Later"
                videoVM.saveToPlaylist(
                    watchLaterText,
                    selectedVideoForOptions.videoId,
                    selectedVideoForOptions.title,
                    selectedVideoForOptions.channelName,
                    selectedVideoForOptions.thumbnailUrl,
                    selectedVideoForOptions.channelAvatarUrl,
                    selectedVideoForOptions.durationSeconds,
                    selectedVideoForOptions.channelId
                ) { success ->
                    videoVM.notificationBannerText = if (success) messages["savedToLater"] ?: "" else messages["alreadyInPlaylist"] ?: ""
                    videoVM.showNotificationBanner = true
                }
                onDismissOptions()
            },
            onAddToPlaylist = {
                onAddToPlaylistTrigger(selectedVideoForOptions)
                onDismissOptions()
            },
            onShare = {
                val shareLink = "https://youtu.be/${selectedVideoForOptions.videoId}"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Fluxa Video", shareLink)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, messages["linkCopied"] ?: "", Toast.LENGTH_SHORT).show()
                onDismissOptions()
            },
            onMarkAsWatched = {
                interactionVM.markAsWatched(selectedVideoForOptions.videoId)
                homeViewModel.removeVideoFromFeed(selectedVideoForOptions.videoId)
                videoVM.notificationBannerText = messages["alreadyWatched"] ?: ""
                videoVM.showNotificationBanner = true
                onDismissOptions()
            },
            onNoRecommend = {
                interactionVM.toggleDislike(selectedVideoForOptions.videoId, selectedVideoForOptions.title, selectedVideoForOptions.channelName)
                homeViewModel.removeVideoFromFeed(selectedVideoForOptions.videoId)
                videoVM.notificationBannerText = messages["noRecommend"] ?: ""
                videoVM.showNotificationBanner = true
                onDismissOptions()
            },
            onBlockChannel = {
                val realAvatar = homeViewModel.getAvatar(
                    selectedVideoForOptions.channelId,
                    selectedVideoForOptions.channelAvatarUrl
                )
                interactionVM.blockChannel(
                    selectedVideoForOptions.channelId,
                    selectedVideoForOptions.channelName,
                    realAvatar
                )
                homeViewModel.removeChannelFromFeed(selectedVideoForOptions.channelId)
                videoVM.notificationBannerText = messages["channelBlocked"] ?: ""
                videoVM.showNotificationBanner = true
                onDismissOptions()
            }
        )
    }

    // Notificación de comportamiento
    if (showPlaylistDialog && videoForPlaylist != null) {
        PlaylistSelectionDialog(
            userPlaylists = userPlaylists,
            onDismiss = onDismissPlaylist,
            onPlaylistSelected = { playlistName ->
                videoVM.saveToPlaylist(
                    playlistName,
                    videoForPlaylist.videoId,
                    videoForPlaylist.title,
                    videoForPlaylist.channelName,
                    videoForPlaylist.thumbnailUrl,
                    videoForPlaylist.channelAvatarUrl,
                    videoForPlaylist.durationSeconds,
                    videoForPlaylist.channelId
                ) { success ->
                    videoVM.notificationBannerText = if (success) {
                        (messages["savedToPlaylist"] ?: "").format(playlistName)
                    } else {
                        messages["alreadyInPlaylist"] ?: ""
                    }
                    videoVM.showNotificationBanner = true
                }
                onDismissPlaylist()
            }
        )
    }
}

@Composable
fun homeMessages(): Map<String, String> {
    return mapOf(
        "alreadyWatched" to stringResource(R.string.already_watched_msg),
        "savedToLater" to stringResource(R.string.saved_to_later),
        "alreadyInPlaylist" to stringResource(R.string.already_in_playlist),
        "linkCopied" to stringResource(R.string.link_copied),
        "noRecommend" to stringResource(R.string.msg_no_recommend),
        "channelBlocked" to stringResource(R.string.msg_channel_blocked),
        "savedToPlaylist" to stringResource(R.string.saved_to_playlist),
        "watchLater" to stringResource(R.string.watch_later)
    )
}