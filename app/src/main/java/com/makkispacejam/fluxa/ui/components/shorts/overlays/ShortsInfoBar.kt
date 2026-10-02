package com.makkispacejam.fluxa.ui.components.shorts.overlays

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.components.core.FluxaAvatar
import com.makkispacejam.fluxa.ui.components.core.TranslatedText
import com.makkispacejam.fluxa.utils.MusicChannelUtils

@Composable
fun ShortsInfoBar(
    modifier: Modifier = Modifier,
    channelName: String,
    channelAvatarUrl: String?,
    onChannelClick: (String) -> Unit,
    isSubscribed: Boolean = false,
    onSubscribeClick: () -> Unit = {},
    isIncognito: Boolean = false,
    onDisabledClick: () -> Unit = {},
    title: String = "",
    isLoadingAvatar: Boolean = false,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = true)
        ) {
            FluxaAvatar(
                avatarUrl = channelAvatarUrl,
                modifier = Modifier.clickable { onChannelClick(channelName) },
                size = 38.dp,
                iconSize = 26.dp,
                placeholderName = channelName,
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                isLoading = isLoadingAvatar
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f, fill = true)
            ) {
                Text(
                    text = channelName,
                    color = textColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (title.isNotBlank()) {
                    TranslatedText(
                        text = title,
                        style = MaterialTheme.typography.bodySmall.copy(color = textColor),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        skipTranslation = MusicChannelUtils.isMusicContent(channelName, title)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = { if (isIncognito) onDisabledClick() else onSubscribeClick() },
            enabled = !isIncognito,
            shape = RoundedCornerShape(24.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
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
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
