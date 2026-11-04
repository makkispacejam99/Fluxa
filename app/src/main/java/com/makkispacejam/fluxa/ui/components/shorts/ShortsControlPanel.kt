@file:Suppress("FunctionName")

package com.makkispacejam.fluxa.ui.components.shorts

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.ui.components.core.SkeletonBar
import kotlinx.coroutines.launch

private fun formatShortCount(count: Long): String = when {
    count >= 1_000_000_000 -> "${count / 1_000_000_000}B"
    count >= 1_000_000 -> "${count / 1_000_000}M"
    count >= 1_000 -> "${count / 1_000}K"
    else -> count.toString()
}

@Composable
fun ShortsControlPanel(
    modifier: Modifier = Modifier,
    isLiked: Boolean,
    isDisliked: Boolean,
    likeCount: Long = -1L,
    dislikeCount: Long = -1L,
    isStatsLoading: Boolean = false,
    onCommentsClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDislikeClick: () -> Unit,
    onMoreClick: () -> Unit,
    isIncognito: Boolean = false,
    onDisabledClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.70f),
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .wrapContentWidth(),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PillIconButton(
                icon = Icons.Rounded.Favorite,
                isActivated = isLiked,
                activeColor = MaterialTheme.colorScheme.primaryFixed,
                label = if (likeCount >= 0L) formatShortCount(likeCount + if (isLiked) 1L else 0L) else null,
                loading = isStatsLoading && likeCount < 0L,
                enabled = !isIncognito,
                onClick = onLikeClick,
                onDisabledClick = onDisabledClick
            )
            PillIconButton(
                icon = Icons.AutoMirrored.Rounded.Comment,
                isActivated = false,
                activeColor = Color.White,
                onClick = onCommentsClick
            )
            PillIconButton(
                icon = Icons.Rounded.ThumbDown,
                isActivated = isDisliked,
                activeColor = MaterialTheme.colorScheme.primaryFixed,
                label = if (dislikeCount >= 0L) formatShortCount(dislikeCount + if (isDisliked) 1L else 0L) else null,
                enabled = !isIncognito,
                onClick = onDislikeClick,
                onDisabledClick = onDisabledClick
            )
            PillIconButton(
                icon = Icons.Rounded.MoreHoriz,
                isActivated = false,
                activeColor = Color.White,
                onClick = onMoreClick
            )
        }
    }
}

@Composable
fun ShufflePill(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }

    Surface(
        onClick = {
            scope.launch {
                rotation.snapTo(0f)
                rotation.animateTo(360f, animationSpec = tween(450))
                onClick()
            }
        },
        modifier = modifier.height(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Autorenew,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer(rotationZ = rotation.value)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PillIconButton(
    icon: ImageVector,
    isActivated: Boolean,
    activeColor: Color,
    label: String? = null,
    loading: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    onDisabledClick: () -> Unit = {}
) {
    val tint = if (!enabled) {
        Color.White.copy(alpha = 0.35f)
    } else if (isActivated) activeColor else Color.White

    Row(
        modifier = Modifier
            .defaultMinSize(minWidth = 36.dp, minHeight = 36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button
            ) {
                if (!enabled) onDisabledClick() else onClick()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = tint
        )
        if (loading) {
            SkeletonBar(width = 18.dp, height = 10.dp, color = Color.White)
        } else if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tint,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
