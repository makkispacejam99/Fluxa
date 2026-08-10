package com.makkispacejam.fluxa.ui.components.shorts.overlays

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Comportamiento de botón de play y pause
@Composable
fun PlayPauseFeedback(
    isVisible: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(spring()) + scaleIn(
            initialScale = 0.7f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
        ),
        exit = fadeOut(spring()) + scaleOut(
            targetScale = 1.4f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        ),
        modifier = modifier
    ) {
        AnimatedContent(
            targetState = isPaused,
            transitionSpec = {
                (fadeIn(spring(stiffness = Spring.StiffnessMediumLow)) + scaleIn(
                    spring(dampingRatio = Spring.DampingRatioLowBouncy)
                )) togetherWith fadeOut(spring()) using SizeTransform(clip = false)
            },
            label = "playPauseFeedback"
        ) { paused ->
            if (paused) {
                Surface(
                    modifier = Modifier.size(82.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Black.copy(alpha = 0.96f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Pause,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.size(82.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.96f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }
}
