package com.makkispacejam.fluxa.data.local

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.net.Uri
import androidx.core.app.NotificationCompat
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

// Copia de seguridad
data class BackupData(
    val subscriptions: List<SubscriptionEntity>,
    val playlists: List<PlaylistEntity>,
    val playlistItems: List<PlaylistItemEntity>,
    val videoInteractions: List<VideoInteractionEntity>,
    val watchedVideos: List<WatchedVideoEntity> = emptyList()
)

// Restauración de copia de seguridad
class BackupRestoreManager(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val database: FluxaDatabase = FluxaDatabase.getDatabase(context)
    private val fluxaDao: FluxaDao = database.fluxaDao()
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val channelId = "import_progress"

    init {
        val channel = NotificationChannel(channelId, "Import Progress", NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)
    }

    // Exportación de datos
    suspend fun exportData(outputUri: Uri) = withContext(Dispatchers.IO) {
        val subscriptions = fluxaDao.getAllSubscriptionsForBackup()
        val playlists = fluxaDao.getAllPlaylists()
        val playlistItems = fluxaDao.getAllPlaylistItems()
        val videoInteractions = fluxaDao.getAllVideoInteractions()
        val watchedVideos = fluxaDao.getAllWatchedVideosForBackup()

        val backupData = BackupData(subscriptions, playlists, playlistItems, videoInteractions, watchedVideos)
        val jsonString = gson.toJson(backupData)

        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
            OutputStreamWriter(outputStream).use { writer ->
                writer.write(jsonString)
            }
        }
    }

    // Importación de datos
    suspend fun importData(inputUri: Uri) = withContext(Dispatchers.IO) {
        val jsonString = context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                reader.readText()
            }
        } ?: throw IllegalArgumentException("No data found in the selected file.")

        if (jsonString.trim().startsWith("Channel Id") || jsonString.trim().startsWith("\"Channel Id\"")) {
            throw IllegalArgumentException("FILE_FORMAT_ERROR")
        }

        if (!jsonString.trim().startsWith("{")) {
            throw IllegalArgumentException("FILE_FORMAT_ERROR")
        }

        val backupDataType = object : TypeToken<BackupData>() {}.type
        val backupData: BackupData = gson.fromJson(jsonString, backupDataType)

        fluxaDao.clearAllSubscriptions()
        fluxaDao.clearAllPlaylists()
        fluxaDao.clearAllPlaylistItems()
        fluxaDao.clearAllVideoInteractions()
        fluxaDao.clearAllWatchedVideos()

        fluxaDao.insertSubscriptions(backupData.subscriptions)
        fluxaDao.insertPlaylists(backupData.playlists)
        fluxaDao.insertPlaylistItems(backupData.playlistItems)
        fluxaDao.insertVideoInteractions(backupData.videoInteractions)
        fluxaDao.insertWatchedVideos(backupData.watchedVideos)
    }

    // Importación por CSV
    suspend fun importFromCSV(inputUri: Uri, onProgress: (Int, Int) -> Unit) = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(inputUri) ?: throw IllegalArgumentException("No data found in the selected file.")
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))

        val allLines = reader.readLines().map { it.trim().removePrefix("\uFEFF") }
        if (allLines.isEmpty()) return@withContext

        if (allLines.first().trim().startsWith("{")) {
            throw IllegalArgumentException("FILE_FORMAT_ERROR")
        }

        val hasHeader = allLines.first().contains("Channel Id", ignoreCase = true)
        val startRow = if (hasHeader) 1 else 0
        val total = allLines.size - startRow
        if (total <= 0) return@withContext

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Importando suscripciones")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(total, 0, false)

        try {
            notificationManager.notify(888, notificationBuilder.build())

            val subscriptionsToInsert = mutableListOf<SubscriptionEntity>()
            val existingIds = fluxaDao.getAllSubscriptions().map { it.channelId }.toSet()

            for (i in startRow until allLines.size) {
                val line = allLines[i]
                if (line.isBlank()) continue
                
                val parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                    .map { it.trim().removeSurrounding("\"") }

                if (parts.size >= 2) {
                    val channelId = parts.find { it.startsWith("UC") && it.length >= 24 }
                    val channelTitle = parts.find { 
                        it != channelId && !it.startsWith("http") && it.isNotBlank() 
                    } ?: "Unknown Channel"

                    if (!channelId.isNullOrEmpty() && !existingIds.contains(channelId)) {
                        subscriptionsToInsert.add(SubscriptionEntity(channelId, channelTitle, null))
                    }
                }
                
                val current = (i - startRow) + 1
                if (current % 100 == 0 || current == total) {
                    notificationManager.notify(888, notificationBuilder.setProgress(total, current, false).build())
                    onProgress(current, total)
                }
            }

            if (subscriptionsToInsert.isNotEmpty()) {
                subscriptionsToInsert.chunked(100).forEach { chunk ->
                    fluxaDao.insertSubscriptions(chunk)
                }
            }

            com.makkispacejam.fluxa.data.HomeRepository().clearFeedCache()

        } finally {
            notificationManager.cancel(888)
        }
    }

    // Limpieza de datos
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        database.clearAllTables()
    }
}
