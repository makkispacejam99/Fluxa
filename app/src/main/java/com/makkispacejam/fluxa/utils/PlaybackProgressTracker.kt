package com.makkispacejam.fluxa.utils

import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.FluxaPlaybackService
import com.makkispacejam.fluxa.viewmodels.player.PlaybackData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Validación de progreso de video
@Suppress("OPT_IN_ARGUMENT_IS_NOT_MARKER")
@UnstableApi
@OptIn(UnstableApi::class)
class PlaybackProgressTracker(
    private val scope: CoroutineScope,
    private val getState: () -> PlaybackData,
    private val updateState: (PlaybackData) -> Unit,
    private val onAdaptiveCheck: (pos: Long, dur: Long, bufferRemaining: Long) -> Unit
) {
    private var progressJob: Job? = null

    fun start() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                FluxaPlaybackService.instance?.getPlayer()?.let { p ->
                    val dur = p.duration.coerceAtLeast(0L)
                    if (dur > 0) {
                        val pos = p.currentPosition
                        val buf = p.bufferedPosition.coerceAtLeast(0L)
                        val bufferRemaining = buf - pos
                        updateState(getState().copy(
                            progressMs = pos, durationMs = dur,
                            bufferedMs = buf, progress = pos.toFloat() / dur.toFloat()
                        ))
                        onAdaptiveCheck(pos, dur, bufferRemaining)
                    }
                    if (getState().isPlaying != p.isPlaying) {
                        updateState(getState().copy(isPlaying = p.isPlaying))
                    }
                }
                delay(500)
            }
        }
    }

    fun stop() {
        progressJob?.cancel()
    }
}