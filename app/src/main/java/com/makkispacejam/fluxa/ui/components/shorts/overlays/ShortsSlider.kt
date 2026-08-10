package com.makkispacejam.fluxa.ui.components.shorts.overlays

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.makkispacejam.fluxa.ui.components.player.shared.WavySlider

// Deslizador de barra de progreso shorts
@Composable
fun ShortsSlider(
    value: Float,
    bufferedValue: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    bottomNavPadding: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = bottomNavPadding + 10.dp)
            .height(20.dp)
            .zIndex(2f),
        contentAlignment = Alignment.Center
    ) {
        WavySlider(
            value = value,
            bufferedValue = bufferedValue,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
