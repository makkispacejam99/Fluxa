package com.makkispacejam.fluxa.video.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.makkispacejam.fluxa.ui.components.core.TranslatedText
import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R

// Tarjeta de canal
@Composable
fun ChannelSubscriptionCard(
    channelName: String,
    isSubscribed: Boolean,
    channelAvatarUrl: String? = null,
    subscriberCount: String = "",
    onSubscribeClick: () -> Unit,
    onChannelClick: (String) -> Unit = {},
    isIncognito: Boolean = false,
    onDisabledClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onChannelClick(channelName) }
                    .padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Avatar del canal
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                        .clip(CircleShape)
                ){
                    if (!channelAvatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = channelAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    TranslatedText(text = channelName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    if (subscriberCount.isNotBlank()) {
                        Text(
                            subscriberCount,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Botón de suscribirse
            Button(
                onClick = { if (isIncognito) onDisabledClick() else onSubscribeClick() },
                enabled = !isIncognito,
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIncognito) {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    } else if (isSubscribed) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (isIncognito) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    } else if (isSubscribed) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    }
                )
            ) {
                Text(
                    text = if (isSubscribed) stringResource(R.string.subscribed) else stringResource(R.string.subscribe),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}