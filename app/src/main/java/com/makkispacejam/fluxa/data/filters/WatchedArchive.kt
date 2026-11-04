package com.makkispacejam.fluxa.data.filters

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.content.edit

object WatchedArchive {

    private const val PREFS_NAME = "fluxa_watched_archive"
    private const val KEY_IDS = "watched_v1"
    private const val MAX_ENTRIES = 20000

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val entries = LinkedHashSet<String>()
    private val lock = Any()
    private val state = MutableStateFlow<Set<String>>(emptySet())

    @Volatile
    private var prefs: SharedPreferences? = null

    @Volatile
    private var loaded = false

    val watchedIds: StateFlow<Set<String>> = state.asStateFlow()

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
        runCatching { decode(p.getString(KEY_IDS, null).orEmpty()) }
            .onFailure { Log.w("FluxaWatched", "no se pudo leer el archivo de vistos", it) }
        publish()
    }

    fun markWatched(videoId: String) {
        if (videoId.isBlank()) return
        markWatchedAll(listOf(videoId))
    }

    fun markWatchedAll(videoIds: Collection<String>) {
        if (videoIds.isEmpty()) return
        var dirty = false
        synchronized(lock) {
            for (id in videoIds) {
                if (id.isBlank()) continue
                if (entries.add(id)) dirty = true
            }
            while (entries.size > MAX_ENTRIES) {
                val oldest = entries.firstOrNull() ?: break
                entries.remove(oldest)
                dirty = true
            }
        }
        if (dirty) {
            publish()
            persist()
        }
    }

    fun preserveBeforeWipe(watchedIds: Collection<String>) {
        markWatchedAll(watchedIds)
    }

    fun snapshot(): Set<String> = synchronized(lock) { LinkedHashSet(entries) }

    private fun publish() {
        val copy = synchronized(lock) { LinkedHashSet(entries) }
        if (state.value != copy) state.value = copy
    }

    private fun persist() {
        val p = prefs ?: return
        val payload = synchronized(lock) { entries.joinToString("\n") }
        scope.launch {
            runCatching {
                p.edit { putString(KEY_IDS, payload) }
            }.onFailure { Log.w("FluxaWatched", "no se pudo guardar el archivo de vistos", it) }
        }
    }

    private fun decode(raw: String) {
        if (raw.isBlank()) return
        raw.split('\n').forEach { line ->
            val id = line.trim()
            if (id.isNotEmpty()) entries.add(id)
        }
        while (entries.size > MAX_ENTRIES) {
            entries.remove(entries.first())
        }
    }
}
