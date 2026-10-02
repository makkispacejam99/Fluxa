package com.makkispacejam.fluxa.ui.screens.player

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import com.makkispacejam.fluxa.FluxaPlaybackService
import com.makkispacejam.fluxa.R

// Popup Fallback
private const val FORCE_POPUP_FALLBACK = false

fun canUseNativePip(context: Context): Boolean {
    if (FORCE_POPUP_FALLBACK) return false
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}

@SuppressLint("UseKtx")
@OptIn(UnstableApi::class)
fun openPopupPlayer(context: Context) {
    if (!Settings.canDrawOverlays(context)) {
        Toast.makeText(context, R.string.popup_overlay_needed, Toast.LENGTH_LONG).show()
        runCatching {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${context.packageName}".toUri()
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
        return
    }
    FluxaPlaybackService.instance?.showPopupOverlay()
}
