package com.makkispacejam.fluxa

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.makkispacejam.fluxa.ui.screens.navigation.AppNavigation
import com.makkispacejam.fluxa.ui.theme.FluxaTheme

val pendingOpenPlayer = mutableStateOf(false)

class MainActivity : AppCompatActivity() {

    private var mediaController: MediaController? = null
    var isInPipMode by mutableStateOf(false)
        private set

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    100
                )
            }
        }

        try {
            val serviceIntent = Intent(this, FluxaPlaybackService::class.java)
            startService(serviceIntent)

            val sessionToken = SessionToken(this, ComponentName(this, FluxaPlaybackService::class.java))
            val controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
            controllerFuture.addListener({
                mediaController = controllerFuture.get()
            }, MoreExecutors.directExecutor())

        } catch (e: Exception) {
            e.printStackTrace()
        }

        enableEdgeToEdge()
        setContent {
            FluxaTheme {
                AppNavigation()
            }
        }
    }

    override fun onDestroy() {
        mediaController?.release()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        intent?.let { handleIntent(it) }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra("open_player", false)) {
            pendingOpenPlayer.value = true
        }
    }
}



enum class ThemeMode(val titleRes: Int) {
    System(R.string.theme_system),
    Light(R.string.theme_light),
    Dark(R.string.theme_dark)
}
