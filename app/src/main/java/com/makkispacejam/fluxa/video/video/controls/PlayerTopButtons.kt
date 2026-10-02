package com.makkispacejam.fluxa.video.video.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.schabi.newpipe.extractor.stream.SubtitlesStream

@Composable
fun PlayerTopButtons(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onResizeModeClick: () -> Unit = {},
    onPipClick: () -> Unit = {},
    availableSubtitles: List<SubtitlesStream> = emptyList(),
    onSubtitlesClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onMoreOptionsClick: () -> Unit = {}
) {
    val bgOpacity = 0.50f

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .background(Color.Black.copy(alpha = bgOpacity), CircleShape)
                .size(42.dp),
            interactionSource = remember { MutableInteractionSource() }
        ) {
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onMoreOptionsClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = bgOpacity), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.MoreHoriz,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onPipClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = bgOpacity), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.PictureInPicture,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = onResizeModeClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = bgOpacity), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.AspectRatio,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (availableSubtitles.isNotEmpty()) {
                IconButton(
                    onClick = onSubtitlesClick,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = bgOpacity), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Rounded.ClosedCaption,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = bgOpacity), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Rounded.Settings,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
