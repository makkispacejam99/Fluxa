package com.makkispacejam.fluxa.ui.components.home.topbar.recents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.animations.FluxaAnimations
import com.makkispacejam.fluxa.ui.components.core.FluxaAvatar
import com.makkispacejam.fluxa.ui.components.core.TranslatedText
import com.makkispacejam.fluxa.utils.MusicChannelUtils
import java.util.Locale

data class RecentsVideos(
    val videoId: String,
    val title: String,
    val channelName: String,
    val channelId: String,
    val channelAvatarUrl: String,
    val thumbnailUrl: String,
    val durationSeconds: Long,
    val publishedTime: String,
    val timestamp: Long
) {
    val formattedDuration: String get() {
        val min = durationSeconds / 60
        val sec = durationSeconds % 60
        return if (min >= 60) {
            String.format(Locale.US, "%d:%02d:%02d", min / 60, min % 60, sec)
        } else {
            String.format(Locale.US, "%d:%02d", min, sec)
        }
    }
}

// Elementos de la pantalla de videos recientes
@Composable
fun RecentItem(
    video: RecentsVideos,
    onClick: () -> Unit,
    onChannelClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 120.dp, height = 68.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            if (video.durationSeconds > 0 && !video.formattedDuration.startsWith("0:00")) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                ) {
                    Text(
                        text = video.formattedDuration,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TranslatedText(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                skipTranslation = MusicChannelUtils.isMusicContent(video.channelName, video.title)
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FluxaAvatar(
                    avatarUrl = video.channelAvatarUrl,
                    size = 18.dp,
                    iconSize = 12.dp,
                    placeholderName = video.channelName,
                    modifier = Modifier.clickable(onClick = onChannelClick)
                )
                Text(
                    text = video.channelName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (video.publishedTime.isNotEmpty()) {
                Text(
                    text = video.publishedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// Pantalla completa de recientes
@Composable
fun RecentsOverlay(
    visible: Boolean,
    notifications: List<RecentsVideos>,
    isLoading: Boolean,
    isRefreshing: Boolean = false,
    hasSubscriptions: Boolean = true,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit = {},
    onVideoClick: (String, String, String, String) -> Unit,
    onChannelClick: (String) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = FluxaAnimations.searchOverlayEnter(),
        exit = FluxaAnimations.searchOverlayExit()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.latest_uploads),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Rounded.Close, null, modifier = Modifier.size(24.dp))
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)

                if (isLoading && notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                    }
                } else if (notifications.isEmpty() && !hasSubscriptions) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.recents_no_subscriptions),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else if (notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.notifications_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(items = notifications, key = { it.videoId }) { video ->
                                RecentItem(
                                    video = video,
                                    onClick = {
                                        onVideoClick(video.title, video.channelName, video.videoId, video.thumbnailUrl)
                                    },
                                    onChannelClick = { onChannelClick(video.channelName) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
