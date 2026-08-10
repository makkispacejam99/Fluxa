package com.makkispacejam.fluxa.viewmodels.player

import org.schabi.newpipe.extractor.stream.SubtitlesStream
import org.schabi.newpipe.extractor.stream.AudioStream

data class PlaybackData(
    val currentVideoId: String = "",
    val videoUrl: String? = null,
    val title: String = "",
    val channel: String = "",
    val thumbnailUrl: String = "",
    val isActive: Boolean = false,
    val isFullyExpanded: Boolean = false,
    val isPlaying: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val progressMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val availableResolutions: List<Int> = emptyList(),
    val availableSubtitles: List<SubtitlesStream> = emptyList(),
    val availableAudioTracks: List<AudioStream> = emptyList(),
    val currentSubtitleText: String = "",
    val error: String? = null,
    val ageRestricted: Boolean = false,

    val playlistQueue: List<QueueItem> = emptyList(),
    val currentIndex: Int = -1,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffled: Boolean = false,
    val activeQuality: Int = 480,
    val defaultQuality: Int = 480,
    val adaptiveQualityEnabled: Boolean = true,
    val resetProgress: Boolean = false
)

enum class RepeatMode {
    OFF, ONE, ALL
}
