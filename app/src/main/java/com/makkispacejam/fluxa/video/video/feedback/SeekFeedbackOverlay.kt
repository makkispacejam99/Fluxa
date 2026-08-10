package com.makkispacejam.fluxa.video.video.feedback

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BoxScope.SeekFeedbackOverlay(visible: Boolean, isForward: Boolean, amount: Int, isFullScreen: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (visible) 150 else 500,
            easing = FastOutSlowInEasing
        ),
        label = "seekAlpha"
    )

    val translationX by animateFloatAsState(
        targetValue = if (visible) 0f else (if (isForward) 40f else -40f),
        animationSpec = if (visible) spring(dampingRatio = Spring.DampingRatioMediumBouncy) else tween(500),
        label = "seekTranslation"
    )

    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.85f,
        animationSpec = if (visible) spring(dampingRatio = Spring.DampingRatioMediumBouncy) else tween(500),
        label = "seekScale"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(0.42f)
            .align(if (isForward) Alignment.CenterEnd else Alignment.CenterStart)
            .graphicsLayer { this.alpha = alpha }
            .background(
                brush = Brush.horizontalGradient(
                    colors = if (isForward) {
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                    } else {
                        listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)
                    }
                )
            ),
        contentAlignment = if (isForward) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(horizontal = if (isFullScreen) 60.dp else 28.dp)
                .graphicsLayer {
                    this.translationX = translationX
                    this.scaleX = scale
                    this.scaleY = scale
                }
        ) {
            Icon(
                imageVector = if (isForward) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(if (isFullScreen) 32.dp else 22.dp)
            )
            Text(
                text = "${if (isForward) "+" else "-"}${amount}s",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                style = if (isFullScreen) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge
            )
        }
    }
}
