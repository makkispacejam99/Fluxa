package com.makkispacejam.fluxa.viewmodels.player

import android.annotation.SuppressLint
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope

import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.FluxaPlaybackService
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.data.local.FluxaDatabase
import com.makkispacejam.fluxa.data.local.PlaylistPersistenceManager
import com.makkispacejam.fluxa.data.newpipe.FluxaStreamItem
import com.makkispacejam.fluxa.utils.AdaptiveQualityManager
import com.makkispacejam.fluxa.utils.NetworkUtils
import com.makkispacejam.fluxa.utils.PlaybackProgressTracker
import com.makkispacejam.fluxa.utils.ResolutionUtils
import com.makkispacejam.fluxa.utils.SubtitleConfigBuilder
import com.makkispacejam.fluxa.utils.SubtitleUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException

@UnstableApi
@Suppress("DEPRECATION", "OPT_IN_ARGUMENT_IS_NOT_MARKER")
@SuppressLint("AutoboxingStateCreation")
class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    var playbackState by mutableStateOf(PlaybackData())
        private set

    var resizeMode by mutableIntStateOf(0)
        private set

    private var _currentPlaylistTitle by mutableStateOf("")
    private val dao = FluxaDatabase.getDatabase(application).fluxaDao()
    private val urlCache = mutableMapOf<String, String>()

    val playerControls = PlayerControls(
        scope = viewModelScope,
        getState = { playbackState },
        updateState = { playbackState = it },
        attachListener = ::attachPlayerListener
    )

    private val persistence = PlayerProgressPersistence(
        dao = dao,
        scope = viewModelScope,
        getState = { playbackState }
    )

    val playlistManager = PlaylistManager(
        scope = viewModelScope,
        onPlayVideo = ::loadAndPlayVideo,
        onClosePlayer = ::closePlayer,
        getState = { playbackState },
        updateState = { playbackState = it }
    )

    private val adaptiveQualityManager = AdaptiveQualityManager(
        scope = viewModelScope,
        getState = { playbackState },
        updateState = { playbackState = it },
        getSubs = { SubtitleConfigBuilder.buildConfigs(playbackState.availableSubtitles) },
        attachListener = ::attachPlayerListener
    )

    private var loadStartedAtMs = 0L
    private var loadCancelled = false

    private val progressTracker = PlaybackProgressTracker(
        scope = viewModelScope,
        getState = { playbackState },
        updateState = { playbackState = it },
        onAdaptiveCheck = { pos, dur, bufferRemaining ->
            val s = playbackState
            if (s.adaptiveQualityEnabled && bufferRemaining > 0L && pos < dur * 9 / 10) {
                val now = System.currentTimeMillis()
                if (now - adaptiveQualityManager.lastQualitySwitchMs > 12_000L) {
                    val resolutions = s.availableResolutions
                    val currQ = s.activeQuality
                    val defaultQ = s.defaultQuality
                    when {
                        bufferRemaining < 8_000L && currQ > resolutions.minOrNull()!! -> {
                            val lower = ResolutionUtils.findLowerQuality(resolutions, currQ)
                            if (lower != null) {
                                adaptiveQualityManager.lastQualitySwitchMs = now
                                viewModelScope.launch {
                                    adaptiveQualityManager.smoothSwitchQuality(lower, pos)
                                }
                            }
                        }
                        bufferRemaining > 40_000L && currQ < defaultQ -> {
                            val higher = ResolutionUtils.findHigherQuality(resolutions, currQ, defaultQ)
                            if (higher != null) {
                                adaptiveQualityManager.lastQualitySwitchMs = now
                                viewModelScope.launch {
                                    adaptiveQualityManager.smoothSwitchQuality(higher, pos)
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    private val playerListener = object : androidx.media3.common.Player.Listener {
        override fun onCues(cueGroup: androidx.media3.common.text.CueGroup) {
            val filteredText = SubtitleUtils.filterSubtitleText(cueGroup)
            if (filteredText.isEmpty()) {
                if (playbackState.currentSubtitleText.isNotEmpty()) {
                    playbackState = playbackState.copy(currentSubtitleText = "")
                }
                return
            }
            if (playbackState.currentSubtitleText != filteredText) {
                playbackState = playbackState.copy(currentSubtitleText = filteredText)
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            if (state == androidx.media3.common.Player.STATE_ENDED) handlePlaybackEnded()
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            playlistManager.skipToNext()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying && playbackState.currentVideoId.isNotEmpty()) {
                persistence.saveCurrentProgress()
            }
        }
    }

    private fun handlePlaybackEnded() {
        val videoId = playbackState.currentVideoId
        if (videoId.isNotEmpty()) {
            persistence.saveCurrentProgress()
            persistence.markAsWatched(videoId)
        }
        if (playbackState.repeatMode == RepeatMode.ONE) {
            playerControls.seekTo(0f)
            FluxaPlaybackService.instance?.getPlayer()?.play()
        } else {
            playlistManager.skipToNext()
        }
    }

    private fun attachPlayerListener() {
        FluxaPlaybackService.instance?.let { service ->
            val player = service.getPlayer()
            player.removeListener(playerListener)
            player.addListener(playerListener)

            service.onSkipNext = { skipToNext() }
            service.onSkipPrevious = { skipToPrevious() }
        }
    }

    @OptIn(UnstableApi::class)
    suspend fun loadAndPlayVideo(
        videoId: String, title: String, channel: String, thumb: String,
        fromQueue: Boolean = false
    ) {
        loadStartedAtMs = System.currentTimeMillis()
        loadCancelled = false
        if (fromQueue && playbackState.currentVideoId == videoId &&
            (playbackState.videoUrl != null || playbackState.isLoading)
        ) { expandPlayer(); return }
        withContext(Dispatchers.Main) {
            if (FluxaPlaybackService.instance == null) {
                val intent = android.content.Intent(getApplication(), FluxaPlaybackService::class.java)
                getApplication<Application>().startService(intent)
            }
            while (FluxaPlaybackService.instance == null) delay(100)
        }
        progressTracker.stop()
        urlCache.remove(videoId)
        adaptiveQualityManager.currentStreamInfo = null

        playbackState = playbackState.copy(
            currentVideoId = videoId, title = title, channel = channel, thumbnailUrl = thumb,
            isActive = true, isFullyExpanded = if (fromQueue) playbackState.isFullyExpanded else true, isLoading = true, isPlaying = false,
            currentSubtitleText = "", videoUrl = null, error = null, progress = 0f,
            progressMs = 0L, durationMs = 0L, bufferedMs = 0L,
            playlistQueue = if (fromQueue) playbackState.playlistQueue
                else listOf(QueueItem(videoId, title, channel, thumb)),
            currentIndex = if (fromQueue) playbackState.currentIndex else 0,
            availableResolutions = emptyList(), availableSubtitles = emptyList(),
            availableAudioTracks = emptyList()
        )

        if (_currentPlaylistTitle.isNotEmpty() && playbackState.playlistQueue.size > 1) {
            withContext(Dispatchers.IO) {
                PlaylistPersistenceManager.saveProgress(getApplication(), _currentPlaylistTitle, playbackState.currentIndex, videoId)
            }
        }

        try {
            val streamInfo = VideoExtractor.getStreamInfo(videoId)
            adaptiveQualityManager.currentStreamInfo = streamInfo
            adaptiveQualityManager.streamInfoFetchedAt = System.currentTimeMillis()

            if (streamInfo != null) {
                val prefs = UserPreferences(getApplication())
                val targetQuality = NetworkUtils.getTargetVideoQuality(getApplication(), prefs.videoQuality)
                val url = VideoExtractor.findBestStream(streamInfo, targetQuality)
                if (url != null) {
                    urlCache[videoId] = url
                    val subs = SubtitleConfigBuilder.buildConfigs(streamInfo.subtitles)
                    val startPos = withContext(Dispatchers.IO) {
                        if (playbackState.resetProgress || dao.isVideoWatched(videoId)) 0L
                        else dao.getInteraction(videoId)?.progressMs ?: 0L
                    }
                    withContext(Dispatchers.Main) {
                        if (loadCancelled) return@withContext
                        var waitMs = 0
                        while (FluxaPlaybackService.instance == null && waitMs < 5000) { delay(100); waitMs += 100 }
                        if (loadCancelled) { FluxaPlaybackService.instance?.getPlayer()?.stop(); return@withContext }
                        FluxaPlaybackService.instance?.loadUrl(
                            url, title, channel, thumb, startPos, subs, 
                            videoId = videoId,
                            queueSize = playbackState.playlistQueue.size,
                            currentIndex = playbackState.currentIndex
                        )
                        attachPlayerListener()
                    }
                    if (loadCancelled) { closePlayer(); return }
                    playbackState = playbackState.copy(
                        videoUrl = url, isLoading = false, isPlaying = true,
                        availableResolutions = ResolutionUtils.extractAvailableResolutions(streamInfo),
                        availableSubtitles = streamInfo.subtitles ?: emptyList(),
                        availableAudioTracks = streamInfo.audioStreams ?: emptyList(),
                        activeQuality = targetQuality, defaultQuality = targetQuality,
                        adaptiveQualityEnabled = prefs.videoQuality == "Automática"
                    )
                } else playbackState = playbackState.copy(isLoading = false,
                    error = getApplication<Application>().getString(R.string.error_video_load))
            } else playbackState = playbackState.copy(isLoading = false,
                error = getApplication<Application>().getString(R.string.error_info_load))
        } catch (_: AgeRestrictedContentException) {
            playbackState = playbackState.copy(isLoading = false, ageRestricted = true)
        } catch (e: Exception) {
            playbackState = playbackState.copy(isLoading = false, error = e.message)
        }
        if (fromQueue && playbackState.error != null) {
            delay(1500)
            playlistManager.skipToNext()
        }
        progressTracker.start()
    }

    fun expandPlayer() { playbackState = playbackState.copy(isFullyExpanded = true) }

    fun minimizePlayer() {
        if (loadStartedAtMs > 0 && System.currentTimeMillis() - loadStartedAtMs < 1500) {
            loadStartedAtMs = 0L
            loadCancelled = true
            closePlayer()
            return
        }
        loadStartedAtMs = 0L
        playbackState = playbackState.copy(isFullyExpanded = false)
        persistence.saveCurrentProgress()
    }

    fun resyncFromService() {
        val player = FluxaPlaybackService.instance?.getPlayer() ?: return
        if (player.isPlaying || player.playbackState == androidx.media3.common.Player.STATE_BUFFERING) {
            if (!playbackState.isActive) {
                val mediaItem = player.currentMediaItem ?: return
                val meta = mediaItem.mediaMetadata
                playbackState = playbackState.copy(
                    currentVideoId = playbackState.currentVideoId.ifEmpty {
                        mediaItem.localConfiguration?.uri?.toString()?.let { VideoExtractor.cleanVideoId(it) } ?: ""
                    },
                    title = meta.title?.toString() ?: playbackState.title,
                    channel = meta.artist?.toString() ?: playbackState.channel,
                    isActive = true,
                    isFullyExpanded = false,
                    isPlaying = player.isPlaying,
                    videoUrl = player.currentMediaItem?.localConfiguration?.uri?.toString()
                )
                progressTracker.start()
                attachPlayerListener()
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun closePlayer() {
        persistence.saveCurrentProgress()
        progressTracker.stop()
        adaptiveQualityManager.currentStreamInfo = null
        FluxaPlaybackService.instance?.getPlayer()?.stop()
        playbackState = PlaybackData()
    }

    fun toggleResizeMode() {
        resizeMode = playerControls.toggleResizeMode(resizeMode)
    }

    fun retryLoad() {
        val currentId = playbackState.currentVideoId
        if (currentId.isNotEmpty()) {
            viewModelScope.launch {
                loadAndPlayVideo(
                    currentId,
                    playbackState.title,
                    playbackState.channel,
                    playbackState.thumbnailUrl,
                    fromQueue = true
                )
            }
        }
    }

    fun seekTo(fraction: Float) = playerControls.seekTo(fraction)
    fun seekOffset(offsetMs: Long) = playerControls.seekOffset(offsetMs)
    fun updateProgress(currentMs: Long, totalMs: Long, bufferedMs: Long = 0L) =
        playerControls.updateProgress(currentMs, totalMs, bufferedMs)
    fun togglePlayback() = playerControls.togglePlayback()
    fun setPlaybackSpeed(speedStr: String) = playerControls.setPlaybackSpeed(speedStr)
    fun setSubtitleTrack(language: String) = playerControls.setSubtitleTrack(language)
    fun setAudioTrack(audioUrl: String) = playerControls.setAudioTrack(audioUrl)

    fun changeQuality(quality: Int) = adaptiveQualityManager.changeQuality(
        quality,
        qualityError = getApplication<Application>().getString(R.string.error_quality_change),
        streamError = getApplication<Application>().getString(R.string.error_stream_fetch)
    )

    fun skipToNext() = playlistManager.skipToNext()
    fun skipToPrevious() = playlistManager.skipToPrevious()
    fun playFromQueue(index: Int) = playlistManager.playFromQueue(index)
    fun removeFromQueue(index: Int) = playlistManager.removeFromQueue(index)
    fun moveQueueItem(from: Int, to: Int) = playlistManager.moveQueueItem(from, to)
    fun toggleShuffle() = playlistManager.toggleShuffle()
    fun toggleRepeatMode() = playlistManager.toggleRepeatMode()
    fun playPlaylist(videos: List<FluxaStreamItem>, startIndex: Int, shuffle: Boolean = false, resetProgress: Boolean = false) =
        playlistManager.playPlaylist(videos, startIndex, shuffle, resetProgress)

    fun setCurrentPlaylistTitle(title: String) {
        _currentPlaylistTitle = title
    }

    override fun onCleared() {
        persistence.saveCurrentProgress()
        progressTracker.stop()
        super.onCleared()
    }
}
