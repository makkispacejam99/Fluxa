package com.makkispacejam.fluxa.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.geometry.Offset
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val ScallopedShape = GenericShape { size, _ ->
    val points = 12
    val center = Offset(size.width / 2f, size.height / 2f)
    val radius = size.width / 2.1f
    val angleStep = (2.0 * PI / points).toFloat()

    for (i in 0 until points) {
        val angle = i * angleStep
        val midAngle = angle + angleStep / 2f
        val nextAngle = (i + 1) * angleStep

        val x = center.x + radius * cos(angle.toDouble()).toFloat()
        val y = center.y + radius * sin(angle.toDouble()).toFloat()

        val mx = center.x + (radius * 1.15f) * cos(midAngle.toDouble()).toFloat()
        val my = center.y + (radius * 1.15f) * sin(midAngle.toDouble()).toFloat()

        val nx = center.x + radius * cos(nextAngle.toDouble()).toFloat()
        val ny = center.y + radius * sin(nextAngle.toDouble()).toFloat()

        if (i == 0) moveTo(x, y)
        quadraticTo(mx, my, nx, ny)
    }
    close()
}