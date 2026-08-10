package com.makkispacejam.fluxa.ui.components.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.data.local.VideoInteractionEntity
import com.makkispacejam.fluxa.models.home.HomeFeedItem
import com.makkispacejam.fluxa.models.home.HomeFeedItemType
import com.makkispacejam.fluxa.ui.components.core.*
import com.makkispacejam.fluxa.ui.components.home.topbar.recents.RecentSearchItem
import com.makkispacejam.fluxa.ui.components.home.topbar.search.SearchFilterRow

// Contenido del feed de inicio
@Composable
fun HomeFeedList(
    homeFeed: List<HomeFeedItem>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    interactionMap: Map<String, VideoInteractionEntity>,
    watchedSet: Set<String>,
    getAvatar: (String, String?) -> String?,
    onChannelClick: (String) -> Unit,
    onVideoClick: (String, String, String, String) -> Unit,
    onOptionsClick: (HomeFeedItem) -> Unit,
    showMiniPlayer: Boolean = false,
    listState: LazyListState = rememberLazyListState()
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (isRefreshing) {
                items(5) { VideoCardSkeleton() }
            } else {
                items(
                    items = homeFeed,
                    key = { it.videoId },
                    contentType = { "video" }
                ) { video ->
                    val avatar = getAvatar(video.channelId, video.channelAvatarUrl)
                    val interaction = interactionMap[video.videoId]
                    val progress = remember(interaction) {
                        if (interaction == null || video.durationSeconds <= 0L) 0f
                        else (interaction.progressMs.toFloat() / (video.durationSeconds * 1000f)).let { if (it >= 0.95f) 0f else it.coerceIn(0f, 1f) }
                    }

                    VideoCard(
                        title = video.title,
                        channel = video.channelName,
                        views = video.formattedViews,
                        duration = video.formattedDuration,
                        thumbnailUrl = video.thumbnailUrl,
                        uploaderAvatarUrl = avatar ?: "",
                        progress = progress,
                        publishedTime = video.publishedTime,
                        isWatched = watchedSet.contains(video.videoId),
                        onChannelClick = { onChannelClick(video.channelName) },
                        onVideoClick = { _, _ -> onVideoClick(video.title, video.videoId, video.channelName, video.thumbnailUrl) },
                        onOptionsClick = { onOptionsClick(video) }
                    )
                }
            }
            if (showMiniPlayer) {
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

// Contenido de los resultados de búsqueda
@Composable
fun SearchResultsList(
    isLoading: Boolean,
    results: List<HomeFeedItem>,
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    interactionMap: Map<String, VideoInteractionEntity>,
    watchedSet: Set<String>,
    getAvatar: (String, String?) -> String?,
    onChannelClick: (String) -> Unit,
    onVideoClick: (String, String, String, String) -> Unit,
    onPlaylistClick: (String, String) -> Unit,
    onOptionsClick: (HomeFeedItem) -> Unit,
    onLoadMore: () -> Unit = {},
    isLoadingMore: Boolean = false,
    hasMore: Boolean = false
) {
    Column {
        SearchFilterRow(
            selectedFilter = selectedFilter,
            onFilterSelected = onFilterSelected
        )

        if (isLoading) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(3) { VideoCardSkeleton() }
            }
        } else if (results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (selectedFilter == "En Vivo") stringResource(R.string.search_no_live)
                    else stringResource(R.string.search_no_results),
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn {
                items(
                    items = results,
                    key = { it.videoId },
                    contentType = { it.itemType }
                ) { item ->
                    when (item.itemType) {
                        HomeFeedItemType.VIDEO,
                        HomeFeedItemType.LIVE -> {
                            val avatar = getAvatar(item.channelId, item.channelAvatarUrl)
                            val interaction = interactionMap[item.videoId]
                            val progress = remember(interaction) {
                                if (interaction == null || item.durationSeconds <= 0L) 0f
                                else (interaction.progressMs.toFloat() / (item.durationSeconds * 1000f)).let { if (it >= 0.95f) 0f else it.coerceIn(0f, 1f) }
                            }
                            val isLiveItem = item.itemType == HomeFeedItemType.LIVE

                            VideoCard(
                                title = item.title,
                                channel = item.channelName,
                                views = item.formattedViews,
                                duration = if (isLiveItem) "LIVE" else item.formattedDuration,
                                thumbnailUrl = item.thumbnailUrl,
                                uploaderAvatarUrl = avatar ?: "",
                                progress = progress,
                                publishedTime = item.publishedTime,
                                isWatched = watchedSet.contains(item.videoId),
                                isLive = isLiveItem,
                                onChannelClick = { onChannelClick(item.channelName) },
                                onVideoClick = { _, _ -> onVideoClick(item.title, item.videoId, item.channelName, item.thumbnailUrl) },
                                onOptionsClick = { onOptionsClick(item) }
                            )
                        }
                        HomeFeedItemType.CHANNEL -> {
                            ChannelCard(
                                name = item.title,
                                subs = item.subscriberCount,
                                videoCount = item.playlistVideoCount,
                                avatarUrl = item.channelAvatarUrl,
                                onChannelClick = { onChannelClick(item.channelName) }
                            )
                        }
                        HomeFeedItemType.PLAYLIST -> {
                            PlaylistCard(
                                title = item.title,
                                videoCount = item.playlistVideoCount,
                                thumbnailUrl = item.thumbnailUrl,
                                onPlaylistClick = { onPlaylistClick(item.title, item.videoId) }
                            )
                        }
                    }
                }
                if (hasMore && results.isNotEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = onLoadMore,
                                enabled = !isLoadingMore,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
                                modifier = Modifier.height(44.dp)
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(stringResource(R.string.load_more))
                            }
                        }
                    }
                }
            }
        }
    }
}

// Búsquedas recientes
@Composable
fun RecentSearchHistory(
    recentSearches: List<String>,
    onSearchClick: (String) -> Unit,
    onRemoveSearch: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        LazyColumn {
            items(
                items = recentSearches,
                key = { it },
                contentType = { "recent_search" }
            ) { search ->
                RecentSearchItem(
                    search = search,
                    onClick = { onSearchClick(search) },
                    onRemove = { onRemoveSearch(search) },
                    padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            if (recentSearches.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    ) {
                        Text("Limpiar historial de búsqueda", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}