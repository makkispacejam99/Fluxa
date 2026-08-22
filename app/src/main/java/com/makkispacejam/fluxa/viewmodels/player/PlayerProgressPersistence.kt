package com.makkispacejam.fluxa.viewmodels.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.FluxaPlaybackService
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.data.local.WatchedVideoEntity
import com.makkispacejam.fluxa.data.local.VideoInteractionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlayerProgressPersistence(
    private val dao: com.makkispacejam.fluxa.data.local.FluxaDao,
    private val scope: CoroutineScope,
    private val getState: () -> PlaybackData
) {
    @OptIn(UnstableApi::class)
    fun saveCurrentProgress() {
        if (UserPreferences.incognitoActive) return
        val state = getState()
        val videoId = state.currentVideoId
        if (videoId.isEmpty()) return
        FluxaPlaybackService.instance?.getPlayer()?.let { p ->
            val pos = p.currentPosition
            val dur = p.duration
            if (dur > 0 && pos > 0 && pos >= 12_000L) scope.launch(Dispatchers.IO) {
                val existing = dao.getInteraction(videoId)
                if (existing != null) dao.updateProgress(videoId, pos, dur, System.currentTimeMillis())
                else if (state.title.isNotBlank() || state.channel.isNotBlank()) {
                    dao.insertOrUpdateInteraction(
                        VideoInteractionEntity(videoId).apply {
                            title = state.title; channelName = state.channel
                            viewCount = 1; progressMs = pos; durationMs = dur
                            lastWatchedAt = System.currentTimeMillis()
                        })
                }
                if (pos.toDouble() / dur.toDouble() >= 0.95) {
                    dao.insertWatchedVideo(WatchedVideoEntity(videoId))
                }
            }
        }
    }

    fun markAsWatched(videoId: String) {
        if (UserPreferences.incognitoActive) return
        scope.launch(Dispatchers.IO) {
            dao.insertWatchedVideo(WatchedVideoEntity(videoId))
        }
    }
}
