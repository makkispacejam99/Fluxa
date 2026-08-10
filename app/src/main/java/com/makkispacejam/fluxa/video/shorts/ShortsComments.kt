@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.video.shorts

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.makkispacejam.fluxa.models.CommentSnippet
import com.makkispacejam.fluxa.models.CommentThread
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.utils.stripHtml
import com.makkispacejam.fluxa.video.video.sections.extractUrls

object TimeUtils {
    fun getTimeAgo(
        isoDate: String,
        recentText: String,
        minAgoText: String,
        hoursAgoText: String,
        daysAgoText: String
    ): String {
        if (isoDate.isEmpty()) return recentText
        if (!isoDate.contains("T") && isoDate.contains(" ")) return isoDate
        
        try {
            val past = ZonedDateTime.parse(isoDate)
            val now = ZonedDateTime.now()
            val hours = ChronoUnit.HOURS.between(past, now)
            val days = ChronoUnit.DAYS.between(past, now)

            return when {
                hours < 1 -> minAgoText
                hours < 24 -> hoursAgoText.format(hours)
                else -> daysAgoText.format(days)
            }
        } catch (_: Exception) {
            return isoDate.ifEmpty { recentText }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsComments(
    comments: List<CommentThread>,
    isLoading: Boolean,
    getAvatar: (String, String?) -> String?,
    ownerChannelId: String = "",
    ownerAvatarUrl: String? = null,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 14.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.64f)
                .padding(horizontal = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.comments_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close_btn),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (comments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.comments_disabled_or_unavailable),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                        .drawWithContent {
                            drawContent()
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0.9f to Color.Black,
                                    1f to Color.Transparent
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        },
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(comments) { thread ->
                        val mainComment = thread.snippet?.topLevelComment?.snippet
                        if (mainComment != null) {
                            CommentRow(comment = mainComment, getAvatar = getAvatar, ownerChannelId = ownerChannelId, ownerAvatarUrl = ownerAvatarUrl)
                        }

                        thread.replies?.comments?.forEach { replyItem ->
                            val replySnippet = replyItem.snippet
                            if (replySnippet != null) {
                                Row(modifier = Modifier.padding(start = 44.dp, top = 8.dp)) {
                                    CommentRow(comment = replySnippet, isReply = true, getAvatar = getAvatar, ownerChannelId = ownerChannelId, ownerAvatarUrl = ownerAvatarUrl)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommentRow(comment: CommentSnippet, isReply: Boolean = false, getAvatar: (String, String?) -> String?, ownerChannelId: String = "", ownerAvatarUrl: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        val isOwner = comment.authorChannelId.isNotEmpty() && comment.authorChannelId == ownerChannelId
        val avatar = getAvatar(comment.authorChannelId, comment.authorProfileImageUrl)
            ?.ifEmpty { if (isOwner) ownerAvatarUrl ?: "" else "" }
            ?: if (isOwner) ownerAvatarUrl ?: "" else ""
        
        com.makkispacejam.fluxa.ui.components.core.FluxaAvatar(
            avatarUrl = avatar,
            size = if (isReply) 28.dp else 36.dp,
            iconSize = if (isReply) 18.dp else 24.dp,
            placeholderName = comment.authorDisplayName
        )

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                    val displayName = comment.authorDisplayName.replace("@", "").trim()
                    Text(
                        text = displayName.ifEmpty { stringResource(R.string.user_placeholder) },
                        style = if (isReply) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val timeAgo = TimeUtils.getTimeAgo(
                        comment.publishedAt,
                        recentText = stringResource(R.string.time_recent),
                        minAgoText = stringResource(R.string.time_min_ago),
                        hoursAgoText = stringResource(R.string.time_hours_ago),
                        daysAgoText = stringResource(R.string.time_days_ago)
                    )
                    
                    Text(
                        text = "• $timeAgo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
            }

            Spacer(modifier = Modifier.height(2.dp))

            val commentText = comment.textDisplay.ifEmpty { "" }.stripHtml()
            val urls = extractUrls(commentText)
            if (urls.isNotEmpty()) {
                val context = LocalContext.current
                val annotatedString = buildAnnotatedString {
                    var lastIndex = 0
                    urls.forEach { range ->
                        append(commentText.substring(lastIndex, range.first))
                        pushStringAnnotation(tag = "URL", annotation = commentText.substring(range.first, range.second))
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                            append(commentText.substring(range.first, range.second))
                        }
                        pop()
                        lastIndex = range.second
                    }
                    append(commentText.substring(lastIndex))
                }
                ClickableText(
                    text = annotatedString,
                    style = (if (isReply) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium).copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    overflow = TextOverflow.Ellipsis,
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset).firstOrNull()?.let {
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, it.item.toUri())) } catch (_: Exception) {}
                        }
                    }
                )
            } else {
                Text(
                    text = commentText,
                    style = (if (isReply) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium).copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            if (comment.likeCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = comment.likeCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
