@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa

import android.content.res.Resources
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import kotlin.math.roundToInt

@UnstableApi
fun computeAspect(player: Player): Float {
    val vs: VideoSize = player.videoSize
    return if (vs.width > 0 && vs.height > 0) {
        if (vs.unappliedRotationDegrees == 90 || vs.unappliedRotationDegrees == 270) {
            vs.height.toFloat() / vs.width
        } else {
            vs.width.toFloat() / vs.height
        }
    } else {
        16f / 9f
    }
}

fun computeInitialSize(resources: Resources, aspect: Float): Pair<Int, Int> {
    val dm = resources.displayMetrics
    val maxW = (dm.widthPixels * 0.64f).roundToInt()
    val maxH = (dm.heightPixels * 0.46f).roundToInt()
    val minH = (120 * dm.density).roundToInt()
    var h = maxH
    var w = (h * aspect).roundToInt()
    if (w > maxW) {
        w = maxW
        h = (w / aspect).roundToInt()
    }
    if (h < minH) h = minH
    return w to h
}
