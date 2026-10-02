package com.makkispacejam.fluxa.ui.components.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.makkispacejam.fluxa.ui.components.core.SkeletonAvatar
import com.makkispacejam.fluxa.ui.components.core.SkeletonLine
import com.makkispacejam.fluxa.ui.components.core.shimmerEffect

@Composable
fun ShortSkeleton(bottomNavPadding: Dp = 0.dp) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp, bottom = bottomNavPadding + 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonAvatar(size = 38.dp)
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SkeletonLine(modifier = Modifier.fillMaxWidth(0.5f), height = 16.dp)
                    Spacer(modifier = Modifier.height(4.dp))
                    SkeletonLine(modifier = Modifier.fillMaxWidth(0.86f), height = 12.dp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .shimmerEffect()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmerEffect()
                )
            }
        }
    }
}
