package com.makkispacejam.fluxa.ui.components.player.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.sin

// Barra de progreso "wavy" en los reproductores
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WavySlider(
    modifier: Modifier = Modifier,
    value: Float,
    bufferedValue: Float = 0f,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
) {
    val primaryColor = MaterialTheme.colorScheme.primaryFixed
    val inactiveColor = Color.White.copy(alpha = 0.24f)
    val bufferedColor = Color.White.copy(alpha = 0.4f)

    val currentBuffered by rememberUpdatedState(bufferedValue)
    var stableBufferedValue by remember { mutableFloatStateOf(bufferedValue) }

    LaunchedEffect(bufferedValue) {
        if (bufferedValue < stableBufferedValue - 0.05f) {
            stableBufferedValue = bufferedValue
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(3500)
            if (currentBuffered > stableBufferedValue) {
                stableBufferedValue = currentBuffered
            }
        }
    }

    val displayBufferedX = maxOf(stableBufferedValue, value)

    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        modifier = modifier,
        track = { sliderState ->
            Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                val width = size.width
                val height = size.height
                val centerY = height / 2

                val fraction = sliderState.value / sliderState.valueRange.endInclusive
                val activeX = width * fraction
                val bufferedX = width * displayBufferedX

                // Fondo base
                drawLine(
                    color = inactiveColor,
                    start = Offset(activeX, centerY),
                    end = Offset(width, centerY),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Barra de buffering
                if (bufferedX > activeX) {
                    drawLine(
                        color = bufferedColor,
                        start = Offset(activeX, centerY),
                        end = Offset(bufferedX.coerceAtMost(width), centerY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Wavy path activo
                if (activeX > 0f) {
                    val wavePath = Path().apply {
                        moveTo(0f, centerY)
                        val waveLength = 18f
                        val amplitude = 5f
                        var x = 0f
                        while (x <= activeX) {
                            val y = centerY + (sin(x / waveLength) * amplitude)
                            lineTo(x, y)
                            x += 2f
                        }
                    }
                    drawPath(
                        path = wavePath,
                        color = primaryColor,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        },
        thumb = {
            Box(modifier = Modifier.size(width = 6.dp, height = 14.dp).background(Color.White, shape = CircleShape))
        }
    )
}

