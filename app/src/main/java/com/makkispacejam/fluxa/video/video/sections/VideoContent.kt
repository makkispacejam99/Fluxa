package com.makkispacejam.fluxa.video.video.sections

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddToPhotos
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbDownOffAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.core.ActionButton
import com.makkispacejam.fluxa.ui.components.core.SkeletonLine
import com.makkispacejam.fluxa.ui.components.core.SkeletonAvatar
import com.makkispacejam.fluxa.ui.components.core.TranslatedText
import com.makkispacejam.fluxa.viewmodels.content.CommentsViewModel
import com.makkispacejam.fluxa.viewmodels.content.VideoViewModel
import com.makkispacejam.fluxa.utils.MusicChannelUtils
import com.makkispacejam.fluxa.utils.stripHtml
import coil.compose.AsyncImage

import androidx.compose.runtime.*
import com.makkispacejam.fluxa.ui.components.player.playlist.PlaylistSelectionDialog
import com.makkispacejam.fluxa.video.video.ChannelSubscriptionCard

// Sección de botones de acción del reproductor horizontal
@SuppressLint("LocalContextGetResourceValueCall")
fun LazyListScope.videoContentSection(
    videoTitle: String,
    videoVM: VideoViewModel,
    channelName: String,
    isLiked: Boolean,
    isDisliked: Boolean,
    isSubscribed: Boolean,
    isExpanded: Boolean,
    commentsViewModel: CommentsViewModel,
    onLike: () -> Unit,
    onDislike: () -> Unit,
    onSubscribe: () -> Unit,
    onExpand: () -> Unit,
    onShowComments: () -> Unit,
    currentVideoId: String,
    onVideoClick: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    onChannelClick: (String) -> Unit = {},
    hideRelatedVideos: Boolean = false,
    isIncognito: Boolean = false
) {

    val isMetadataLoading = videoTitle.isBlank() && videoVM.videoDescriptionState.isBlank()

    item {
        Spacer(modifier = Modifier.height(16.dp))
        if (isMetadataLoading) {
            SkeletonLine(modifier = Modifier.fillMaxWidth(0.85f), height = 24.dp)
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onExpand() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                TranslatedText(
                    text = videoTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    skipTranslation = MusicChannelUtils.isMusicContent(channelName, videoTitle),
                    modifier = Modifier.weight(1f)
                )
                val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "arrow")
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(rotation).padding(start = 4.dp)
                )
            }
        }
    }

    item {
        if (isMetadataLoading) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                SkeletonLine(modifier = Modifier.width(120.dp), height = 14.dp)
                SkeletonLine(modifier = Modifier.fillMaxWidth(0.95f), height = 14.dp)
            }
        } else {
            Column {
                Text(
                    text = videoVM.videoViewsState,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                VideoDescription(
                    description = videoVM.videoDescriptionState.stripHtml(),
                    isExpanded = isExpanded,
                    onExpandClick = onExpand
                )
            }
        }
    }

    item {
        val context = LocalContext.current
        val resolvedChannelName = videoVM.cleanChannelNameState.ifBlank { channelName }
        val onDisabledAction = { videoVM.notificationBannerText = context.getString(R.string.incognito_action_blocked); videoVM.showNotificationBanner = true }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            //Botón Like
            ActionButton(
                text = stringResource(R.string.me_gusta),
                icon = if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                isActive = isLiked,
                enabled = !isIncognito,
                onClick = onLike,
                onDisabledClick = onDisabledAction
            )
            //Botón Dislike
            ActionButton(
                text = null,
                icon = if (isDisliked) Icons.Rounded.ThumbDown else Icons.Rounded.ThumbDownOffAlt,
                isActive = isDisliked,
                enabled = !isIncognito,
                onClick = onDislike,
                onDisabledClick = onDisabledAction
            )
            //Botón Colecciones
            var showPlaylistDialog by remember { mutableStateOf(false) }
            val userPlaylists by videoVM.getUserPlaylistsFlow().collectAsState(initial = emptyList())

            ActionButton(icon = Icons.Rounded.AddToPhotos, enabled = !isIncognito, onClick = { showPlaylistDialog = true }, onDisabledClick = onDisabledAction)

            if (showPlaylistDialog) {
                PlaylistSelectionDialog(
                    userPlaylists = userPlaylists,
                    onDismiss = { showPlaylistDialog = false },
                    onPlaylistSelected = { playlistName ->
                        videoVM.saveToPlaylist(
                            playlistName = playlistName,
                            videoId = currentVideoId,
                            title = videoTitle,
                            channelName = resolvedChannelName,
                            thumbnailUrl = videoVM.currentVideoThumbnailState,
                            uploaderAvatarUrl = videoVM.channelAvatarState ?: "",
                            durationSec = 0L,
                            channelId = videoVM.channelIdState,
                            onResult = { success ->
                                videoVM.notificationBannerText = if (success) {
                                    context.getString(R.string.saved_to_playlist, playlistName)
                                } else {
                                    context.getString(R.string.already_in_playlist_msg)
                                }
                                videoVM.showNotificationBanner = true
                            }
                        )
                        showPlaylistDialog = false
                    }
                )
            }

            //Botón Share
            ActionButton(icon = Icons.Rounded.Share, onClick = { 
                val shareLink = "https://youtu.be/$currentVideoId"
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Fluxa Video", shareLink)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.link_copied), Toast.LENGTH_SHORT).show()
            })
        }
    }

    item {
        Spacer(modifier = Modifier.height(20.dp))
        val resolvedChannelName = videoVM.cleanChannelNameState.ifBlank { channelName }
        val context2 = LocalContext.current
        val onDisabledAction = { videoVM.notificationBannerText = context2.getString(R.string.incognito_action_blocked); videoVM.showNotificationBanner = true }

        if (resolvedChannelName.isBlank() || videoVM.channelAvatarState == null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonAvatar(size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SkeletonLine(modifier = Modifier.width(140.dp), height = 16.dp)
                    SkeletonLine(modifier = Modifier.width(80.dp), height = 12.dp)
                }
                Spacer(modifier = Modifier.weight(1f))
                SkeletonLine(modifier = Modifier.width(90.dp), height = 36.dp, cornerRadius = 18.dp)
            }
        } else {
            ChannelSubscriptionCard(
                channelName = resolvedChannelName,
                channelAvatarUrl = videoVM.channelAvatarState,
                subscriberCount = videoVM.subscriberCountState,
                isSubscribed = isSubscribed,
                onSubscribeClick = onSubscribe,
                onChannelClick = onChannelClick,
                isIncognito = isIncognito,
                onDisabledClick = onDisabledAction
            )
        }
    }

    item {
        val currentChannelId = videoVM.channelIdState
        val sortedRelated = remember(videoVM.relatedVideosState, currentChannelId) {
            val sameChannel = videoVM.relatedVideosState.filter { it.channelId == currentChannelId }
            val others = videoVM.relatedVideosState.filter { it.channelId != currentChannelId }
            (sameChannel.take(3) + others).distinctBy { it.id }
        }

        if (sortedRelated.isNotEmpty() && !hideRelatedVideos) {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Text(
                    text = stringResource(id = R.string.related_videos_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    items(sortedRelated) { video ->
                        Column(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable {
                                    onVideoClick(
                                        video.title,
                                        video.id,
                                        video.channelName,
                                        video.imageUrl
                                    )
                                }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(90.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                AsyncImage(
                                    model = video.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TranslatedText(
                                text = video.title,
                                maxLines = 2,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                overflow = TextOverflow.Ellipsis,
                                skipTranslation = MusicChannelUtils.isMusicContent(video.channelName, video.title)
                            )
                            Text(
                                text = video.channelName,
                                maxLines = 1,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    item {
        Spacer(modifier = Modifier.height(24.dp))

        if (commentsViewModel.isCommentsLoading && commentsViewModel.commentList.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SkeletonLine(modifier = Modifier.width(100.dp), height = 18.dp)
                SkeletonLine(modifier = Modifier.fillMaxWidth(), height = 64.dp, cornerRadius = 12.dp)
            }
        } else {
            CommentsPreview(
                comments = commentsViewModel.commentList,
                isLoading = commentsViewModel.isCommentsLoading,
                getAvatar = { id, url -> commentsViewModel.getAvatar(id, url) },
                ownerChannelId = videoVM.channelIdState,
                ownerAvatarUrl = videoVM.channelAvatarState,
                onShowAll = onShowComments
            )
        }
    }
}