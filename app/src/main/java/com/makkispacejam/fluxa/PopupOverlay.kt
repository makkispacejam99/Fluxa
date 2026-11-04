@file:Suppress("FunctionName", "DEPRECATION")

package com.makkispacejam.fluxa

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.view.TextureView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.ui.components.player.shared.WavySlider
import com.makkispacejam.fluxa.ui.theme.Typography
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Duration.Companion.milliseconds

@SuppressLint("LocalContextResourcesRead")
@UnstableApi
@Composable
fun PopupMiniPlayer(
    player: Player,
    onClose: () -> Unit,
    aspect: Float,
    initialWidthPx: Int,
    initialHeightPx: Int,
    onPositionChanged: (x: Int, y: Int, width: Int, height: Int) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    val displayMetrics = context.resources.displayMetrics
    val density = LocalDensity.current.density
    val screenW = displayMetrics.widthPixels
    val screenH = displayMetrics.heightPixels
    val statusBar = statusBarHeightPx(context)
    val margin = (8 * density).roundToInt()
    val minHeightPx = (72 * density).roundToInt()
    val maxWidthPx = (screenW - margin * 2).coerceAtLeast(minHeightPx)
    val maxHeightPx = (screenH - statusBar - margin).coerceAtLeast(minHeightPx)
    val corner = 20.dp

    val sizeTargets = intArrayOf(
        (screenW * 0.64f).roundToInt(),
        (screenW * 0.50f).roundToInt(),
        (screenW * 0.78f).roundToInt(),
    )

    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentAspect by remember { mutableFloatStateOf(aspect) }
    var pendingRotation by remember { mutableFloatStateOf(0f) }

    var widthPx by remember { mutableIntStateOf(initialWidthPx) }
    var heightPx by remember { mutableIntStateOf(initialHeightPx) }
    var posX by remember { mutableFloatStateOf((screenW - initialWidthPx - margin).toFloat()) }
    var posY by remember { mutableFloatStateOf((statusBar + margin).toFloat()) }

    var resizeTap by remember { mutableIntStateOf(sizeTargets.size - 1) }
    var uiVisible by remember { mutableStateOf(true) }

    var currentPosMs by remember { mutableLongStateOf(player.currentPosition.coerceAtLeast(0L)) }
    var totalDurationMs by remember { mutableLongStateOf(player.duration.coerceAtLeast(0L)) }
    var bufferedPosMs by remember { mutableLongStateOf(player.bufferedPosition.coerceAtLeast(0L)) }
    var isUserDragging by remember { mutableStateOf(false) }
    var dragProgressMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(posX, posY, widthPx, heightPx) {
        onPositionChanged(posX.roundToInt(), posY.roundToInt(), widthPx, heightPx)
    }

    LaunchedEffect(isPlaying, uiVisible) {
        while (isActive) {
            if (!isUserDragging) currentPosMs = player.currentPosition.coerceAtLeast(0L)
            totalDurationMs = player.duration.coerceAtLeast(0L)
            bufferedPosMs = player.bufferedPosition.coerceAtLeast(0L)
            delay(250.milliseconds)
        }
    }

    LaunchedEffect(uiVisible, isPlaying) {
        if (uiVisible && isPlaying && !isUserDragging) {
            delay(2500.milliseconds)
            uiVisible = false
        }
    }

    val playerListener = remember(player) {
        object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlaying = player.isPlaying
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val rot = videoSize.unappliedRotationDegrees
                    pendingRotation = rot.toFloat()
                    currentAspect = if (rot == 90 || rot == 270) {
                        videoSize.height.toFloat() / videoSize.width
                    } else {
                        videoSize.width.toFloat() / videoSize.height
                    }
                    var newW = (heightPx * currentAspect).roundToInt()
                    var newH = heightPx
                    if (newW > maxWidthPx) {
                        newW = maxWidthPx
                        newH = (newW / currentAspect).roundToInt()
                    }
                    widthPx = newW
                    heightPx = newH.coerceIn(minHeightPx, maxHeightPx)
                    posX = posX.coerceIn(0f, (screenW - widthPx).coerceAtLeast(0).toFloat())
                    posY = posY.coerceIn(statusBar.toFloat(), (screenH - heightPx).coerceAtLeast(0).toFloat())
                }
            }
        }
    }

    LaunchedEffect(player) {
        player.addListener(playerListener)
    }

    DisposableEffect(player) {
        onDispose { player.removeListener(playerListener) }
    }

    val currentWidthPx by rememberUpdatedState(widthPx)
    val currentHeightPx by rememberUpdatedState(heightPx)
    val sliderBandPx by rememberUpdatedState(with(LocalDensity.current) { 52.dp.toPx() })
    var dragStartInSlider by remember { mutableStateOf(false) }

    val dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { offset ->
                dragStartInSlider = offset.y >= currentHeightPx - sliderBandPx
            },
            onDragEnd = { dragStartInSlider = false },
            onDragCancel = { dragStartInSlider = false },
            onDrag = { change, dragAmount ->
                change.consume()
                if (!dragStartInSlider) {
                    posX = (posX + dragAmount.x).coerceIn(
                        0f,
                        (screenW - currentWidthPx).coerceAtLeast(0).toFloat()
                    )
                    posY = (posY + dragAmount.y).coerceIn(
                        statusBar.toFloat(),
                        (screenH - currentHeightPx).coerceAtLeast(0).toFloat()
                    )
                    onPositionChanged(posX.roundToInt(), posY.roundToInt(), currentWidthPx, currentHeightPx)
                }
            }
        )
    }

    fun onVideoTap() {
        uiVisible = !uiVisible
    }

    fun onPlayPause() {
        uiVisible = true
        if (player.isPlaying) player.pause() else player.play()
    }

    val tapModifier = Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val firstDown = awaitFirstDown(requireUnconsumed = false)
            val firstPos = firstDown.position
            var settled = false
            var upConsumed = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == firstDown.id } ?: continue
                when {
                    !change.pressed -> {
                        upConsumed = change.isConsumed
                        break
                    }
                    else -> {
                        if (!settled) {
                            val dx = change.position.x - firstPos.x
                            val dy = change.position.y - firstPos.y
                            if (sqrt(dx * dx + dy * dy) > viewConfiguration.touchSlop) {
                                settled = true
                            }
                        }
                    }
                }
            }
            if (settled || upConsumed) return@awaitEachGesture
            onVideoTap()
        }
    }

    fun applyResizeSize(targetWidthPx: Int) {
        val w = targetWidthPx.coerceAtMost(maxWidthPx)
        var h = (w / currentAspect).roundToInt()
        if (h > maxHeightPx) {
            h = maxHeightPx
            widthPx = (h * currentAspect).roundToInt().coerceAtMost(maxWidthPx)
            heightPx = h
        } else {
            widthPx = w
            heightPx = h
        }
        posX = (screenW - widthPx - margin).coerceAtLeast(0).toFloat()
        posY = (statusBar + margin).coerceAtMost(screenH - heightPx).coerceAtLeast(0).toFloat()
    }

    fun onResizeTap() {
        resizeTap = (resizeTap + 1) % sizeTargets.size
        applyResizeSize(sizeTargets[resizeTap])
    }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val scheme = remember(isDark, context) {
        runCatching {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }.getOrElse { if (isDark) darkColorScheme() else lightColorScheme() }
    }

    MaterialTheme(colorScheme = scheme, typography = Typography) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(corner))
                .background(Color.Black)
                .then(dragModifier)
                .then(tapModifier)
        ) {
            AndroidView(
                factory = { ctx ->
                    TextureView(ctx).apply {
                        player.setVideoTextureView(this)
                    }
                },
                update = { view ->
                    player.setVideoTextureView(view)
                    if (pendingRotation != 0f) {
                        val cx = view.width / 2f
                        val cy = view.height / 2f
                        val m = Matrix().apply { postRotate(pendingRotation, cx, cy) }
                        view.setTransform(m)
                    } else {
                        view.setTransform(Matrix())
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(corner))
            )

            AnimatedVisibility(
                visible = uiVisible || !isPlaying,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(250)),
                modifier = Modifier.fillMaxSize(),
                label = "popupCenterUi"
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    IconButton(
                        onClick = { onPlayPause() },
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0x66000000), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .height(44.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xCC000000),
                                Color(0x66000000),
                                Color.Transparent
                            )
                        )
                    )
            ) {
                AnimatedVisibility(
                    visible = uiVisible || !isPlaying,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(250)),
                    label = "popupHeaderUi"
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(44.dp)) {
                        IconButton(
                            onClick = { onResizeTap() },
                            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ClearAll,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        IconButton(
                            onClick = {
                                if (player.isPlaying) player.pause()
                                onClose()
                            },
                            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = uiVisible || !isPlaying,
                enter = fadeIn(tween(150)),
                exit = fadeOut(tween(250)),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                label = "popupProgressUi"
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    WavySlider(
                        value = if (totalDurationMs > 0) {
                            (if (isUserDragging) dragProgressMs else currentPosMs)
                                .toFloat() / totalDurationMs.toFloat()
                        } else 0f,
                        bufferedValue = if (totalDurationMs > 0) {
                            (bufferedPosMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f,
                        onValueChange = { fraction ->
                            isUserDragging = true
                            dragProgressMs = (fraction * totalDurationMs.coerceAtLeast(1L)).toLong()
                        },
                        onValueChangeFinished = {
                            if (totalDurationMs > 0) {
                                player.seekTo(dragProgressMs.coerceIn(0L, totalDurationMs))
                            }
                            isUserDragging = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@SuppressLint("DiscouragedApi", "InternalInsetResource")
fun statusBarHeightPx(context: android.content.Context): Int {
    val res = context.resources
    val id = res.getIdentifier("status_bar_height", "dimen", "android")
    return if (id > 0) res.getDimensionPixelSize(id) else 0
}
