package com.makkispacejam.fluxa.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.window.Dialog
import com.makkispacejam.fluxa.R

// Diálogo de mas opciones de shorts
@Composable
fun ShortsMoreOptionsDialog(
    onDismiss: () -> Unit,
    onQualityClick: () -> Unit,
    onAudioClick: () -> Unit,
    onShareClick: () -> Unit,
    onMarkAsWatchedClick: () -> Unit,
    onBlockClick: () -> Unit,
    hasValidAudioTracks: Boolean = true,
    isIncognito: Boolean = false
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stringResource(R.string.options_title), 
                    style = MaterialTheme.typography.titleMedium, 
                    fontWeight = FontWeight.ExtraBold, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ShortsMenuOption(icon = Icons.Rounded.Settings, label = stringResource(R.string.video_quality_short), onClick = { onQualityClick(); onDismiss() })
                    if (hasValidAudioTracks) {
                        ShortsMenuOption(icon = Icons.Rounded.Audiotrack, label = stringResource(R.string.audio_quality_short), onClick = { onAudioClick(); onDismiss() })
                    }
                    ShortsMenuOption(icon = Icons.Rounded.Share, label = stringResource(R.string.share_short), onClick = { onShareClick(); onDismiss() })
                    if (!isIncognito) {
                        ShortsMenuOption(icon = Icons.Rounded.Check, label = stringResource(R.string.already_watched), onClick = { onMarkAsWatchedClick(); onDismiss() })
                        ShortsMenuOption(icon = Icons.Rounded.Block, label = stringResource(R.string.block_channel), onClick = { onBlockClick(); onDismiss() }, isError = true)
                    }
                }
            }
        }
    }
}

// Opciones individuales
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
        shape = RoundedCornerShape(16.dp),
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
                style = MaterialTheme.typography.bodyMedium, 
                fontWeight = FontWeight.Medium,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
