@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

private val AmoledBlack = Color(0xFF000000)

private fun androidx.compose.material3.ColorScheme.toAmoledScheme(): androidx.compose.material3.ColorScheme {
    return copy(
        background = AmoledBlack,
        onBackground = onBackground,
        surface = AmoledBlack,
        onSurface = onSurface,
        surfaceVariant = AmoledBlack,
        onSurfaceVariant = onSurfaceVariant,
        surfaceTint = Color.Transparent,
        surfaceContainerLowest = AmoledBlack,
        surfaceContainerLow = AmoledBlack,
        surfaceContainer = AmoledBlack,
        surfaceContainerHigh = AmoledBlack,
        surfaceContainerHighest = AmoledBlack,
        inverseSurface = Color(0xFFE2E2E6),
        inverseOnSurface = Color(0xFF2F3033),
        outline = outline,
        outlineVariant = outlineVariant
    )
}

@Composable
fun FluxaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    amoledMode: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            val base = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (darkTheme && amoledMode) base.toAmoledScheme() else base
        }
        darkTheme -> if (amoledMode) DarkColorScheme.toAmoledScheme() else DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = LocalDensity.current.density,
                    fontScale = LocalDensity.current.fontScale.coerceIn(1.0f, 1.15f)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = colorScheme.background
                ) { content() }
            }
        }
    )
}