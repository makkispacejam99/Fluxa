package com.makkispacejam.fluxa.ui.components.library.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.makkispacejam.fluxa.data.local.VideoInteractionEntity
import com.makkispacejam.fluxa.ui.components.core.TranslatedText
import com.makkispacejam.fluxa.utils.MusicChannelUtils
import com.makkispacejam.fluxa.utils.ThumbnailUtils

import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R
import java.util.Locale

// Apartado de historial
@Composable
fun HistorySection(
    history: List<VideoInteractionEntity>,
    onShowAll: () -> Unit,
    onVideoClick: (String, String, String, String) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.historial),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.ver_todo),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.clickable { onShowAll() }
            )
        }
        if (history.isEmpty()) {
            Text(
                stringResource(R.string.no_history_yet),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 12.dp)
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(top = 12.dp)
            ) {
                items(history.take(6)) { video ->
                    Column(
                        modifier = Modifier
                            .width(160.dp)
                            .clickable {
                                onVideoClick(
                                    video.title ?: "Sin título",
                                    video.videoId,
                                    video.channelName ?: "",
                                    ThumbnailUtils.getBestThumbnailUrl(video.videoId, null)
                                )
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.outlineVariant),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            AsyncImage(
                                model = ThumbnailUtils.getBestThumbnailUrl(video.videoId, null),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Barra de progreso
                            if (video.durationMs > 0) {
                                val progress = video.progressMs.toFloat() / video.durationMs.toFloat()
                                if (progress > 0.05f) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                                .fillMaxHeight()
                                                .background(MaterialTheme.colorScheme.primaryFixed)
                                        )
                                    }
                                }
                            }
                            
                            // Duración
                            if (video.durationMs > 0) {
                                val sec = video.durationMs / 1000
                                val min = sec / 60
                                val displayDur = if (min >= 60) {
                                    String.format(Locale.US, "%d:%02d:%02d", min / 60, min % 60, sec % 60)
                                } else {
                                    String.format(Locale.US, "%d:%02d", min, sec % 60)
                                }
                                Surface(
                                    color = Color.Black.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(4.dp),
                                    modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp)
                                ) {
                                    Text(
                                        text = displayDur,
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        TranslatedText(
                            text = video.title ?: "Sin título",
                            maxLines = 2,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            overflow = TextOverflow.Ellipsis,
                            skipTranslation = MusicChannelUtils.isMusicContent(video.channelName ?: "", video.title ?: "")
                        )
                    }
                }
            }
        }
    }
}
