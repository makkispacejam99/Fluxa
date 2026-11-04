@file:Suppress("FunctionName")

package com.makkispacejam.fluxa.ui.components.system

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.makkispacejam.fluxa.R
import com.makkispacejam.fluxa.data.UserPreferences
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

private const val TICK_MS = 5000L
private const val BREAK_SNOOZE_MS = 10 * 60 * 1000L
private const val BEDTIME_SNOOZE_MS = 5 * 60 * 1000L
private const val BEDTIME_WINDOW_MS = 30 * 60 * 1000L
private const val OVERLAY_AUTO_HIDE_MS = 30 * 1000L
private const val BREAK_DEFER_MAX_MS = 10 * 60 * 1000L
private const val WELLBEING_NOTICE_OWNER = "wellbeing_notice"

fun formatWellbeingTime(context: Context, hour: Int, minute: Int): String {
    val calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return android.text.format.DateFormat.getTimeFormat(context).format(calendar.time)
}

private fun bedtimeWindowStart(nowMillis: Long, hour: Int, minute: Int): Pair<Long, String> {
    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val nowMinutes = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val targetMinutes = hour * 60 + minute
    if (nowMinutes < targetMinutes) calendar.add(Calendar.DAY_OF_YEAR, -1)
    calendar.set(Calendar.HOUR_OF_DAY, hour)
    calendar.set(Calendar.MINUTE, minute)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    val key = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    return calendar.timeInMillis to key
}

@Composable
fun WellbeingReminderHost(
    playerFullscreen: Boolean = false,
    playerPlaying: Boolean = false,
    onPausePlayback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val playbackBlocking by rememberUpdatedState(playerFullscreen && playerPlaying)
    val pausePlayback by rememberUpdatedState(onPausePlayback)

    var isForeground by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var breakVisible by remember { mutableStateOf(false) }
    var bedtimeVisible by remember { mutableStateOf(false) }
    var accumulatedMs by remember { mutableLongStateOf(prefs.breakReminderAccumulatedMs) }
    var intervalMinutes by remember { mutableIntStateOf(prefs.breakReminderIntervalMinutes) }
    var bedtimeHour by remember { mutableIntStateOf(prefs.bedtimeReminderHour) }
    var bedtimeMinute by remember { mutableIntStateOf(prefs.bedtimeReminderMinute) }
    var breakDeferredSinceMs by remember { mutableLongStateOf(0L) }
    var bedtimeDeferredKey by remember { mutableStateOf("") }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> isForeground = true
                Lifecycle.Event.ON_STOP -> {
                    isForeground = false
                    prefs.breakReminderAccumulatedMs = accumulatedMs
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            prefs.breakReminderAccumulatedMs = accumulatedMs
        }
    }

    LaunchedEffect(isForeground) {
        if (!isForeground) return@LaunchedEffect
        var lastTick = System.currentTimeMillis()
        while (true) {
            delay(TICK_MS.milliseconds)
            val now = System.currentTimeMillis()
            val elapsed = (now - lastTick).coerceAtLeast(0L)
            lastTick = now

            val breakEnabled = prefs.breakReminderEnabled
            val bedtimeEnabled = prefs.bedtimeReminderEnabled
            val newInterval = prefs.breakReminderIntervalMinutes
            if (newInterval != intervalMinutes) {
                intervalMinutes = newInterval
                accumulatedMs = 0L
            }
            bedtimeHour = prefs.bedtimeReminderHour
            bedtimeMinute = prefs.bedtimeReminderMinute

            if (!breakEnabled) {
                accumulatedMs = 0L
                breakDeferredSinceMs = 0L
            } else if (!breakVisible && !bedtimeVisible) {
                accumulatedMs += elapsed
                val breakDue = accumulatedMs >= intervalMinutes * 60_000L && now >= prefs.breakReminderSnoozeUntil
                if (breakDue) {
                    val deferredEnough = breakDeferredSinceMs > 0L &&
                        now - breakDeferredSinceMs >= BREAK_DEFER_MAX_MS
                    if (!playbackBlocking || deferredEnough) {
                        if (NoticeCoordinator.tryAcquire(WELLBEING_NOTICE_OWNER)) {
                            breakVisible = true
                            breakDeferredSinceMs = 0L
                        }
                    } else if (breakDeferredSinceMs == 0L) {
                        breakDeferredSinceMs = now
                    }
                } else {
                    breakDeferredSinceMs = 0L
                }
            }

            if (!bedtimeEnabled) {
                bedtimeDeferredKey = ""
            } else if (!breakVisible && !bedtimeVisible) {
                val snoozeUntil = prefs.bedtimeReminderSnoozeUntil
                if (snoozeUntil > 0L) {
                    if (now >= snoozeUntil && !playbackBlocking &&
                        NoticeCoordinator.tryAcquire(WELLBEING_NOTICE_OWNER)) {
                        prefs.bedtimeReminderSnoozeUntil = 0L
                        bedtimeVisible = true
                    }
                } else {
                    val (windowStart, windowKey) = bedtimeWindowStart(now, bedtimeHour, bedtimeMinute)
                    val inWindow = now >= windowStart && now - windowStart <= BEDTIME_WINDOW_MS
                    if (inWindow && prefs.bedtimeReminderLastShownDate != windowKey) {
                        if (!playbackBlocking && NoticeCoordinator.tryAcquire(WELLBEING_NOTICE_OWNER)) {
                            prefs.bedtimeReminderLastShownDate = windowKey
                            bedtimeVisible = true
                        } else if (playbackBlocking) {
                            bedtimeDeferredKey = windowKey
                        }
                    }
                    if (bedtimeDeferredKey.isNotEmpty() && !playbackBlocking &&
                        NoticeCoordinator.tryAcquire(WELLBEING_NOTICE_OWNER)) {
                        prefs.bedtimeReminderLastShownDate = bedtimeDeferredKey
                        bedtimeDeferredKey = ""
                        bedtimeVisible = true
                    }
                }
            }
        }
    }

    LaunchedEffect(breakVisible) {
        if (!breakVisible) {
            NoticeCoordinator.release(WELLBEING_NOTICE_OWNER)
            return@LaunchedEffect
        }
        delay(OVERLAY_AUTO_HIDE_MS.milliseconds)
        breakVisible = false
        accumulatedMs = 0L
        prefs.breakReminderAccumulatedMs = 0L
    }

    LaunchedEffect(bedtimeVisible) {
        if (!bedtimeVisible) {
            NoticeCoordinator.release(WELLBEING_NOTICE_OWNER)
            return@LaunchedEffect
        }
        delay(OVERLAY_AUTO_HIDE_MS.milliseconds)
        bedtimeVisible = false
    }

    LaunchedEffect(breakVisible, bedtimeVisible, playerPlaying) {
        if ((breakVisible || bedtimeVisible) && playerPlaying) pausePlayback()
    }

    FullScreenNoticeOverlay(
        visible = breakVisible,
        title = stringResource(R.string.break_reminder_title),
        message = stringResource(R.string.break_reminder_message),
        icon = Icons.Rounded.SelfImprovement,
        primaryLabel = stringResource(R.string.remind_later_10),
        onPrimary = {
            breakVisible = false
            prefs.breakReminderSnoozeUntil = System.currentTimeMillis() + BREAK_SNOOZE_MS
            prefs.breakReminderAccumulatedMs = accumulatedMs
        },
        secondaryLabel = stringResource(R.string.not_now),
        onSecondary = {
            breakVisible = false
            accumulatedMs = 0L
            prefs.breakReminderAccumulatedMs = 0L
            prefs.breakReminderSnoozeUntil = 0L
        },
        modifier = modifier
    )

    FullScreenNoticeOverlay(
        visible = bedtimeVisible,
        title = stringResource(R.string.bedtime_title),
        message = stringResource(
            R.string.bedtime_message,
            formatWellbeingTime(context, bedtimeHour, bedtimeMinute)
        ),
        icon = Icons.Rounded.Bedtime,
        primaryLabel = stringResource(R.string.bedtime_snooze),
        onPrimary = {
            bedtimeVisible = false
            prefs.bedtimeReminderSnoozeUntil = System.currentTimeMillis() + BEDTIME_SNOOZE_MS
        },
        secondaryLabel = stringResource(R.string.bedtime_dismiss),
        onSecondary = {
            bedtimeVisible = false
            prefs.bedtimeReminderSnoozeUntil = 0L
        },
        modifier = modifier
    )
}
