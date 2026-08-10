package com.makkispacejam.fluxa

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.makkispacejam.fluxa.data.local.FluxaDatabase
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.data.newpipe.YouTubeDownloader
import com.makkispacejam.fluxa.video.video.VideoProgressCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization

class FluxaApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .crossfade(true)
            .respectCacheHeaders(false)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        try {
            val prefs = UserPreferences(this)
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(prefs.contentLanguage)
            AppCompatDelegate.setApplicationLocales(appLocale)

            NewPipe.init(
                YouTubeDownloader(),
                Localization(prefs.contentLanguage),
                ContentCountry(prefs.contentRegion)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        VideoProgressCache.init(this)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = FluxaDatabase.getDatabase(this@FluxaApplication)
                db.fluxaDao().deleteOrphanedStreamUrlEntries()
                db.fluxaDao().deleteGarbledVideoIdEntries()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val channel = NotificationChannel(
            "fluxa_playback_channel",
            "Reproducción de Fluxa",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Controles de reproducción de video"
            setShowBadge(false)
            setSound(null, null)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
}