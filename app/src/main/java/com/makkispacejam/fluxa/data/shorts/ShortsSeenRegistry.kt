package com.makkispacejam.fluxa.data.shorts

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.makkispacejam.fluxa.data.UserPreferences
import com.makkispacejam.fluxa.models.VideoModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import androidx.core.content.edit

data class RecentShort(
    val videoId: String,
    val title: String,
    val channelId: String,
    val channelName: String,
    val servedAt: Long
)

object ShortsSeenRegistry {

    private const val PREFS_NAME = "fluxa_shorts_seen"
    private const val KEY_SEEN = "served_v1"
    private const val KEY_RECENT = "recent_v1"
    private const val MAX_ENTRIES = 6000
    private const val MAX_RECENT = 40
    private const val PRUNE_AFTER_MS = 45L * 24 * 60 * 60 * 1000

    const val WINDOW_HARD = 3 * 60 * 60 * 1000L

    const val WINDOW_SHADOW = 14L * 24 * 60 * 60 * 1000

    const val WINDOW_SOFT = 30L * 24 * 60 * 60 * 1000

    const val WINDOW_3_DAYS = 3L * 24 * 60 * 60 * 1000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val entries = LinkedHashMap<String, Long>()
    private val recent = ArrayDeque<RecentShort>()

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var loaded = false

    private val lock = Any()

    fun attach(context: Context) {
        if (prefs != null) return
        synchronized(lock) {
            if (prefs != null) return
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (!loaded) load()
        }
    }

    private fun load() {
        val p = prefs ?: return
        loaded = true
        runCatching { decodeSeen(p.getString(KEY_SEEN, null).orEmpty()) }
            .onFailure { Log.w("FluxaShorts", "no se pudo leer el registro de vistos", it) }
        runCatching { decodeRecent(p.getString(KEY_RECENT, null).orEmpty()) }
            .onFailure { Log.w("FluxaShorts", "no se pudo leer el anillo de vistos", it) }
    }

    fun markServed(videos: List<VideoModel>) {
        if (videos.isEmpty()) return
        if (UserPreferences.incognitoActive) return
        val now = System.currentTimeMillis()
        var dirty = false

        synchronized(lock) {
            for (video in videos) {
                val id = video.id
                if (id.isBlank()) continue
                if (entries[id] != now) {
                    entries[id] = now
                    dirty = true
                }
                recent.removeAll { it.videoId == id }
                recent.addFirst(
                    RecentShort(
                        videoId = id,
                        title = video.title,
                        channelId = ShortsClassifier.normalizeChannelId(video.channelId),
                        channelName = video.channelName,
                        servedAt = now
                    )
                )
            }
            while (recent.size > MAX_RECENT) {
                recent.removeLast()
                dirty = true
            }
            if (entries.size > MAX_ENTRIES) {
                entries.entries.sortedBy { it.value }
                    .take(entries.size - MAX_ENTRIES)
                    .forEach { entries.remove(it.key) }
                dirty = true
            }
        }
        if (dirty) persist()
    }

    fun isServedWithin(videoId: String, windowMs: Long, now: Long = System.currentTimeMillis()): Boolean {
        val ts = synchronized(lock) { entries[videoId] } ?: return false
        return now - ts < windowMs
    }

    fun servedWithin(windowMs: Long, now: Long = System.currentTimeMillis()): Set<String> = synchronized(lock) {
        entries.filterValues { now - it < windowMs }.keys.toSet()
    }

    fun recentShorts(): List<RecentShort> = synchronized(lock) { recent.toList() }

    fun clear() {
        synchronized(lock) {
            entries.clear()
            recent.clear()
        }
        persist()
    }

    private fun persist() {
        val p = prefs ?: return
        val seenPayload = synchronized(lock) { encodeSeen() }
        val recentPayload = synchronized(lock) { encodeRecent() }
        scope.launch {
            runCatching {
                p.edit {
                    putString(KEY_SEEN, seenPayload)
                        .putString(KEY_RECENT, recentPayload)
                }
            }.onFailure { Log.w("FluxaShorts", "no se pudo guardar el registro de vistos", it) }
        }
    }

    private fun encodeSeen(): String {
        val now = System.currentTimeMillis()
        val sb = StringBuilder(entries.size * 16)
        for ((id, ts) in entries) {
            if (now - ts > PRUNE_AFTER_MS) continue
            if (sb.isNotEmpty()) sb.append('\n')
            sb.append(id).append(' ').append(ts)
        }
        return sb.toString()
    }

    private fun encodeRecent(): String = synchronized(lock) {
        recent.joinToString("\n") { item ->
            listOf(
                item.videoId,
                item.title.replace('\n', ' ').replace('|', ' '),
                item.channelId,
                item.channelName.replace('\n', ' ').replace('|', ' '),
                item.servedAt.toString()
            ).joinToString("|")
        }
    }

    private fun decodeSeen(raw: String) {
        if (raw.isBlank()) return
        val now = System.currentTimeMillis()
        val parsed = HashMap<String, Long>()
        raw.split('\n').forEach { line ->
            val parts = line.trim().split(' ')
            if (parts.size != 2) return@forEach
            val id = parts[0]
            val ts = parts[1].toLongOrNull() ?: return@forEach
            if (id.isBlank() || now - ts > PRUNE_AFTER_MS) return@forEach
            parsed[id] = ts
        }
        parsed.entries.sortedBy { it.value }.forEach { entries[it.key] = it.value }
        if (parsed.size > MAX_ENTRIES) {
            parsed.entries.sortedBy { it.value }
                .take(parsed.size - MAX_ENTRIES)
                .forEach { entries.remove(it.key) }
        }
    }

    private fun decodeRecent(raw: String) {
        if (raw.isBlank()) return
        val now = System.currentTimeMillis()
        val parsed = raw.split('\n').mapNotNull { line ->
            val parts = line.split('|')
            if (parts.size < 4) return@mapNotNull null
            val id = parts[0]
            val servedAt = if (parts.size >= 5) parts[4].toLongOrNull() ?: now else now
            if (id.isBlank() || now - servedAt > PRUNE_AFTER_MS) return@mapNotNull null
            RecentShort(
                videoId = id,
                title = parts[1],
                channelId = parts[2],
                channelName = parts[3],
                servedAt = servedAt
            )
        }
        recent.clear()
        parsed.take(MAX_RECENT).forEach { recent.addLast(it) }
    }
}
