@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.video.shorts

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.models.CommentSnippet
import com.makkispacejam.fluxa.models.CommentThread
import com.makkispacejam.fluxa.utils.stripHtml

import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

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
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.68f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.comments_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (comments.isNotEmpty()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = comments.size.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
                FilledTonalIconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.close_btn),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (comments.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.comments_disabled_or_unavailable),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
                                .drawWithContent {
                                    drawContent()
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            0.94f to Color.Black,
                                            1f to Color.Transparent
                                        ),
                                        blendMode = BlendMode.DstIn
                                    )
                                },
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            itemsIndexed(comments) { index, thread ->
                                val mainComment = thread.snippet?.topLevelComment?.snippet
                                if (mainComment != null) {
                                    if (index > 0) {
                                        HorizontalDivider(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                        )
                                    }
                                    CommentRow(
                                        comment = mainComment,
                                        getAvatar = getAvatar,
                                        ownerChannelId = ownerChannelId,
                                        ownerAvatarUrl = ownerAvatarUrl,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )

                                    thread.replies?.comments?.forEach { replyItem ->
                                        val replySnippet = replyItem.snippet
                                        if (replySnippet != null) {
                                            HorizontalDivider(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 10.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                            )
                                            CommentRow(
                                                comment = replySnippet,
                                                isReply = true,
                                                getAvatar = getAvatar,
                                                ownerChannelId = ownerChannelId,
                                                ownerAvatarUrl = ownerAvatarUrl,
                                                modifier = Modifier.padding(start = 36.dp, end = 16.dp)
                                            )
                                        }
                                    }
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
fun CommentRow(
    comment: CommentSnippet,
    modifier: Modifier = Modifier,
    isReply: Boolean = false,
    getAvatar: (String, String?) -> String?,
    ownerChannelId: String = "",
    ownerAvatarUrl: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val displayName = comment.authorDisplayName.replace("@", "").trim()
                Text(
                    text = displayName.ifEmpty { stringResource(R.string.user_placeholder) },
                    style = if (isReply) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            val commentText = comment.textDisplay.ifEmpty { "" }.stripHtml()
            val urlMatches = Regex("https?://\\S+").findAll(commentText).toList()
            if (urlMatches.isNotEmpty()) {
                val context = LocalContext.current
                val primaryColor = MaterialTheme.colorScheme.primary
                val annotatedString = buildAnnotatedString {
                    var lastIndex = 0
                    urlMatches.forEach { match ->
                        val start = match.range.first
                        val end = match.range.last + 1
                        append(commentText.substring(lastIndex, start))
                        pushStringAnnotation(tag = "URL", annotation = commentText.substring(start, end))
                        withStyle(SpanStyle(color = primaryColor, textDecoration = TextDecoration.Underline)) {
                            append(commentText.substring(start, end))
                        }
                        pop()
                        lastIndex = end
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
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = comment.likeCount.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
