package com.makkispacejam.fluxa.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

object NetworkUtils {

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun getTargetVideoQuality(context: Context, preferredQuality: String? = null): Int {
        if (preferredQuality != null && preferredQuality != "Automática" && preferredQuality != "Seleccionar calidad por defecto") {
            return when (preferredQuality) {
                "1080p (FHD)" -> 1080
                "720p (HD)" -> 720
                "480p" -> 480
                "360p" -> 360
                "Ahorro de datos" -> 240
                else -> preferredQuality.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 480
            }
        }

        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = cm.getNetworkCapabilities(cm.activeNetwork)
            ?: return 480

        val kbps = capabilities.linkDownstreamBandwidthKbps

        val quality = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->
                wifiQuality(kbps)

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->
                mobileQuality(kbps)

            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->
                1080
            else -> 480
        }

        Log.d("FluxaQuality", "📶 ${kbps}kbps → ${quality}p")
        return quality
    }

    private fun wifiQuality(kbps: Int): Int = when {
        kbps > 5000 -> 1080
        kbps > 2000 -> 720
        kbps > 800 -> 480
        else -> 480
    }

    @Suppress("DEPRECATION")
    private fun mobileQuality(kbps: Int): Int {
        if (kbps > 3000) return 720
        if (kbps > 1000) return 480
        if (kbps > 400) return 360
        if (kbps > 0) return 240
        return 480
    }
}