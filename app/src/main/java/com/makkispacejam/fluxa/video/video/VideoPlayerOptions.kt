@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.video.video

import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import com.makkispacejam.fluxa.ui.components.system.DialogOptions
import com.makkispacejam.fluxa.video.shorts.ShortsComments
import com.makkispacejam.fluxa.viewmodels.content.CommentsViewModel
import org.schabi.newpipe.extractor.stream.SubtitlesStream
import org.schabi.newpipe.extractor.stream.AudioStream
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale
import androidx.media3.common.util.UnstableApi

import com.makkispacejam.fluxa.ui.components.player.queue.QueuePanel
import com.makkispacejam.fluxa.viewmodels.player.PlayerViewModel

import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerDialogs(
    commentsViewModel: CommentsViewModel,
    playerViewModel: PlayerViewModel,
    ownerChannelId: String = "",
    ownerAvatarUrl: String? = null,
    showFullComments: Boolean,
    showQueuePanel: Boolean,
    showQualityDialog: Boolean,
    showSpeedDialog: Boolean,
    showSubtitlesDialog: Boolean,
    showAudioTracksDialog: Boolean,
    targetHeight: Int,
    availableResolutions: List<Int>,
    availableSubtitles: List<SubtitlesStream>,
    availableAudioTracks: List<AudioStream>,
    selectedSpeed: String,
    selectedSubtitle: String,
    selectedAudioTrack: String,
    onQualitySelected: (Int) -> Unit,
    onSpeedSelected: (String) -> Unit,
    onSubtitleSelected: (String) -> Unit,
    onAudioTrackSelected: (String) -> Unit,
    onDismissComments: () -> Unit,
    onDismissQueue: () -> Unit,
    onDismissQuality: () -> Unit,
    onDismissSpeed: () -> Unit,
    onDismissSubtitles: () -> Unit,
    onDismissAudioTracks: () -> Unit
) {
    if (showQualityDialog) {
        DialogOptions(
            currentResolution = targetHeight,
            availableResolutions = availableResolutions,
            onResolutionSelected = onQualitySelected,
            onDismissRequest = onDismissQuality
        )
    }

    if (showFullComments) {
        ShortsComments(
            comments = commentsViewModel.commentList,
            isLoading = commentsViewModel.isCommentsLoading,
            getAvatar = { id, url -> commentsViewModel.getAvatar(id, url) },
            ownerChannelId = ownerChannelId,
            ownerAvatarUrl = ownerAvatarUrl,
            onDismiss = onDismissComments
        )
    }

    if (showQueuePanel) {
        QueuePanel(
            playerViewModel = playerViewModel,
            onDismiss = onDismissQueue
        )
    }

    if (showSpeedDialog) {
        PlayerSettingsDialog(
            title = stringResource(R.string.speed_dialog_title),
            options = listOf("0.25x", "0.5x", "0.75x", stringResource(R.string.speed_normal), "1.5x", "2.0x"),
            selectedOption = selectedSpeed,
            onOptionSelected = onSpeedSelected,
            onDismiss = onDismissSpeed
        )
    }

    if (showSubtitlesDialog) {
        val offText = stringResource(R.string.subtitles_off)
        val subtitleOptions = mutableListOf(offText)
        val tagToNameMap = availableSubtitles.associate { sub ->
            val tag = sub.languageTag ?: "und"
            val name = Locale.forLanguageTag(tag).getDisplayName(LocalLocale.current.platformLocale).replaceFirstChar { it.uppercase() }
            tag to name
        }
        
        subtitleOptions.addAll(tagToNameMap.values)
        
        PlayerSettingsDialog(
            title = stringResource(R.string.subtitles_dialog_title),
            options = subtitleOptions,
            selectedOption = selectedSubtitle,
            onOptionSelected = { displayName ->
                val tag = if (displayName == offText) "" else tagToNameMap.entries.find { it.value == displayName }?.key ?: displayName
                onSubtitleSelected(tag)
            },
            onDismiss = onDismissSubtitles
        )
    }

    if (showAudioTracksDialog) {
        // Pistas de Audio
        val uniqueTracks = availableAudioTracks
            .sortedByDescending { it.bitrate }
            .filter { it.audioLocale != null || (!it.audioTrackName.isNullOrBlank()) }
            .distinctBy { it.audioLocale?.toLanguageTag() ?: it.audioTrackName }

        val unknownText = stringResource(R.string.unknown_audio)
        val audioOptions = uniqueTracks.map { track ->
            val lang = track.audioLocale?.let { 
                it.getDisplayName(LocalLocale.current.platformLocale)
                    .replaceFirstChar { char -> char.uppercase() }
            } ?: track.audioTrackName ?: unknownText
            lang
        }
        
        val urlToDisplayMap = uniqueTracks.zip(audioOptions).associate { it.first.url to it.second }

        PlayerSettingsDialog(
            title = stringResource(R.string.audio_tracks_dialog_title),
            options = audioOptions,
            selectedOption = selectedAudioTrack,
            onOptionSelected = { displayName ->
                val url = urlToDisplayMap.entries.find { it.value == displayName }?.key ?: ""
                if (url.isNotEmpty()) onAudioTrackSelected(url)
            },
            onDismiss = onDismissAudioTracks
        )
    }
}
