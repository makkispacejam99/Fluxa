@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.video.shorts

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.makkispacejam.fluxa.video.source.ChunkedReconnectDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Reproductor de video de shorts
@SuppressLint("InflateParams")
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    videoUrl: String,
    thumbnailUrl: String,
    isPaused: Boolean = false,
    playbackSpeed: Float = 1f,
    onProgressUpdate: (currentPos: Long, totalDuration: Long, bufferedPos: Long) -> Unit = { _, _, _ -> },
    onSeekControllerReady: ((seekToPercent: Float) -> Unit) -> Unit = {},
    onLoadingChanged: (Boolean) -> Unit = {}
) {
    val systemUiController = rememberSystemUiController()
    val appContext = LocalContext.current.applicationContext ?: LocalContext.current

    val trackSelector = remember {
        val targetQuality = com.makkispacejam.fluxa.utils.NetworkUtils.getTargetVideoQuality(appContext)
        val (maxWidth, maxHeight) = when {
            targetQuality >= 1080 -> 1920 to 1080
            targetQuality >= 720 -> 1280 to 720
            targetQuality >= 480 -> 854 to 480
            else -> 640 to 360
        }
        DefaultTrackSelector(appContext).apply {
            setParameters(buildUponParameters().setMaxVideoSize(maxWidth, maxHeight))
        }
    }

    val okHttpClient = remember {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
            .connectionPool(okhttp3.ConnectionPool(20, 5, java.util.concurrent.TimeUnit.MINUTES))
            .build()
    }

    val httpDataSourceFactory = remember {
        OkHttpDataSource.Factory(okHttpClient)
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .setDefaultRequestProperties(
                mapOf(
                    "Origin" to "https://www.youtube.com",
                    "Referer" to "https://www.youtube.com/"
                )
            )
    }
    val chunkedDataSourceFactory = remember {
        ChunkedReconnectDataSource.Factory(httpDataSourceFactory)
    }

    val loadControl = remember<LoadControl> {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                50_000,
                120_000,
                4_000,
                6_000
            )
            .setBackBuffer(15_000, true)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(appContext)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
            .build().apply {
                repeatMode = Player.REPEAT_MODE_ALL
                playWhenReady = true
            }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                onLoadingChanged(playbackState == Player.STATE_BUFFERING)
                Log.d("FluxaBuffering", "Shorts State: $playbackState (1=Idle, 2=Buffering, 3=Ready, 4=Ended)")
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                Log.d("FluxaBuffering", "Shorts Loading: $isLoading | Buffered: ${exoPlayer.bufferedPosition / 1000}s")
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                Log.e("FluxaBuffering", "Shorts ERROR: ${error.errorCodeName} - ${error.message}")
            }
        }
        exoPlayer.addListener(listener)
        onLoadingChanged(exoPlayer.playbackState == Player.STATE_BUFFERING)
        onDispose {
            exoPlayer.removeListener(listener)
        }
    }

    onSeekControllerReady { percent ->
        val duration = exoPlayer.duration
        if (duration > 0) exoPlayer.seekTo((percent * duration).toLong())
    }

    DisposableEffect(Unit) {
        systemUiController.setStatusBarColor(
            color = Color.Transparent,
            darkIcons = false
        )
        onDispose {
            systemUiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = true
            )
        }
    }

    // Progreso del video
    LaunchedEffect(exoPlayer) {
        var lastBuffered = 0L
        var stallCount = 0
        while (isActive) {
            val currentBuffered = exoPlayer.bufferedPosition
            val delta = currentBuffered - lastBuffered
            
            if (exoPlayer.playbackState != Player.STATE_IDLE) {
                Log.d("FluxaBuffering", "[HEARTBEAT] Buffered: ${currentBuffered / 1000}s | Growth: +${delta}ms | Loading: ${exoPlayer.isLoading}")

                if (exoPlayer.isLoading && delta == 0L && exoPlayer.playbackState != Player.STATE_READY) {
                    stallCount++
                    if (stallCount >= 10) {
                        Log.w("FluxaBuffering", "[WATCHDOG] Stall detectado. Re-preparando player...")
                        exoPlayer.prepare() 
                        stallCount = 0
                    }
                } else {
                    stallCount = 0
                }
            }
            lastBuffered = currentBuffered

            onProgressUpdate(
                exoPlayer.currentPosition,
                exoPlayer.duration.coerceAtLeast(0L),
                currentBuffered.coerceAtLeast(0L)
            )
            delay(1000)
        }
    }

    // Carga del video
    LaunchedEffect(videoUrl) {
        if (videoUrl.isNotEmpty()) {
            val progressiveMediaSourceFactory =
                ProgressiveMediaSource.Factory(chunkedDataSourceFactory)

            val finalMediaSource = if (videoUrl.contains("|")) {
                val parts = videoUrl.split("|")
                val videoSource =
                    progressiveMediaSourceFactory.createMediaSource(MediaItem.fromUri(parts[0]))
                val audioSource =
                    progressiveMediaSourceFactory.createMediaSource(MediaItem.fromUri(parts[1]))
                MergingMediaSource(videoSource, audioSource)
            } else {
                progressiveMediaSourceFactory.createMediaSource(MediaItem.fromUri(videoUrl))
            }

            exoPlayer.setMediaSource(finalMediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = !isPaused
        }
    }

    LaunchedEffect(isPaused) {
        exoPlayer.playWhenReady = !isPaused
    }

    LaunchedEffect(playbackSpeed) {
        exoPlayer.setPlaybackSpeed(playbackSpeed)
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Capa de Modo Ambiente
        if (thumbnailUrl.isNotEmpty()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(thumbnailUrl)
                    .size(20)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = 0.6f, scaleX = 1.6f, scaleY = 1.6f)
                    .blur(80.dp),
                contentScale = ContentScale.Crop
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.4f),
                            0.5f to Color.Black.copy(alpha = 0.7f),
                            1.0f to Color.Black
                        )
                    )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.05f to Color.Black,
                            0.95f to Color.Black,
                            1.0f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { ctx ->
                    android.view.LayoutInflater.from(ctx).inflate(
                        com.makkispacejam.fluxa.R.layout.custom_player_view,
                        null
                    ).apply {
                        (this as? androidx.media3.ui.PlayerView)?.let { playerView ->
                            playerView.player = exoPlayer
                            playerView.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
                            playerView.setBackgroundColor(android.graphics.Color.TRANSPARENT)

                            playerView.videoSurfaceView?.let { surface ->
                                (surface.layoutParams as? android.widget.FrameLayout.LayoutParams)?.gravity =
                                    android.view.Gravity.CENTER
                            }
                        }
                    }
                },
                update = { view ->
                    (view as? androidx.media3.ui.PlayerView)?.resizeMode =
                        androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Capa de protección para texto
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.65f),
                        0.18f to Color.Black.copy(alpha = 0.15f),
                        0.3f to Color.Transparent,
                        0.7f to Color.Transparent,
                        0.82f to Color.Black.copy(alpha = 0.4f),
                        1.0f to Color.Black.copy(alpha = 0.95f)
                    )
                )
        )
    }
}
