package com.makkispacejam.fluxa.viewmodels.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.FluxaPlaybackService
import com.makkispacejam.fluxa.utils.SubtitleConfigBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException

class PlayerControls(
    private val scope: CoroutineScope,
    private val getState: () -> PlaybackData,
    private val updateState: (PlaybackData) -> Unit,
    private val attachListener: () -> Unit
) {
    private var lastTapTime = 0L
    private var currentMultiplier = 1
    private var lastDirection = 0

    @OptIn(UnstableApi::class)
    fun togglePlayback() {
        FluxaPlaybackService.instance?.getPlayer()?.let { player ->
            if (player.playbackState != Player.STATE_READY) {
                player.playWhenReady = true
                updateState(getState().copy(isPlaying = true))
                return
            }
            player.playWhenReady = !player.playWhenReady
            updateState(getState().copy(isPlaying = player.playWhenReady))
        }
    }

    @OptIn(UnstableApi::class)
    fun setPlaybackSpeed(speedStr: String) {
        val speed = when (speedStr) {
            "0.25x" -> 0.25f; "0.5x" -> 0.5f; "0.75x" -> 0.75f
            "1.5x" -> 1.5f; "2.0x" -> 2.0f; else -> 1.0f
        }
        FluxaPlaybackService.instance?.getPlayer()?.setPlaybackSpeed(speed)
    }

    @OptIn(UnstableApi::class)
    fun setSubtitleTrack(language: String) {
        val player = FluxaPlaybackService.instance?.getPlayer() ?: return
        if (language.isEmpty()) {
            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                .clearOverrides()
                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                .build()
            return
        }
        val allGroups = player.currentTracks.groups
        val textGroups = allGroups.filter { it.type == C.TRACK_TYPE_TEXT }
        for (group in textGroups) {
            for (i in 0 until group.length) {
                val lang = group.getTrackFormat(i).language
                if (lang == language || lang?.startsWith(language.take(2)) == true) {
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .clearOverrides()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .setOverrideForType(
                            androidx.media3.common.TrackSelectionOverride(group.mediaTrackGroup, listOf(i))
                        )
                        .build()
                    return
                }
            }
        }
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverrides()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
            .setPreferredTextLanguage(language)
            .build()
    }

    @OptIn(UnstableApi::class)
    fun setAudioTrack(audioUrl: String) {
        val s = getState()
        if (s.currentVideoId.isEmpty()) return
        val currentVideoUrl = s.videoUrl?.substringBefore("|") ?: return
        val newFullUrl = "$currentVideoUrl|$audioUrl"

        scope.launch {
            updateState(getState().copy(isLoading = true))
            try {
                val currentPos = withContext(Dispatchers.Main) {
                    FluxaPlaybackService.instance?.getPlayer()?.currentPosition ?: 0L
                }
                val subs = SubtitleConfigBuilder.buildConfigs(s.availableSubtitles)
                withContext(Dispatchers.Main) {
                    FluxaPlaybackService.instance?.loadUrl(newFullUrl, s.title,
                        s.channel, s.thumbnailUrl, currentPos, subs, videoId = getState().currentVideoId)
                    attachListener()
                }
                updateState(getState().copy(videoUrl = newFullUrl,
                    isLoading = false, isPlaying = true))
            } catch (_: AgeRestrictedContentException) {
                updateState(getState().copy(isLoading = false, ageRestricted = true))
            } catch (e: Exception) {
                updateState(getState().copy(isLoading = false, error = e.message))
            }
        }
    }

    fun toggleResizeMode(current: Int): Int = when (current) { 0 -> 3; 3 -> 4; else -> 0 }

    @OptIn(UnstableApi::class)
    fun seekTo(fraction: Float) {
        val player = FluxaPlaybackService.instance?.getPlayer()
        val state = getState()
        val duration = player?.duration ?: state.durationMs
        if (duration > 0) player?.seekTo((fraction * duration).toLong())
        updateState(state.copy(
            progressMs = (fraction * state.durationMs).toLong(), progress = fraction))
    }

    @OptIn(UnstableApi::class)
    fun seekOffset(offsetMs: Long) {
        val player = FluxaPlaybackService.instance?.getPlayer() ?: return
        val now = System.currentTimeMillis()
        val direction = if (offsetMs > 0) 1 else -1
        currentMultiplier = if (direction == lastDirection && (now - lastTapTime) < 800L)
            (currentMultiplier + 1).coerceAtMost(5) else 1
        lastTapTime = now; lastDirection = direction
        val newPos = (player.currentPosition + (10000L * currentMultiplier * direction))
            .coerceIn(0L, player.duration.coerceAtLeast(0L))
        player.seekTo(newPos)
        updateProgress(newPos, player.duration)
    }

    fun updateProgress(currentMs: Long, totalMs: Long, bufferedMs: Long = 0L) {
        if (totalMs > 0) updateState(getState().copy(
            progressMs = currentMs, durationMs = totalMs, bufferedMs = bufferedMs,
            progress = currentMs.toFloat() / totalMs.toFloat()))
    }
}
