package com.makkispacejam.fluxa.utils

import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.FluxaPlaybackService
import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.viewmodels.player.PlaybackData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.stream.StreamInfo

// Adaptador de calidad de video
@Suppress("DEPRECATION")
class AdaptiveQualityManager(
    private val scope: CoroutineScope,
    private val getState: () -> PlaybackData,
    private val updateState: (PlaybackData) -> Unit,
    private val getSubs: () -> List<MediaItem.SubtitleConfiguration>,
    private val attachListener: () -> Unit
) {
    var currentStreamInfo: StreamInfo? = null
    var streamInfoFetchedAt: Long = 0L
    val streamInfoTtlMs = 4 * 60 * 60 * 1000L
    var lastQualitySwitchMs: Long = 0L

    @OptIn(UnstableApi::class)
    suspend fun smoothSwitchQuality(newQuality: Int, currentPos: Long) {
        val streamInfo = currentStreamInfo ?: return
        if (streamInfoFetchedAt + streamInfoTtlMs < System.currentTimeMillis()) return
        val newUrl = VideoExtractor.findBestStream(streamInfo, newQuality) ?: return
        val subs = getSubs()
        updateState(getState().copy(activeQuality = newQuality))
        withContext(Dispatchers.Main) {
            FluxaPlaybackService.instance?.loadUrl(
                newUrl, getState().title, getState().channel,
                getState().thumbnailUrl, currentPos, subs, playImmediately = true,
                videoId = getState().currentVideoId
            )
            attachListener()
        }
    }

    @OptIn(UnstableApi::class)
    fun changeQuality(quality: Int, qualityError: String = "Could not change quality", streamError: String = "Could not get the stream") {
        scope.launch {
            updateState(getState().copy(isLoading = true))
            try {
                val currentPos = withContext(Dispatchers.Main) {
                    FluxaPlaybackService.instance?.getPlayer()?.currentPosition ?: 0L
                }
                val streamInfo = if (currentStreamInfo != null &&
                    (System.currentTimeMillis() - streamInfoFetchedAt) < streamInfoTtlMs
                ) currentStreamInfo!!
                else withContext(Dispatchers.IO) {
                    VideoExtractor.getStreamInfo(getState().currentVideoId)
                }.also { currentStreamInfo = it; streamInfoFetchedAt = System.currentTimeMillis() }

                if (streamInfo != null) {
                    val url = VideoExtractor.findBestStream(streamInfo, quality)
                    if (url != null) {
                        val subs = SubtitleConfigBuilder.buildConfigs(streamInfo.subtitles)

                        withContext(Dispatchers.Main) {
                            FluxaPlaybackService.instance?.loadUrl(
                                url, getState().title,
                                getState().channel, getState().thumbnailUrl, currentPos, subs,
                                videoId = getState().currentVideoId
                            )
                            attachListener()
                        }
                        updateState(getState().copy(
                            videoUrl = url, isLoading = false, isPlaying = true,
                            availableResolutions = ResolutionUtils.extractAvailableResolutions(streamInfo),
                            availableSubtitles = streamInfo.subtitles ?: emptyList(),
                            availableAudioTracks = streamInfo.audioStreams ?: emptyList(),
                            activeQuality = quality, defaultQuality = quality,
                            adaptiveQualityEnabled = false
                        ))
                    } else updateState(getState().copy(isLoading = false,
                        error = qualityError))
                } else updateState(getState().copy(isLoading = false,
                    error = streamError))
            } catch (e: Exception) {
                updateState(getState().copy(isLoading = false, error = e.message))
            }
        }
    }
}