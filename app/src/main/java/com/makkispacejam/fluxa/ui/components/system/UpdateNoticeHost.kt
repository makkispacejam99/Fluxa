@file:Suppress("FunctionName")

package com.makkispacejam.fluxa.ui.components.system

import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.makkispacejam.fluxa.data.updates.UpdateCheckResult
import com.makkispacejam.fluxa.data.updates.UpdateChecker
import com.makkispacejam.fluxa.data.updates.UpdateInfo
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

private const val UPDATE_CHECK_INTERVAL_MS = 24 * 60 * 60 * 1000L
private const val UPDATE_CHECK_START_DELAY_MS = 1500L
private const val UPDATE_OVERLAY_AUTO_HIDE_MS = 30 * 1000L
private const val UPDATE_NOTICE_OWNER = "update_notice"

object UpdateNoticeController {
    var pending by mutableStateOf<UpdateInfo?>(null)
        private set

    fun show(info: UpdateInfo) {
        pending = info
    }

    fun consume() {
        pending = null
    }
}

@Composable
fun UpdateNoticeHost(
    playerFullscreen: Boolean = false,
    playerPlaying: Boolean = false,
    onPausePlayback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember { UserPreferences(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val pending = UpdateNoticeController.pending
    val activeOwner = NoticeCoordinator.activeOwner
    val playbackBlocking = playerFullscreen && playerPlaying
    val pausePlayback by rememberUpdatedState(onPausePlayback)

    var isForeground by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
    }
    var visible by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<UpdateInfo?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> isForeground = true
                Lifecycle.Event.ON_STOP -> isForeground = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (visible) NoticeCoordinator.release(UPDATE_NOTICE_OWNER)
        }
    }

    LaunchedEffect(isForeground) {
        if (!isForeground) return@LaunchedEffect
        if (!prefs.updateNoticeEnabled) return@LaunchedEffect
        val now = System.currentTimeMillis()
        if (now - prefs.lastUpdateCheckMs < UPDATE_CHECK_INTERVAL_MS) return@LaunchedEffect
        delay(UPDATE_CHECK_START_DELAY_MS.milliseconds)
        val result = UpdateChecker.check(context)
        prefs.lastUpdateCheckMs = System.currentTimeMillis()
        if (result is UpdateCheckResult.Available && prefs.lastNotifiedUpdateVersion != result.info.version) {
            UpdateNoticeController.show(result.info)
        }
    }

    LaunchedEffect(pending, activeOwner, playbackBlocking) {
        if (playbackBlocking) return@LaunchedEffect
        val info = pending ?: return@LaunchedEffect
        if (visible) return@LaunchedEffect
        if (!NoticeCoordinator.tryAcquire(UPDATE_NOTICE_OWNER)) return@LaunchedEffect
        UpdateNoticeController.consume()
        current = info
        visible = true
    }

    val dismiss: () -> Unit = {
        visible = false
        UpdateNoticeController.consume()
        current?.let { prefs.lastNotifiedUpdateVersion = it.version }
        NoticeCoordinator.release(UPDATE_NOTICE_OWNER)
    }

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        delay(UPDATE_OVERLAY_AUTO_HIDE_MS.milliseconds)
        dismiss()
    }

    LaunchedEffect(visible, playerPlaying) {
        if (visible && playerPlaying) pausePlayback()
    }

    val info = current

    FullScreenNoticeOverlay(
        visible = visible,
        title = stringResource(R.string.update_notice_title),
        message = stringResource(R.string.update_notice_message, info?.version ?: ""),
        icon = Icons.Rounded.SystemUpdate,
        primaryLabel = stringResource(R.string.update_notice_primary),
        onPrimary = {
            val url = info?.url
            dismiss()
            if (url != null) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: Exception) {
                }
            }
        },
        secondaryLabel = stringResource(R.string.update_notice_secondary),
        onSecondary = dismiss,
        modifier = modifier
    )
}
