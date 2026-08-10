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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

import androidx.compose.ui.res.stringResource
import com.makkispacejam.fluxa.R

// Panel lateral de shorts
@Composable
fun ShortsControlPanel(
    isLiked: Boolean,
    isDisliked: Boolean,
    onShuffleClick: () -> Unit,
    onCommentsClick: () -> Unit,
    onLikeClick: () -> Unit,
    onDislikeClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShortInteractionButton(icon = Icons.Rounded.Casino, label = stringResource(R.string.shuffle), isActivated = false, activeColor = Color.White, onActivateClick = onShuffleClick)
        ShortInteractionButton(icon = Icons.AutoMirrored.Rounded.Comment, label = stringResource(R.string.comments_title), isActivated = false, activeColor = Color.White, onActivateClick = onCommentsClick)
        ShortInteractionButton(icon = Icons.Rounded.Favorite, label = stringResource(R.string.me_gusta), isActivated = isLiked, activeColor = MaterialTheme.colorScheme.primaryFixed, onActivateClick = onLikeClick)
        ShortInteractionButton(icon = Icons.Rounded.ThumbDown, label = stringResource(R.string.no_me_gusta), isActivated = isDisliked, activeColor = MaterialTheme.colorScheme.primaryFixed, onActivateClick = onDislikeClick)
        ShortInteractionButton(icon = Icons.Rounded.MoreHoriz, label = stringResource(R.string.more_options), isActivated = false, activeColor = Color.White, onActivateClick = onMoreClick)
    }
}

// Interacción de botones
@Composable
fun ShortInteractionButton(
    icon: ImageVector,
    label: String,
    isActivated: Boolean,
    activeColor: Color,
    onActivateClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(
            modifier = Modifier
                .size(46.dp)
                .graphicsLayer(rotationZ = rotation.value),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.30f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Button
                    ) {
                        if (icon == Icons.Rounded.Casino) {
                            scope.launch {
                                rotation.snapTo(0f)
                                rotation.animateTo(360f, animationSpec = tween(400))
                                onActivateClick()
                            }
                        } else onActivateClick()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(24.dp),
                    tint = if (isActivated) activeColor else Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.45f),
                    offset = Offset(0f, 3f),
                    blurRadius = 10f
                )
            )
        )
    }
}
