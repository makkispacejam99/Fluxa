@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.video.video.sections

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.makkispacejam.fluxa.models.CommentThread
import com.makkispacejam.fluxa.video.shorts.TimeUtils
import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.utils.stripHtml

@Composable
fun CommentsPreview(
    comments: List<CommentThread>,
    isLoading: Boolean,
    getAvatar: (String, String?) -> String?,
    ownerChannelId: String = "",
    ownerAvatarUrl: String? = null,
    onShowAll: () -> Unit
) {
    val topComments = comments.take(3)

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.comments_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (comments.isNotEmpty()) {
                TextButton(onClick = onShowAll) {
                    Text(
                        text = stringResource(R.string.show_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp),
            onClick = onShowAll
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    topComments.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.comments_disabled_or_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                    else -> {
                        topComments.forEachIndexed { index, thread ->
                            val snippet = thread.snippet?.topLevelComment?.snippet
                            if (snippet != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    val isOwner = snippet.authorChannelId.isNotEmpty() && snippet.authorChannelId == ownerChannelId
                                    val avatar = getAvatar(snippet.authorChannelId, snippet.authorProfileImageUrl)
                                        ?.ifEmpty { if (isOwner) ownerAvatarUrl ?: "" else "" }
                                        ?: if (isOwner) ownerAvatarUrl ?: "" else ""
                                    com.makkispacejam.fluxa.ui.components.core.FluxaAvatar(
                                        avatarUrl = avatar,
                                        size = 32.dp,
                                        iconSize = 22.dp,
                                        placeholderName = snippet.authorDisplayName
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val displayName = snippet.authorDisplayName.replace("@", "").trim()
                                            Text(
                                                text = displayName.ifEmpty { stringResource(R.string.user_placeholder) },
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            
                                            val timeAgo = TimeUtils.getTimeAgo(
                                                snippet.publishedAt,
                                                recentText = stringResource(R.string.time_recent),
                                                minAgoText = stringResource(R.string.time_min_ago),
                                                hoursAgoText = stringResource(R.string.time_hours_ago),
                                                daysAgoText = stringResource(R.string.time_days_ago)
                                            )
                                            
                                            Text(
                                                text = "• $timeAgo",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        val commentText = snippet.textDisplay.ifEmpty { "" }.stripHtml()
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
                                                style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                                maxLines = 2,
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
                                                style = MaterialTheme.typography.bodyMedium,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                                if (index < topComments.size - 1) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}
