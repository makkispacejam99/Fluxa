package com.makkispacejam.fluxa.ui.components.core

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.ui.theme.LocalFluxaDesign
import com.makkispacejam.fluxa.utils.MusicChannelUtils
import com.makkispacejam.fluxa.utils.ThumbnailUtils

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

    val design = LocalFluxaDesign.current
    val cornerRadius = remember(design.ThumbnailCorner) {
        if (design.ThumbnailCorner.value > 0f) design.ThumbnailCorner else 20.dp
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

    Column(
        modifier = interactionModifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(cornerRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
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
                Text(
                    stringResource(R.string.thumbnail_label),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isWatched) {
                    Surface(
                        color = Color(0xCC000000),
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
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

                if (isLive) {
                    Surface(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color.White, CircleShape)
                            )
                            Text(
                                text = stringResource(R.string.live_badge),
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else if (duration.isNotEmpty() && duration != "00:00" && duration != "0:00") {
                    Surface(
                        color = Color(0xCC000000),
                        shape = CircleShape
                    ) {
                        Text(
                            text = duration,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .align(Alignment.BottomStart)
                        .background(Color.White.copy(alpha = 0.25f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            if (showCheckbox) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isItemSelected) Color.Black.copy(alpha = 0.35f) else Color.Transparent)
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

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            FluxaAvatar(
                avatarUrl = uploaderAvatarUrl,
                modifier = Modifier.clickable { onChannelClick(channel) },
                size = 40.dp,
                iconSize = 28.dp,
                placeholderName = channel,
                isLoading = isLoadingAvatar
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                TranslatedText(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    skipTranslation = MusicChannelUtils.isMusicContent(channel, title)
                )

                Spacer(modifier = Modifier.height(2.dp))

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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { onChannelClick(channel) }
                )
            }

            if (onOptionsClick != null) {
                IconButton(
                    onClick = onOptionsClick,
                    modifier = Modifier
                        .size(32.dp)
                        .padding(start = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.options_title),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
