package com.makkispacejam.fluxa.viewmodels.content

import android.annotation.SuppressLint
import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.system.ErrorType
import com.makkispacejam.fluxa.data.HomeRepository
import com.makkispacejam.fluxa.data.avatars.AvatarRepository
import com.makkispacejam.fluxa.data.SearchRepository
import com.makkispacejam.fluxa.data.SearchPageResult
import com.makkispacejam.fluxa.data.channels.ChannelDataExtractor
import com.makkispacejam.fluxa.data.VideoExtractor
import org.schabi.newpipe.extractor.Page

import com.makkispacejam.fluxa.data.local.FluxaDatabase
import com.makkispacejam.fluxa.models.home.HomeFeedItem
import com.makkispacejam.fluxa.ui.components.home.topbar.recents.RecentsVideos
import com.makkispacejam.fluxa.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

@SuppressLint("AutoboxingStateCreation")
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = HomeRepository()
    val avatarRepo = AvatarRepository(viewModelScope)

    var homeFeed by mutableStateOf<List<HomeFeedItem>>(emptyList())
        private set
    var searchResults by mutableStateOf<List<HomeFeedItem>>(emptyList())
        private set
    var isLoadingSearch by mutableStateOf(false)
        private set
    var isLoadingMoreSearch by mutableStateOf(false)
        private set
    var hasMoreSearch by mutableStateOf(false)
        private set
    private var nextSearchPage: Page? = null
    private var _currentSearchQuery: String? = null
    private var _currentSearchFilter: String = "Todo"
    var isLoadingFeed by mutableStateOf(false)
        private set
    var errorType by mutableStateOf<ErrorType?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var recentSearches by mutableStateOf<List<String>>(emptyList())
        private set

    var loadedCount by mutableIntStateOf(0)
        private set
    var isRefreshingFeed by mutableStateOf(false)
        private set

    private fun isNetworkError() =
        !NetworkUtils.isInternetAvailable(getApplication<Application>().applicationContext)

    fun getAvatar(channelId: String, fallback: String): String =
        avatarRepo.getAvatar(channelId, fallback)

    fun loadHomeFeed() {
        if (isLoadingFeed) return
        if (isNetworkError()) {
            errorType = ErrorType.NO_INTERNET
            errorMessage = getApplication<Application>().getString(R.string.error_no_internet)
            return
        }
        isLoadingFeed = true
        errorType = null
        errorMessage = null
        val isFirstLoad = homeFeed.isEmpty()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val feed = repository.getHomeFeed(getApplication(), quickLoad = !isFirstLoad && !isRefreshingFeed)
                withContext(Main) {
                    homeFeed = feed
                    loadedCount = feed.size
                    isRefreshingFeed = false
                    if (feed.isEmpty()) {
                        errorType = ErrorType.SERVER_ERROR
                        errorMessage = getApplication<Application>().getString(R.string.error_empty_feed)
                    }
                    isLoadingFeed = false
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error cargando home feed: ${e.message}")
                withContext(Main) {
                    isRefreshingFeed = false
                    errorType = ErrorType.SERVER_ERROR
                    errorMessage = getApplication<Application>().getString(R.string.error_server)
                    isLoadingFeed = false
                }
            }
        }
    }

    fun refreshFeed() {
        repository.clearFeedCache()
        homeFeed = emptyList()
        isRefreshingFeed = true
        loadHomeFeed()
    }

    fun search(query: String, filter: String = "Todo") {
        if (query.isBlank()) return
        if (isNetworkError()) {
            errorType = ErrorType.NO_INTERNET
            errorMessage = getApplication<Application>().getString(R.string.error_no_internet)
            return
        }
        addRecentSearch(query)

        isLoadingSearch = true
        hasMoreSearch = false
        nextSearchPage = null
        _currentSearchQuery = query
        _currentSearchFilter = filter
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = SearchRepository.searchVideos(query, filter = filter)
                withContext(Main) {
                    searchResults = result.items
                    nextSearchPage = result.nextPage
                    hasMoreSearch = result.nextPage != null
                }
            } catch (_: Exception) {
                withContext(Main) { searchResults = emptyList() }
            } finally {
                withContext(Main) { isLoadingSearch = false }
            }
        }
    }

    fun loadMoreSearch() {
        if (isLoadingMoreSearch || nextSearchPage == null) return
        val currentQuery = _currentSearchQuery ?: return
        isLoadingMoreSearch = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var result = SearchRepository.loadMoreSearch(currentQuery, nextSearchPage!!, _currentSearchFilter)
                if (result.nextPage != null) {
                    val second = SearchRepository.loadMoreSearch(currentQuery, result.nextPage, _currentSearchFilter)
                    result = SearchPageResult(
                        items = (result.items + second.items).distinctBy { it.videoId },
                        nextPage = second.nextPage
                    )
                }
                withContext(Main) {
                    searchResults = (searchResults + result.items).distinctBy { it.videoId }
                    nextSearchPage = result.nextPage
                    hasMoreSearch = result.nextPage != null
                }
            } catch (_: Exception) {
            } finally {
                withContext(Main) { isLoadingMoreSearch = false }
            }
        }
    }

    fun clearSearch() { searchResults = emptyList(); hasMoreSearch = false; nextSearchPage = null }

    private fun addRecentSearch(query: String) {
        val current = recentSearches.toMutableList()
        current.remove(query)
        current.add(0, query)
        recentSearches = current.take(8)
    }

    fun removeRecentSearch(query: String) {
        recentSearches = recentSearches.filter { it != query }
    }

    fun clearRecentSearches() {
        recentSearches = emptyList()
    }

    fun removeVideoFromFeed(videoId: String) {
        homeFeed = homeFeed.filter { it.videoId != videoId }
    }

    fun removeChannelFromFeed(channelId: String) {
        homeFeed = homeFeed.filter { it.channelId != channelId }
    }

    var notifications by mutableStateOf<List<RecentsVideos>>(emptyList())
        private set
    var isLoadingNotifications by mutableStateOf(false)
        private set
    var isRefreshingNotifications by mutableStateOf(false)
        private set

    private companion object {
        const val MAX_RECENT_VIDEOS = 30
        const val MAX_RECENT_AGE_MS = 7L * 24 * 60 * 60 * 1000
    }

    private fun getTodayStart(): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        cal.set(java.util.Calendar.MINUTE, 0)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getTwoDaysAgoStart(): Long = getTodayStart() - 2L * 24 * 60 * 60 * 1000

    private suspend fun fetchNotifications(): List<RecentsVideos> = withContext(Dispatchers.IO) {
        val dao = FluxaDatabase.getDatabase(getApplication()).fluxaDao()
        val watchedIds = dao.allWatchedVideoIds.takeLast(500).toSet()
        val subs = dao.getAllSubscriptions() ?: emptyList()

        if (subs.isEmpty()) return@withContext emptyList()

        val allVideos = coroutineScope {
            subs.map { sub ->
                async(Dispatchers.IO) {
                    try {
                        withTimeoutOrNull(5000) {
                            val streams = ChannelDataExtractor.getChannelVideos("https://www.youtube.com/channel/${sub.channelId}")
                            streams.filter { !it.isLiveStream && it.duration > 0 }.map { item ->
                                RecentsVideos(
                                    videoId = VideoExtractor.cleanVideoId(item.url),
                                    title = item.title,
                                    channelName = sub.channelName ?: item.uploaderName,
                                    channelId = sub.channelId,
                                    channelAvatarUrl = sub.avatarUrl ?: item.uploaderAvatar,
                                    thumbnailUrl = item.thumbnail,
                                    durationSeconds = item.duration,
                                    publishedTime = item.uploadDate,
                                    timestamp = item.timestamp
                                )
                            }
                        } ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                }
            }.awaitAll().flatten()
        }

        val now = System.currentTimeMillis()
        val recentStart = getTwoDaysAgoStart()
        val unwatched = allVideos.filter { it.videoId !in watchedIds && it.timestamp > 0L }

        val recentVideos = unwatched
            .filter { it.timestamp >= recentStart }
            .distinctBy { it.videoId }

        if (recentVideos.isNotEmpty()) {
            return@withContext recentVideos.sortedByDescending { it.timestamp }.take(MAX_RECENT_VIDEOS)
        }

        return@withContext unwatched
            .filter { (now - it.timestamp) < MAX_RECENT_AGE_MS }
            .distinctBy { it.videoId }
            .sortedByDescending { it.timestamp }
            .take(MAX_RECENT_VIDEOS)
    }

    fun loadNotifications() {
        if (isLoadingNotifications) return
        if (notifications.isNotEmpty()) return
        isLoadingNotifications = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dao = FluxaDatabase.getDatabase(getApplication()).fluxaDao()
                val subs = dao.getAllSubscriptions()
                hasSubscriptions = !subs.isNullOrEmpty()

                val fresh = fetchNotifications()
                val result = fresh.sortedByDescending { it.timestamp }.take(MAX_RECENT_VIDEOS)

                withContext(Main) {
                    notifications = result
                    isLoadingNotifications = false
                }
            } catch (_: Exception) {
                withContext(Main) { isLoadingNotifications = false }
            }
        }
    }

    fun refreshNotifications() {
        if (isLoadingNotifications) return
        isRefreshingNotifications = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fresh = fetchNotifications()
                val result = (notifications + fresh)
                    .distinctBy { it.videoId }
                    .sortedByDescending { it.timestamp }
                    .take(MAX_RECENT_VIDEOS)

                withContext(Main) {
                    notifications = result
                    isRefreshingNotifications = false
                }
            } catch (_: Exception) {
                withContext(Main) { isRefreshingNotifications = false }
            }
        }
    }

    fun dismissNotification(videoId: String) {
        notifications = notifications.filter { it.videoId != videoId }
    }

    var hasSubscriptions by mutableStateOf(false)
        private set

    var lastPlayedNotificationId by mutableStateOf<String?>(null)
        private set

    fun markNotificationPlaying(videoId: String) {
        lastPlayedNotificationId = videoId
    }

    fun clearNotificationPlaying() {
        lastPlayedNotificationId = null
    }
}
