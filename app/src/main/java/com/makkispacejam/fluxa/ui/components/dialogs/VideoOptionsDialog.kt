package com.makkispacejam.fluxa.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.core.TranslatedText
import com.makkispacejam.fluxa.utils.MusicChannelUtils

// Diálogo de opciones de video
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoOptionsMenu(
    title: String,
    channelName: String = "",
    onDismiss: () -> Unit,
    onSaveLater: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onShare: () -> Unit,
    showExtraOptions: Boolean = true,
    onMarkAsWatched: () -> Unit = {},
    onNoRecommend: () -> Unit = {},
    onBlockChannel: () -> Unit = {},
    showDeleteOption: Boolean = false,
    onDeleteFromPlaylist: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            TranslatedText(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp),
                skipTranslation = MusicChannelUtils.isMusicContent(channelName, title)
            )

            OptionMenuItem(
                icon = Icons.Rounded.Schedule,
                label = stringResource(R.string.watch_later),
                onClick = {
                    onSaveLater()
                    onDismiss()
                }
            )
            OptionMenuItem(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                label = stringResource(R.string.add_to_playlist),
                onClick = {
                    onAddToPlaylist()
                    onDismiss()
                }
            )
            OptionMenuItem(
                icon = Icons.Rounded.Share,
                label = stringResource(R.string.share_short),
                onClick = {
                    onShare()
                    onDismiss()
                }
            )

            if (showDeleteOption) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                OptionMenuItem(
                    icon = Icons.Rounded.Delete,
                    label = stringResource(R.string.delete_from_playlist),
                    onClick = { onDeleteFromPlaylist(); onDismiss() }
                )
            }

            if (showExtraOptions) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )

                OptionMenuItem(
                    icon = Icons.Rounded.Check,
                    label = stringResource(R.string.already_watched),
                    onClick = {
                        onMarkAsWatched()
                        onDismiss()
                    }
                )

                OptionMenuItem(
                    icon = Icons.Rounded.VisibilityOff,
                    label = stringResource(R.string.no_recommend_video),
                    onClick = {
                        onNoRecommend()
                        onDismiss()
                    }
                )

                OptionMenuItem(
                    icon = Icons.Rounded.Block,
                    label = stringResource(R.string.block_channel),
                    onClick = {
                        onBlockChannel()
                        onDismiss()
                    }
                )
            }
        }
    }
}

// Elementos del menu de opciones
@Composable
fun OptionMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
