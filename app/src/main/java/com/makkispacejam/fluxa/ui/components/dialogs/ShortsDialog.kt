package com.makkispacejam.fluxa.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsMoreOptionsDialog(
    onDismiss: () -> Unit,
    onQualityClick: () -> Unit,
    onAudioClick: () -> Unit,
    onShareClick: () -> Unit,
    onMarkAsWatchedClick: () -> Unit,
    onBlockClick: () -> Unit,
    onAudioNormalizeClick: () -> Unit,
    hasValidAudioTracks: Boolean = true,
    isIncognito: Boolean = false
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.more_options_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ShortsMenuOption(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    label = stringResource(R.string.normalize_audio),
                    onClick = { onAudioNormalizeClick(); onDismiss() }
                )
                ShortsMenuOption(
                    icon = Icons.Rounded.Settings,
                    label = stringResource(R.string.video_quality_short),
                    onClick = { onQualityClick(); onDismiss() }
                )
                if (hasValidAudioTracks) {
                    ShortsMenuOption(
                        icon = Icons.Rounded.Audiotrack,
                        label = stringResource(R.string.audio_quality_short),
                        onClick = { onAudioClick(); onDismiss() }
                    )
                }
                ShortsMenuOption(
                    icon = Icons.Rounded.Share,
                    label = stringResource(R.string.share_short),
                    onClick = { onShareClick(); onDismiss() }
                )
                if (!isIncognito) {
                    ShortsMenuOption(
                        icon = Icons.Rounded.Check,
                        label = stringResource(R.string.already_watched),
                        onClick = { onMarkAsWatchedClick(); onDismiss() }
                    )
                    ShortsMenuOption(
                        icon = Icons.Rounded.Block,
                        label = stringResource(R.string.block_channel),
                        onClick = { onBlockClick(); onDismiss() },
                        isError = true
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortsMenuOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isError: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
