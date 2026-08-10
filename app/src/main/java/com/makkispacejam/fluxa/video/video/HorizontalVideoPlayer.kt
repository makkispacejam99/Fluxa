package com.makkispacejam.fluxa.video.video

import android.graphics.Color
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.makkispacejam.fluxa.FluxaPlaybackService
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Lógica del reproductor horizontal
@OptIn(UnstableApi::class)
@Composable
fun HorizontalVideoPlayer(
    resizeMode: Int = 0,
    onProgressUpdate: (currentPos: Long, totalDuration: Long, bufferedPos: Long) -> Unit,
    onSeekControllerReady: ((Float) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var serviceReady by remember { mutableStateOf(false) }
    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }

    // Iniciar servicio
    LaunchedEffect(Unit) {
        val intent = android.content.Intent(context, FluxaPlaybackService::class.java)
        context.startService(intent)
        val startTime = System.currentTimeMillis()
        while (FluxaPlaybackService.instance == null && System.currentTimeMillis() - startTime < 3000) {
            delay(100)
        }
        serviceReady = true
    }

    // Iniciar reproductor
    LaunchedEffect(serviceReady) {
        if (!serviceReady) return@LaunchedEffect
        val player = FluxaPlaybackService.instance?.getPlayer() ?: return@LaunchedEffect
        playerViewRef.value?.player = player
    }

    // Progreso
    LaunchedEffect(serviceReady) {
        if (!serviceReady) return@LaunchedEffect
        while (isActive) {
            val player = FluxaPlaybackService.instance?.getPlayer()
            if (player != null) {
                onProgressUpdate(
                    player.currentPosition,
                    player.duration.coerceAtLeast(0L),
                    player.bufferedPosition.coerceAtLeast(0L)
                )
            }
            delay(250)
        }
    }

    onSeekControllerReady { percent ->
        val player = FluxaPlaybackService.instance?.getPlayer() ?: return@onSeekControllerReady
        val duration = player.duration
        if (duration > 0) player.seekTo((percent * duration).toLong())
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                this.resizeMode = resizeMode
                setShutterBackgroundColor(Color.TRANSPARENT)
                setBackgroundColor(Color.TRANSPARENT)
                subtitleView?.visibility = android.view.View.GONE
                playerViewRef.value = this
                FluxaPlaybackService.instance?.getPlayer()?.let { player = it }
            }
        },
        update = { playerView ->
            if (playerView.resizeMode != resizeMode) {
                playerView.resizeMode = resizeMode
                playerView.requestLayout()
            }
            playerViewRef.value = playerView
            if (serviceReady) {
                FluxaPlaybackService.instance?.getPlayer()?.let {
                    if (playerView.player != it) playerView.player = it
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}