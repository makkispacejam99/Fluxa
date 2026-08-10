@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.ui.components.shorts.dialogs

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.dialogs.OptionDialog
import org.schabi.newpipe.extractor.stream.AudioStream

// Diálogo de calidad de audio
@Composable
fun ShortsAudioDialog(
    availableAudioTracks: List<AudioStream>,
    selectedAudioTrackDisplay: String,
    videoUrl: String,
    onAudioTrackChanged: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val uniqueTracks = availableAudioTracks
        .sortedByDescending { it.bitrate }
        .filter { it.audioLocale != null || (!it.audioTrackName.isNullOrBlank()) }
        .distinctBy { it.audioLocale?.toLanguageTag() ?: it.audioTrackName }

    val audioOptions = uniqueTracks.map { track ->
        val lang = track.audioLocale?.let {
            it.getDisplayName(LocalLocale.current.platformLocale)
                .replaceFirstChar { char -> char.uppercase() }
        } ?: track.audioTrackName ?: stringResource(R.string.unknown_audio)
        lang
    }

    val urlToDisplayMap = uniqueTracks.zip(audioOptions).associate { it.first.url to it.second }

    OptionDialog(
        title = stringResource(R.string.audio_tracks_dialog_title),
        options = audioOptions,
        selectedOption = selectedAudioTrackDisplay,
        onOptionSelected = { displayName ->
            val url = urlToDisplayMap.entries.find { it.value == displayName }?.key ?: ""
            if (url.isNotEmpty()) {
                val baseVideoUrl = videoUrl.substringBefore("|")
                onAudioTrackChanged("$baseVideoUrl|$url")
            }
            onDismiss()
        },
        onDismiss = onDismiss
    )
}
