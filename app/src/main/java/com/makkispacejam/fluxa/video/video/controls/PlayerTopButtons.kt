package com.makkispacejam.fluxa.video.video.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.SubtitlesStream

// Controles superiores del reproductor horizontal
@Composable
fun PlayerTopButtons(
    onBackClick: () -> Unit,
    onResizeModeClick: () -> Unit,
    onPipClick: () -> Unit,
    availableSubtitles: List<SubtitlesStream>,
    availableAudioTracks: List<AudioStream>,
    onSubtitlesClick: () -> Unit,
    onAudioTracksClick: () -> Unit,
    onSpeedClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(12.dp))
                .size(40.dp),
            interactionSource = remember { MutableInteractionSource() }
        ) {
            Icon(
                Icons.Rounded.ExpandMore,
                null,
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }

        Row(
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.50f), RoundedCornerShape(12.dp))
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onResizeModeClick,
                modifier = Modifier.size(40.dp),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                Icon(Icons.Rounded.AspectRatio, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            IconButton(
                onClick = onPipClick,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(Icons.Rounded.PictureInPicture, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }

            if (availableSubtitles.isNotEmpty()) {
                IconButton(
                    onClick = onSubtitlesClick,
                    modifier = Modifier.size(40.dp),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Icon(Icons.Rounded.ClosedCaption, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            val hasValidAudioTracks = remember(availableAudioTracks) {
                availableAudioTracks.any { it.audioLocale != null || !it.audioTrackName.isNullOrBlank() }
            }
            if (hasValidAudioTracks) {
                IconButton(
                    onClick = onAudioTracksClick,
                    modifier = Modifier.size(40.dp),
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    Icon(Icons.Rounded.Audiotrack, null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }

            IconButton(
                onClick = onSpeedClick,
                modifier = Modifier.size(40.dp),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                Icon(Icons.Rounded.Speed, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(40.dp),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                Icon(Icons.Rounded.Settings, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
    }
}
