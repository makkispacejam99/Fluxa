package com.makkispacejam.fluxa.ui.components.core

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.ui.theme.LocalFluxaDesign

// Motor de Animación
fun Modifier.shimmerEffect(): Modifier = composed {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val baseColor = MaterialTheme.colorScheme.surfaceVariant
    val isAmoled = baseColor == Color(0xFF000000)
    val skeletonBase = if (isAmoled) Color(0xFF1A1A1A) else baseColor
    val skeletonHigh = if (isDark) {
        lerp(skeletonBase, Color.White, 0.12f)
    } else {
        lerp(skeletonBase, Color.Black, 0.05f)
    }

    val transition = rememberInfiniteTransition(label = "fluxa_shimmer")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerPulse"
    )

    drawBehind {
        drawRect(color = lerp(skeletonBase, skeletonHigh, pulse))
    }
}

// Simula lineas de texto
@Composable
fun SkeletonLine(
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 16.dp,
    cornerRadius: androidx.compose.ui.unit.Dp = 6.dp
) {
    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .shimmerEffect()
    )
}

// Simula Carga de Avatar
@Composable
fun SkeletonAvatar(
    size: androidx.compose.ui.unit.Dp = 40.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .shimmerEffect()
    )
}

// Simula cargar la tarjeta de video
@Composable
fun VideoCardSkeleton() {
    val design = LocalFluxaDesign.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(design.ThumbnailCorner))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Top) {
            SkeletonAvatar(size = 40.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonLine(modifier = Modifier.fillMaxWidth(0.8f), height = 18.dp)
                Spacer(modifier = Modifier.height(8.dp))
                SkeletonLine(modifier = Modifier.fillMaxWidth(0.4f), height = 14.dp)
            }
        }
    }
}

// Simula la playlistCard
@Composable
fun PlaylistCardSkeleton() {
    val design = LocalFluxaDesign.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(180.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(design.ThumbnailCorner))
                    .background(Color.White.copy(alpha = 0.04f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(design.ThumbnailCorner))
                    .shimmerEffect()
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        SkeletonLine(modifier = Modifier.fillMaxWidth(0.6f), height = 18.dp)
    }
}