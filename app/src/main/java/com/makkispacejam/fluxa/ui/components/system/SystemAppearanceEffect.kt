@file:Suppress("DEPRECATION")

package com.makkispacejam.fluxa.ui.components.system

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController


// Apariencia del sistema
@Composable
fun SystemAppearanceEffect(isDark: Boolean, amoledMode: Boolean = false) {
    val context = LocalContext.current
    val activity = context as? Activity
    val systemUiController = rememberSystemUiController()
    MaterialTheme.colorScheme.surface

    LaunchedEffect(isDark, amoledMode) {
        try {
            activity?.let { act ->
                val window = act.window
                val controller = WindowCompat.getInsetsController(window, window.decorView)

                WindowCompat.setDecorFitsSystemWindows(window, false)

                val navBarColor = if (isDark && amoledMode) {
                    androidx.compose.ui.graphics.Color.Black
                } else {
                    androidx.compose.ui.graphics.Color.Transparent
                }

                systemUiController.setStatusBarColor(
                    color = androidx.compose.ui.graphics.Color.Transparent,
                    darkIcons = !isDark
                )
                systemUiController.setNavigationBarColor(
                    color = navBarColor,
                    darkIcons = !isDark,
                    navigationBarContrastEnforced = false
                )

                controller.show(WindowInsetsCompat.Type.systemBars())
                controller.isAppearanceLightStatusBars = !isDark
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}