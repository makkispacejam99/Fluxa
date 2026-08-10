package com.makkispacejam.fluxa.ui.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment

object FluxaAnimations {
    private val expressiveSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    const val FADE_MS = 100
    const val SHARED_TRANSITION_MS = 100
    const val NAV_TRANSITION_MS = 100

    // Crossfade
    fun <T> premiumFadeSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = FADE_MS,
        easing = EaseInOutQuart
    )

    // Animación de pantallas compartidas
    fun detailEnter() = fadeIn(animationSpec = tween(durationMillis = SHARED_TRANSITION_MS)) +
            scaleIn(initialScale = 0.92f, animationSpec = expressiveSpring)

    fun detailExit() = fadeOut(animationSpec = tween(durationMillis = SHARED_TRANSITION_MS)) +
            scaleOut(targetScale = 0.92f, animationSpec = tween(durationMillis = SHARED_TRANSITION_MS))

    // Animaciones de entrada y salida generales
    fun enterTransition() = fadeIn(animationSpec = tween(durationMillis = NAV_TRANSITION_MS)) +
            scaleIn(initialScale = 0.92f, animationSpec = expressiveSpring)

    fun exitTransition() = fadeOut(animationSpec = tween(durationMillis = NAV_TRANSITION_MS)) +
            scaleOut(targetScale = 0.92f, animationSpec = tween(durationMillis = NAV_TRANSITION_MS))

    // Animación de intro
    fun introTransition() = fadeIn(
        animationSpec = tween(durationMillis = FADE_MS)
    ) + scaleIn(
        initialScale = 0.92f,
        animationSpec = expressiveSpring
    ) togetherWith fadeOut(
        animationSpec = tween(durationMillis = FADE_MS)
    )

    fun searchOverlayEnter() = fadeIn(
        animationSpec = tween(durationMillis = 150),
        initialAlpha = 0.3f
    ) + expandVertically(
        expandFrom = Alignment.Top,
        animationSpec = tween(durationMillis = 200)
    )

    fun searchOverlayExit() = fadeOut(
        animationSpec = tween(durationMillis = 150),
        targetAlpha = 0.3f
    ) + shrinkVertically(
        shrinkTowards = Alignment.Top,
        animationSpec = tween(durationMillis = 200)
    )
}
