@file:Suppress("FunctionName")

package com.makkispacejam.fluxa.ui.components.settings.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.makkispacejam.fluxa.R
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val MaxBreakHours = 5
private const val BreakMinuteStep = 5
private val WheelItemHeight = 40.dp
private val WheelHeight = 200.dp
private val WheelItemPadding = (WheelHeight - WheelItemHeight) / 2

@Composable
fun formatBreakIntervalText(minutes: Int): String {
    val hours = minutes / 60
    val remaining = minutes % 60
    return when {
        hours <= 0 -> stringResource(R.string.break_reminder_interval, remaining)
        remaining == 0 -> stringResource(R.string.break_reminder_interval_hours, hours)
        else -> stringResource(R.string.break_reminder_interval_hours_minutes, hours, remaining)
    }
}

@Composable
fun BreakIntervalDialog(
    initialMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val hourOptions = remember { (0..MaxBreakHours).toList() }
    val minuteOptions = remember { (0 until 60 step BreakMinuteStep).toList() }

    val initialHourIndex = hourOptions.indexOf((initialMinutes / 60).coerceIn(0, MaxBreakHours))
    val initialMinuteIndex = minuteOptions.indexOf(
        ((initialMinutes % 60).coerceIn(0, 59) / BreakMinuteStep) * BreakMinuteStep
    )

    val hourState = rememberLazyListState(initialFirstVisibleItemIndex = initialHourIndex.coerceAtLeast(0))
    val minuteState = rememberLazyListState(initialFirstVisibleItemIndex = initialMinuteIndex.coerceAtLeast(0))

    val selectedHour by remember(hourState) {
        derivedStateOf {
            if (hourState.layoutInfo.visibleItemsInfo.isEmpty()) {
                initialHourIndex.coerceAtLeast(0)
            } else {
                centeredIndex(hourState).coerceIn(hourOptions.indices)
            }
        }
    }
    val selectedMinute by remember(minuteState) {
        derivedStateOf {
            if (minuteState.layoutInfo.visibleItemsInfo.isEmpty()) {
                initialMinuteIndex.coerceAtLeast(0)
            } else {
                centeredIndex(minuteState).coerceIn(minuteOptions.indices)
            }
        }
    }

    val totalMinutes = hourOptions[selectedHour] * 60 + minuteOptions[selectedMinute]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.break_reminder_interval_label)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    WheelPicker(
                        values = hourOptions,
                        state = hourState,
                        isHour = true,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(20.dp)
                    )
                    WheelPicker(
                        values = minuteOptions,
                        state = minuteState,
                        isHour = false,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (totalMinutes < 5) {
                        stringResource(R.string.break_reminder_interval_min)
                    } else {
                        formatBreakIntervalText(totalMinutes)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (totalMinutes < 5) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(totalMinutes) },
                enabled = totalMinutes >= 5
            ) {
                Text(stringResource(R.string.accept_btn))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel_btn))
            }
        }
    )
}

@Composable
private fun WheelPicker(
    values: List<Int>,
    state: LazyListState,
    isHour: Boolean,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val itemHeightPx = with(LocalDensity.current) { WheelItemHeight.toPx() }
    val snapProvider = remember(state) { SnapLayoutInfoProvider(state) }
    val flingBehavior = rememberSnapFlingBehavior(snapProvider)

    Box(
        modifier = modifier.height(WheelHeight),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelItemHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        )
        LazyColumn(
            state = state,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = WheelItemPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .height(WheelHeight)
        ) {
            items(values.size) { index ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WheelItemHeight)
                        .clickable {
                            scope.launch { state.animateScrollToItem(index) }
                        }
                        .graphicsLayer {
                            val info = state.layoutInfo
                            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                            val distance = if (item != null) {
                                abs(item.offset + item.size / 2f - center)
                            } else {
                                itemHeightPx * 2f
                            }
                            val fraction = (distance / (itemHeightPx * 1.5f)).coerceIn(0f, 1f)
                            alpha = 1f - fraction * 0.65f
                            val scale = 1f - fraction * 0.18f
                            scaleX = scale
                            scaleY = scale
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isHour) {
                            stringResource(R.string.break_interval_hours_value, values[index])
                        } else {
                            stringResource(R.string.break_interval_minutes_value, values[index])
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

private fun centeredIndex(state: LazyListState): Int {
    val info = state.layoutInfo
    if (info.visibleItemsInfo.isEmpty()) return 0
    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
    return info.visibleItemsInfo.minByOrNull {
        abs(it.offset + it.size / 2 - center)
    }?.index ?: 0
}
