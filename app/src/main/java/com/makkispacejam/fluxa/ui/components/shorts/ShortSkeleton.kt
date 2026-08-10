package com.makkispacejam.fluxa.ui.components.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.ui.components.core.SkeletonAvatar
import com.makkispacejam.fluxa.ui.components.core.SkeletonLine
import com.makkispacejam.fluxa.ui.components.core.shimmerEffect

// Esqueleto de shorts
@Composable
fun ShortSkeleton(bottomNavPadding: Dp = 0.dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomNavPadding)
                .shimmerEffect()
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = bottomNavPadding)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.7f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.95f)
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = bottomNavPadding + 32.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonAvatar(size = 36.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    SkeletonLine(modifier = Modifier.width(110.dp), height = 14.dp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                SkeletonLine(modifier = Modifier.fillMaxWidth(0.75f), height = 12.dp)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(5) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .shimmerEffect()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        SkeletonLine(modifier = Modifier.width(28.dp), height = 8.dp, cornerRadius = 4.dp)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 20.dp, vertical = bottomNavPadding + 10.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .shimmerEffect()
        )
    }
}
