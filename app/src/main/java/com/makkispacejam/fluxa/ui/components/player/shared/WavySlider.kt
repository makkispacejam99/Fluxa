@file:Suppress("AssignedValueIsNeverRead")

package com.makkispacejam.fluxa.ui.components.player.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun WavySlider(
    modifier: Modifier = Modifier,
    value: Float,
    bufferedValue: Float = 0f,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    markers: List<Float> = emptyList(),
    hoverTitle: String? = null,
) {
    val primaryColor = MaterialTheme.colorScheme.primaryFixed
    val inactiveColor = Color.White.copy(alpha = 0.24f)
    val bufferedColor = Color.White.copy(alpha = 0.4f)

    val currentBuffered by rememberUpdatedState(bufferedValue)
    var stableBufferedValue by remember { mutableFloatStateOf(bufferedValue) }

    var containerWidth by remember { mutableIntStateOf(0) }
    var labelWidth by remember { mutableIntStateOf(0) }

    var wavePhase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (isActive) {
            withFrameNanos { frameTimeNanos ->
                wavePhase = frameTimeNanos / 1_000_000_000f * (2f * PI.toFloat())
            }
        }
    }

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

    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val displayFraction = if (dragging) dragFraction else value.coerceIn(0f, 1f)
    val displayBufferedX = maxOf(stableBufferedValue, displayFraction)

    Box(modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .onSizeChanged { containerWidth = it.width }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val downId = down.id
                        val downX = down.position.x
                        val touchSlop = viewConfiguration.touchSlop
                        var draggingThis = false

                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == downId }
                                    ?: break
                                if (!change.pressed) {
                                    if (draggingThis) {
                                        change.consume()
                                        dragging = false
                                        onValueChangeFinished()
                                    } else if (!change.isConsumed) {
                                        change.consume()
                                        val f = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                        onValueChange(f)
                                        onValueChangeFinished()
                                    }
                                    break
                                } else {
                                    if (!draggingThis && abs(change.position.x - downX) > touchSlop) {
                                        draggingThis = true
                                        dragging = true
                                        change.consume()
                                    }
                                    if (draggingThis) {
                                        val f = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                                        dragFraction = f
                                        onValueChange(f)
                                        change.consume()
                                    }
                                }
                            }
                        } finally {
                            if (dragging) dragging = false
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
                val width = size.width
                val height = size.height
                val centerY = height / 2

                val activeX = width * displayFraction
                val bufferedX = width * displayBufferedX

                val notchPx = 8.dp.toPx()
                val strokePx = 3.dp.toPx()
                val sortedMarkers = markers.sorted().filter { it in 0f..1f }

                fun drawSegmentedLine(startF: Float, endF: Float, color: Color) {
                    if (endF <= startF) return
                    var cursor = startF
                    for (m in sortedMarkers) {
                        if (m <= cursor || m >= endF) continue
                        val xFrom = width * cursor
                        val xTo = width * m - notchPx / 2f
                        if (xTo - xFrom >= 1f) {
                            drawLine(color, Offset(xFrom, centerY), Offset(xTo, centerY), strokePx, StrokeCap.Round)
                        }
                        cursor = m + (notchPx / 2f) / width
                    }
                    val xEnd = width * endF
                    val xFrom = width * cursor
                    if (xEnd - xFrom >= 1f) {
                        drawLine(color, Offset(xFrom, centerY), Offset(xEnd, centerY), strokePx, StrokeCap.Round)
                    }
                }

                drawSegmentedLine(displayFraction, 1f, inactiveColor)

                if (bufferedX > activeX) {
                    drawSegmentedLine(displayFraction, (bufferedX / width).coerceIn(0f, 1f), bufferedColor)
                }

                fun drawSegmentedWave(startF: Float, endF: Float, color: Color) {
                    if (endF <= startF) return
                    val amplitude = 5f + 1.2f * sin(wavePhase * 0.5f)
                    val waveLength = 18f

                    fun drawWaveSegment(xFrom: Float, xTo: Float) {
                        if (xTo - xFrom < 1f) return
                        val wavePath = Path().apply {
                            moveTo(xFrom, centerY + sin(xFrom / waveLength - wavePhase) * amplitude)
                            var x = xFrom
                            while (x <= xTo) {
                                val y = centerY + sin(x / waveLength - wavePhase) * amplitude
                                lineTo(x, y)
                                x += 2f
                            }
                            if (x - 2f < xTo) {
                                val y = centerY + sin(xTo / waveLength - wavePhase) * amplitude
                                lineTo(xTo, y)
                            }
                        }
                        drawPath(
                            path = wavePath,
                            color = color,
                            style = Stroke(width = strokePx, cap = StrokeCap.Round)
                        )
                    }

                    var cursor = startF
                    for (m in sortedMarkers) {
                        if (m <= cursor || m >= endF) continue
                        val xFrom = width * cursor
                        val xTo = width * m - notchPx / 2f
                        drawWaveSegment(xFrom, xTo)
                        cursor = m + (notchPx / 2f) / width
                    }
                    val xEnd = width * endF
                    val xFrom = width * cursor
                    drawWaveSegment(xFrom, xEnd)
                }

                if (activeX > 0f) {
                    drawSegmentedWave(0f, displayFraction, primaryColor)
                }

                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(activeX - 3.dp.toPx(), centerY - 7.dp.toPx()),
                    size = Size(6.dp.toPx(), 14.dp.toPx()),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
            }
        }

        if (hoverTitle != null && containerWidth > 0) {
            val thumbX = (containerWidth * displayFraction)
                .let { center ->
                    val w = labelWidth.coerceAtLeast(0)
                    (center - w / 2f).roundToInt().coerceIn(8, (containerWidth - w - 8).coerceAtLeast(8))
                }
            Text(
                text = hoverTitle,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .offset {
                        IntOffset(thumbX, (-50.dp.toPx()).roundToInt())
                    }
                    .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
                    .widthIn(max = 190.dp)
                    .onGloballyPositioned { labelWidth = it.size.width }
            )
        }
    }
}