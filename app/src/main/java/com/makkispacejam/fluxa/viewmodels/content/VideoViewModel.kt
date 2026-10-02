package com.makkispacejam.fluxa.viewmodels.content

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.system.ErrorType
import com.makkispacejam.fluxa.data.ShortsRepository
import com.makkispacejam.fluxa.data.avatars.AvatarRepository
import com.makkispacejam.fluxa.data.metadata.VideoMetadataProvider
import com.makkispacejam.fluxa.data.shorts.ShortsSeenRegistry
import com.makkispacejam.fluxa.data.VideoExtractor

import com.makkispacejam.fluxa.models.VideoModel
import com.makkispacejam.fluxa.models.home.HomeFeedItem
import com.makkispacejam.fluxa.utils.NetworkUtils
import com.makkispacejam.fluxa.viewmodels.user.InteractionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val PAGE_LOAD_MORE = 30
    }

    private var consecutiveFailures = 0
    private val criticalFailureThreshold = 3
    private val repository = ShortsRepository()
    private val urlCache = mutableMapOf<String, String>()
    private val rateLimiter = RefreshRateLimiter(
        application.getSharedPreferences("fluxa_prefs", Context.MODE_PRIVATE),
    )

    private val interactionRepo = InteractionViewModel(application)

    fun getUserPlaylistsFlow() = interactionRepo.getUserPlaylists()

    fun saveToPlaylist(
        playlistName: String,
        videoId: String,
        title: String,
        channelName: String,
        thumbnailUrl: String,
        uploaderAvatarUrl: String,
        durationSec: Long,
        channelId: String = "",
        onResult: (Boolean) -> Unit
    ) {
        interactionRepo.addVideoToPlaylist(
            playlistName,
            videoId,
            title,
            channelName,
            channelId,
            thumbnailUrl,
            uploaderAvatarUrl,
            durationSec,
            onResult
        )
    }

    var errorTypeState by mutableStateOf<ErrorType?>(null)
        private set
    var errorMessageState by mutableStateOf<String?>(null)
        private set
    var videoList by mutableStateOf<List<VideoModel>>(emptyList())
        private set
    var isLoading by mutableStateOf(value = false)
        private set
    var videoDescriptionState by mutableStateOf("")
        private set
    var videoViewsState by mutableStateOf("")
        private set
    var cleanChannelNameState by mutableStateOf("")
        private set
    var currentVideoThumbnailState by mutableStateOf("")
        private set
    var channelAvatarState by mutableStateOf<String?>(null)
        private set
    var channelIdState by mutableStateOf("")
        private set
    var subscriberCountState by mutableStateOf("")
        private set
    var relatedVideosState by mutableStateOf<List<VideoModel>>(emptyList())
        private set

    private val videoMetadataProvider = VideoMetadataProvider()
    var currentPageIndex by mutableIntStateOf(0)
    var showCooldownBanner by mutableStateOf(false)
    var showNotificationBanner by mutableStateOf(false)
    var notificationBannerText by mutableStateOf("")

    val avatarRepo = AvatarRepository(viewModelScope)
    val avatarCache: Map<String, String> get() = avatarRepo.cachedAvatars

    fun getAvatarForChannel(channelName: String, channelId: String = ""): String =
        avatarRepo.getAvatarForChannel(channelName, channelId)

    suspend fun fetchAvatarForChannel(channelName: String, channelId: String = ""): String =
        avatarRepo.fetchAvatarForChannel(channelName, channelId)

    fun getAvatarUrl(video: VideoModel): String? {
        val channelId = video.channelId ?: ""
        if (channelId.isEmpty()) return null
        avatarRepo.getAvatar(channelId)
        return avatarRepo.getCachedAvatar(channelId)
    }

    fun hideVideo(videoId: String) {
        repository.hideShort(videoId)
        videoList = videoList.filter { it.id != videoId }
    }

    fun removeChannelVideos(channelId: String) {
        videoList = videoList.filter { it.channelId != channelId }
    }

    private fun ErrorType.handleFeedError(message: String, isCritical: Boolean = false) {
        if (isCritical) consecutiveFailures++
        if (consecutiveFailures >= criticalFailureThreshold) {
            errorTypeState = this
            errorMessageState = message
        } else {
            Log.w(
                "VideoViewModel",
                "Error del feed ($consecutiveFailures/$criticalFailureThreshold): $message"
            )
        }
    }

    fun clearErrors() {
        errorTypeState = null
        errorMessageState = null
        consecutiveFailures = 0
    }

    private fun isNetworkError(): Boolean {
        return !NetworkUtils.isInternetAvailable(getApplication())
    }

    private fun setNoInternet() {
        errorTypeState = ErrorType.NO_INTERNET
        errorMessageState = getApplication<Application>().getString(R.string.error_no_internet)
    }

    fun checkHomeConnection() {
        if (isNetworkError()) {
            setNoInternet()
        } else if (errorTypeState == ErrorType.NO_INTERNET) {
            clearErrors()
        }
    }

    fun fetchYoutubeShorts(forceRefresh: Boolean = false) {
        if (isNetworkError()) {
            setNoInternet()
            return
        }

        if (forceRefresh && !rateLimiter.canRefresh()) {
            showCooldownBanner = true
            return
        }

        isLoading = true
        viewModelScope.launch {
            try {
                ShortsSeenRegistry.attach(getApplication())
                val shorts = repository.getShorts(
                    getApplication(),
                    forceLoadMore = false,
                    forceRefresh = forceRefresh
                ) { preview ->
                    if (preview.isNotEmpty()) {
                        val known = videoList.mapTo(HashSet()) { it.id }
                        val filtered = preview.filter { it.id !in known }
                        if (filtered.isNotEmpty()) {
                            videoList = (videoList + filtered).distinctBy { it.id }.take(40)
                        }
                    }
                }
                if (shorts.isNotEmpty()) {
                    val current = videoList
                    videoList = if (current.isEmpty() || currentPageIndex == 0) {
                        shorts.take(40)
                    } else {
                        val known = current.mapTo(HashSet()) { it.id }
                        (current + shorts.filterNot { it.id in known })
                            .distinctBy { it.id }
                            .take(40)
                    }
                    shortsExhausted = false
                    emptyLoadStreak = 0
                    clearErrors()
                } else {
                    Log.w("VideoViewModel", "Shorts: la extraccion no devolvio videos")
                    notificationBannerText =
                        getApplication<Application>().getString(R.string.error_connect_server)
                    showNotificationBanner = true
                }
            } catch (e: Exception) {
                Log.e("VideoViewModel", "Error extrayendo shorts", e)
                    ErrorType.SERVER_ERROR.handleFeedError(
                        getApplication<Application>().getString(R.string.error_connect_server),
                    false
                )
            } finally {
                isLoading = false
            }
        }
    }

    fun shuffleShorts() {
        if (isLoading || isNetworkError()) return

        isLoading = true
        viewModelScope.launch {
            try {
                ShortsSeenRegistry.attach(getApplication())
                val fresh = repository.shuffleShorts(getApplication(), PAGE_LOAD_MORE)
                if (fresh.isEmpty()) {
                    fetchYoutubeShorts(forceRefresh = true)
                    return@launch
                }
                val known = videoList.mapTo(HashSet()) { it.id }
                videoList = (fresh + videoList.filter { it.id !in fresh.map { f -> f.id } && it.id !in known })
                    .distinctBy { it.id }
                    .take(40)
                currentPageIndex = 0
                shouldUpdatePagerIndex = true
                shortsExhausted = false
                emptyLoadStreak = 0
                clearErrors()
            } catch (e: Exception) {
                Log.e("VideoViewModel", "Error mezclando shorts", e)
            } finally {
                isLoading = false
            }
        }
    }

    fun loadMoreShorts() {
        if (isLoading || isNetworkError() || shortsExhausted) return

        isLoading = true
        viewModelScope.launch {
            try {
                val exclude = videoList.mapTo(HashSet()) { it.id }
                val moreShorts = repository.loadMore(getApplication(), exclude, PAGE_LOAD_MORE)

                if (moreShorts.isEmpty()) {
                    emptyLoadStreak++
                    if (emptyLoadStreak >= 2) shortsExhausted = true
                    return@launch
                }

                emptyLoadStreak = 0
                val combined = (videoList + moreShorts).distinctBy { it.id }

                if (combined.size > 50 && currentPageIndex > 25) {
                    val dropCount = 20
                    videoList = combined.drop(dropCount)
                    currentPageIndex -= dropCount
                    shouldUpdatePagerIndex = true
                } else {
                    videoList = combined.take(100)
                }
                consecutiveFailures = 0
            } catch (_: Exception) {
                Log.e("VideoViewModel", "Error cargando mas videos")
            } finally {
                isLoading = false
            }
        }
    }

    var shortsExhausted by mutableStateOf(false)
        private set

    private var emptyLoadStreak = 0

    var shouldUpdatePagerIndex by mutableStateOf(false)

    suspend fun extractUrlForVideo(videoId: String, targetQuality: Int = 480): String? =
        withContext(Dispatchers.IO) {
            val cacheKey = "$videoId-$targetQuality"
            urlCache[cacheKey]?.let { return@withContext it }

            var lastException: Exception? = null
            repeat(2) { attempt ->
                try {
                    val url = VideoExtractor.extractVideoUrl(videoId, targetQuality)
                    if (url != null) {
                        urlCache[cacheKey] = url
                        return@withContext url
                    }
                } catch (e: org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException) {
                    throw e
                } catch (e: Exception) {
                    lastException = e
                    if (attempt == 0) delay(200)
                }
            }
            Log.e("VideoViewModel", "Error extrayendo url para $videoId", lastException)
            null
        }

    suspend fun getAudioTracksForVideo(videoId: String): List<org.schabi.newpipe.extractor.stream.AudioStream> = withContext(Dispatchers.IO) {
        try {
            val info = VideoExtractor.getStreamInfo(videoId)
            info?.audioStreams ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    private var lastLoadedVideoId: String = ""

    fun loadDetailedMetadata(videoId: String) {
        if ((videoId == lastLoadedVideoId) && videoDescriptionState.isNotEmpty()) return

        videoDescriptionState = ""
        videoViewsState = ""
        subscriberCountState = ""
        channelAvatarState = null
        cleanChannelNameState = ""
        channelIdState = ""
        currentVideoThumbnailState = ""
        relatedVideosState = emptyList()

        viewModelScope.launch {
            try {
                val metadata = videoMetadataProvider.loadVideoData(videoId)
                if (metadata != null) {
                    lastLoadedVideoId = videoId
                    videoDescriptionState = metadata.description
                    videoViewsState = "${HomeFeedItem.formatCount(metadata.viewCount)} ${getApplication<Application>().getString(R.string.views)}"
                    cleanChannelNameState = metadata.uploaderName
                    currentVideoThumbnailState = metadata.videoModel.imageUrl
                    channelAvatarState = metadata.uploaderAvatarUrl
                    channelIdState = metadata.videoModel.channelId ?: ""
                    subscriberCountState = if (metadata.subscriberCount >= 0) {
                        "${HomeFeedItem.formatCount(metadata.subscriberCount)} ${getApplication<Application>().getString(R.string.subscribers)}"
                    } else ""
                    relatedVideosState = metadata.relatedVideos
                }
            } catch (e: Exception) {
                Log.e("VideoViewModel", "Error cargando metadata for $videoId", e)
                videoDescriptionState = getApplication<Application>().getString(R.string.error_detail_load)
            }
        }
    }
}
