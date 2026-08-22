package com.makkispacejam.fluxa.viewmodels.user

import android.app.Application
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.data.local.*
import com.makkispacejam.fluxa.data.newpipe.FluxaStreamItem
import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.ui.components.player.playlist.ImportError
import com.makkispacejam.fluxa.ui.components.player.playlist.ImportResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class InteractionViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = FluxaDatabase.getDatabase(application).fluxaDao()
    val subscriptionSummary: Flow<List<SubscriptionEntity>> = dao.getSubscriptionSummaryFlow().distinctUntilChanged()
    val avatarCache = mutableStateMapOf<String, String>()

    fun getAvatar(channelId: String, currentUrl: String?): String? {
        if (!currentUrl.isNullOrEmpty()) return currentUrl
        return avatarCache[channelId]
    }

    fun isSubscribed(channelId: String): Flow<Boolean> = dao.isSubscribed(channelId).distinctUntilChanged()
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>> = dao.getAllSubscriptionsFlow()
    fun getBlockedSubscriptions(): Flow<List<SubscriptionEntity>> = dao.getBlockedSubscriptionsFlow()

    fun toggleSubscription(channelId: String, channelName: String, avatarUrl: String?, currentStatus: Boolean) {
        if (UserPreferences.incognitoActive) return
        viewModelScope.launch(Dispatchers.IO) {
            if (currentStatus) {

                if (dao.isChannelBlocked(channelId)) {
                    dao.deleteSubscription(SubscriptionEntity(channelId, channelName, avatarUrl))
                } else {
                    dao.deleteSubscription(SubscriptionEntity(channelId, channelName, avatarUrl))
                }
            } else {
                dao.insertSubscription(SubscriptionEntity(channelId, channelName, avatarUrl))
            }
        }
    }

    fun blockChannel(channelId: String, channelName: String, avatarUrl: String?): kotlinx.coroutines.Job {
        if (UserPreferences.incognitoActive) return viewModelScope.launch { }
        return viewModelScope.launch(Dispatchers.IO) {
            val entity = SubscriptionEntity(channelId, channelName, avatarUrl)
            entity.isBlocked = true
            dao.insertSubscription(entity)
        }
    }

    fun updateSubscriptionAvatar(channelId: String, avatarUrl: String) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.updateSubscriptionAvatar(channelId, avatarUrl)
        }
    }

    val allInteractions: Flow<List<VideoInteractionEntity>> = dao.getHistory()

    fun getInteraction(videoId: String): Flow<VideoInteractionEntity?> = dao.getInteractionFlow(videoId)
    fun getHistory(): Flow<List<VideoInteractionEntity>> = dao.getHistory()
    fun toggleLike(videoId: String, title: String?, channelName: String?) {
        if (UserPreferences.incognitoActive) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getInteraction(videoId)
            if (existing != null) {
                val newStatus = !existing.isLiked
                existing.isLiked = newStatus
                if (newStatus) existing.isDisliked = false
                if (title != null) existing.title = title
                if (channelName != null) existing.channelName = channelName
                existing.lastWatchedAt = System.currentTimeMillis()

                if (!existing.isLiked && !existing.isDisliked && existing.progressMs < 5000) {
                    dao.deleteInteraction(existing)
                } else {
                    dao.insertOrUpdateInteraction(existing)
                }
            } else {
                val interaction = VideoInteractionEntity(videoId)
                interaction.title = title
                interaction.channelName = channelName
                interaction.isLiked = true
                interaction.isDisliked = false
                dao.insertOrUpdateInteraction(interaction)
            }
        }
    }

    fun toggleDislike(videoId: String, title: String?, channelName: String?) {
        if (UserPreferences.incognitoActive) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getInteraction(videoId)
            if (existing != null) {
                val newStatus = !existing.isDisliked
                existing.isDisliked = newStatus
                if (newStatus) existing.isLiked = false
                if (title != null) existing.title = title
                if (channelName != null) existing.channelName = channelName
                existing.lastWatchedAt = System.currentTimeMillis()

                if (!existing.isLiked && !existing.isDisliked && existing.progressMs < 5000) {
                    dao.deleteInteraction(existing)
                } else {
                    dao.insertOrUpdateInteraction(existing)
                }
            } else {
                val interaction = VideoInteractionEntity(videoId)
                interaction.title = title
                interaction.channelName = channelName
                interaction.isLiked = false
                interaction.isDisliked = true
                dao.insertOrUpdateInteraction(interaction)
            }
        }
    }

    fun incrementViewCount(videoId: String, title: String? = null, channelName: String? = null) {
        if (UserPreferences.incognitoActive) return
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getInteraction(videoId)
            if (existing == null) {
                if (!title.isNullOrBlank() || !channelName.isNullOrBlank()) {
                    val interaction = VideoInteractionEntity(videoId)
                    interaction.title = title
                    interaction.channelName = channelName
                    interaction.viewCount = 1
                    dao.insertOrUpdateInteraction(interaction)
                }
            } else {
                dao.incrementViewCount(videoId, System.currentTimeMillis())
            }
        }
    }

    fun getUserPlaylists(): Flow<List<PlaylistEntity>> = dao.getUserPlaylists()

    fun createPlaylist(name: String) {
        if (UserPreferences.incognitoActive) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertPlaylist(PlaylistEntity(name, false))
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        if (UserPreferences.incognitoActive) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.deletePlaylist(playlist)
        }
    }

    fun updatePlaylist(playlist: PlaylistEntity) {
        if (UserPreferences.incognitoActive) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.updatePlaylist(playlist)
        }
    }

    fun removeVideosFromPlaylist(playlistName: String, videoIds: List<String>) {
        if (UserPreferences.incognitoActive) return
        viewModelScope.launch(Dispatchers.IO) {
            if (playlistName == "Historial") {
                videoIds.forEach { videoId ->
                    val interaction = dao.getInteraction(videoId)
                    if (interaction != null) {
                        interaction.lastWatchedAt = 0
                        if (!interaction.isLiked && !interaction.isDisliked && interaction.progressMs < 5000) {
                            dao.deleteInteraction(interaction)
                        } else {
                            dao.insertOrUpdateInteraction(interaction)
                        }
                    }
                }
                return@launch
            }

            val playlists = dao.getUserPlaylists().firstOrNull() ?: emptyList()
            var p = playlists.find { it.name == playlistName }
            if (p == null) p = dao.getSystemPlaylistByName(playlistName)

            p?.let { playlist ->
                videoIds.forEach { videoId ->
                    dao.removeItemFromPlaylist(playlist.id, videoId)
                }
            }
        }
    }

    fun getVideosFromPlaylist(playlistName: String): Flow<List<FluxaStreamItem>> {
        return getUserPlaylists().map { playlists ->
            val p = playlists.find { it.name == playlistName }
            p?.id ?: -1L
        }.flatMapLatest { playlistId ->
            val finalIdFlow = if (playlistId == -1L) {
                flow {
                    val sys = dao.getSystemPlaylistByName(playlistName)
                    emit(sys?.id ?: -1L)
                }.flowOn(Dispatchers.IO)
            } else {
                flowOf(playlistId)
            }

            finalIdFlow.flatMapLatest { id ->
                if (id == -1L) flowOf(emptyList())
                else dao.getItemsForPlaylist(id).map { items ->
                    items.map { item ->
                        FluxaStreamItem(
                            url = "https://www.youtube.com/watch?v=${item.videoId}",
                            title = item.title ?: "",
                            thumbnail = item.thumbnailUrl ?: "",
                            uploaderName = item.channelName ?: "",
                            views = 0L,
                            duration = item.durationSeconds,
                            isLiveStream = false,
                            uploaderAvatar = item.uploaderAvatarUrl ?: "",
                            uploadDate = "",
                            channelId = item.channelId ?: ""
                        )
                    }
                }
            }
        }
    }
    fun addVideoToPlaylist(
        playlistName: String, 
        videoId: String, 
        title: String, 
        channelName: String,
        channelId: String = "",
        thumbnailUrl: String,
        uploaderAvatarUrl: String,
        durationSec: Long,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (UserPreferences.incognitoActive) return
        viewModelScope.launch(Dispatchers.IO) {
            var playlistId = -1L
            val userPlaylists = dao.getUserPlaylists().map { list -> 
                list.find { it.name == playlistName } 
            }.firstOrNull()
            
            if (userPlaylists != null) {
                playlistId = userPlaylists.id
            } else {
                val sys = dao.getSystemPlaylistByName(playlistName)
                if (sys != null) {
                    playlistId = sys.id
                } else {
                    if (playlistName == "Ver más tarde" || playlistName == "Favoritos") {
                        playlistId = dao.insertPlaylist(PlaylistEntity(playlistName, true))
                    }
                }
            }
            
            if (playlistId != -1L) {
                if (!dao.isVideoInPlaylist(playlistId, videoId)) {
                    dao.insertPlaylistItem(PlaylistItemEntity(
                        playlistId, videoId, title, channelName, channelId, thumbnailUrl, uploaderAvatarUrl, durationSec
                    ))
                    withContext(Dispatchers.Main) { onResult(true) }
                } else {
                    withContext(Dispatchers.Main) { onResult(false) }
                }
            }
        }
    }

    val watchedVideos: Flow<List<String>> = dao.allWatchedVideoIdsFlow

    fun markAsWatched(videoId: String) {
        if (UserPreferences.incognitoActive) return
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertWatchedVideo(WatchedVideoEntity(videoId))
        }
    }

    suspend fun importFromYouTubePlaylist(
        playlistName: String,
        playlistUrl: String
    ): ImportResult = withContext(Dispatchers.IO) {
        if (UserPreferences.incognitoActive) return@withContext ImportResult.Success(0, 0)
        if (playlistUrl.isBlank()) {
            return@withContext ImportResult.Error(ImportError.NO_URL)
        }

        val playlists = dao.getUserPlaylists().firstOrNull() ?: emptyList()
        val userPlaylist = playlists.find { it.name == playlistName }
        val sysPlaylist = dao.getSystemPlaylistByName(playlistName)
        val playlistId = userPlaylist?.id ?: sysPlaylist?.id

        if (playlistId == null || playlistId == -1L) {
            return@withContext ImportResult.Error(ImportError.FETCH_FAILED)
        }

        val remoteVideos = try {
            VideoExtractor.extractPlaylistVideos(playlistUrl)
        } catch (_: Exception) {
            return@withContext ImportResult.Error(ImportError.FETCH_FAILED)
        }

        if (remoteVideos.isEmpty()) {
            return@withContext ImportResult.Error(ImportError.EMPTY)
        }

        var added = 0
        var skipped = 0

        for (video in remoteVideos) {
            val videoId = when {
                video.url.contains("v=") -> video.url.substringAfter("v=").substringBefore("&")
                video.url.contains("/shorts/") -> video.url.substringAfter("/shorts/").substringBefore("?")
                else -> video.url.substringAfterLast("/")
            }
            if (videoId.isBlank()) {
                skipped++
                continue
            }

            if (dao.isVideoInPlaylist(playlistId, videoId)) {
                skipped++
                continue
            }

            dao.insertPlaylistItem(
                PlaylistItemEntity(
                    playlistId,
                    videoId,
                    video.title,
                    video.uploaderName,
                    video.channelId,
                    video.thumbnail,
                    video.uploaderAvatar,
                    video.duration
                )
            )
            added++
        }

        return@withContext ImportResult.Success(added, skipped)
    }
}
