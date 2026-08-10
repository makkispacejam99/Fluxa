package com.makkispacejam.fluxa.ui.components.shorts.overlays

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.ui.components.core.FluxaAvatar
import com.makkispacejam.fluxa.ui.components.core.TranslatedText
import com.makkispacejam.fluxa.utils.MusicChannelUtils

// Información inferior de canal y video
@Composable
fun ShortsInfoBar(
    channelName: String,
    channelAvatarUrl: String?,
    title: String,
    onChannelClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    isLoadingAvatar: Boolean = false
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FluxaAvatar(
                avatarUrl = channelAvatarUrl,
                modifier = Modifier.clickable { onChannelClick(channelName) },
                size = 36.dp,
                iconSize = 26.dp,
                placeholderName = channelName,
                backgroundColor = Color.DarkGray,
                iconColor = Color.LightGray,
                isLoading = isLoadingAvatar
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = channelName,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        TranslatedText(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(color = Color.White.copy(alpha = 0.90f)),
            maxLines = 2,
            skipTranslation = MusicChannelUtils.isMusicContent(channelName, title)
        )
    }
}
