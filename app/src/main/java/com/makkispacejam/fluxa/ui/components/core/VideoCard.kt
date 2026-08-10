package com.makkispacejam.fluxa.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.utils.MusicChannelUtils
import com.makkispacejam.fluxa.utils.ThumbnailUtils

// Tarjeta de video
@Composable
fun VideoCard(
    title: String,
    channel: String,
    views: String,
    duration: String,
    thumbnailUrl: String,
    uploaderAvatarUrl: String = "",
    progress: Float = 0f,
    publishedTime: String = "",
    isWatched: Boolean = false,
    isLive: Boolean = false,
    onOptionsClick: (() -> Unit)? = null,
    onVideoClick: (String, String) -> Unit,
    onChannelClick: (String) -> Unit = {},
    showCheckbox: Boolean = false,
    isItemSelected: Boolean = false,
    isLoadingAvatar: Boolean = false,
    onToggleSelection: () -> Unit = {}
) {
    val safeThumbnail = remember(thumbnailUrl) {
        ThumbnailUtils.getHighQualityThumbnail(thumbnailUrl)
    }

    val interactionModifier = if (showCheckbox) {
        Modifier
            .fillMaxWidth()
            .clickable { onToggleSelection() }
    } else {
        Modifier
            .fillMaxWidth()
            .clickable { onVideoClick(title, channel) }
    }

    Column(modifier = interactionModifier.padding(16.dp)) {

        // Thumbnail
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (safeThumbnail.isNotEmpty()) {
                AsyncImage(
                    model = safeThumbnail,
                    contentDescription = stringResource(R.string.thumbnail_desc),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(stringResource(R.string.thumbnail_label), color = MaterialTheme.colorScheme.primary)
            }

            // Tarjeta de duración y visto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isWatched) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = stringResource(R.string.watched_badge),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (duration.isNotEmpty() && duration != "00:00" && duration != "0:00") {
                    Surface(
                        color = Color.Black.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = duration,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            // Barra de progreso
            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primaryFixed)
                    )
                }
            }

            // Checkbox para seleccionar que videos eliminar
            if (showCheckbox) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isItemSelected) Color.Black.copy(alpha = 0.3f) else Color.Transparent)
                )
                Checkbox(
                    checked = isItemSelected,
                    onCheckedChange = { onToggleSelection() },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = Color.White
                    )
                )
            }
        }

        // Metadatos y avatar
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            FluxaAvatar(
                avatarUrl = uploaderAvatarUrl,
                modifier = Modifier.clickable { onChannelClick(channel) },
                size = 40.dp,
                iconSize = 30.dp,
                placeholderName = channel,
                isLoading = isLoadingAvatar
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Título, canal y vistas
            Column(modifier = Modifier.weight(1f)) {
                TranslatedText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    skipTranslation = MusicChannelUtils.isMusicContent(channel, title)
                )

                val metaText = remember(views, publishedTime) {
                    val base = views.takeIf { it.isNotEmpty() } ?: ""
                    if (publishedTime.isNotEmpty()) {
                        if (base.isNotEmpty()) "$base • $publishedTime" else publishedTime
                    } else base
                }

                val liveBadge = stringResource(R.string.live_badge)
                val liveColor = MaterialTheme.colorScheme.error
                val metaContent = remember(metaText, isLive, liveBadge, liveColor) {
                    buildAnnotatedString {
                        append(channel)
                        if (metaText.isNotEmpty()) append(" • $metaText")
                        if (isLive) {
                            append(" • ")
                            withStyle(SpanStyle(color = liveColor, fontWeight = FontWeight.Bold)) {
                                append(liveBadge)
                            }
                        }
                    }
                }

                Text(
                    text = metaContent,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable { onChannelClick(channel) }
                )
            }

            // Botón de opciones
            if (onOptionsClick != null) {
                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.options_title),
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}