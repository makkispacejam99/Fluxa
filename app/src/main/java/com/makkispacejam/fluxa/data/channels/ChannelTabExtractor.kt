@file:Suppress("DEPRECATION", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.makkispacejam.fluxa.data.channels

import android.util.Log
import com.makkispacejam.fluxa.data.newpipe.FluxaPlaylistItem
import com.makkispacejam.fluxa.data.newpipe.FluxaStreamItem
import org.schabi.newpipe.extractor.StreamingService
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem
import org.schabi.newpipe.extractor.stream.ContentAvailability
import org.schabi.newpipe.extractor.stream.StreamInfoItem

// Extracción de videos por pestaña
fun extractStreamsFromTab(
    service: StreamingService,
    tabs: List<ListLinkHandler>,
    urlKeyword: String,
    tabLabel: String,
    channelName: String,
    cleanAvatar: String,
    channelId: String = ""
): List<FluxaStreamItem> {
    val tab = tabs.firstOrNull { it.url?.contains(urlKeyword, ignoreCase = true) == true }

    if (tab == null) {
        Log.w("FluxaExtractor", "No se encontró la pestaña para: $tabLabel")
        return emptyList()
    }

    return try {
        val extractor = service.getChannelTabExtractor(tab)
        extractor.fetchPage()

        val items = when {
            extractor.initialPage.items?.isNotEmpty() == true -> extractor.initialPage.items
            extractor.initialPage.nextPage != null -> {
                Log.d("FluxaExtractor", "Usando token de continuación para $tabLabel...")
                extractor.getPage(extractor.initialPage.nextPage!!).items
            }
            else -> emptyList()
        }

        Log.d("FluxaExtractor", "Tab '$tabLabel' -> Elementos rescatados: ${items.size}")

        items.mapNotNull { item ->
            try {
                if (item.url != null && !item.name.isNullOrBlank()) {

                    if (item is StreamInfoItem) {
                        // 1 -- Verificar disponibilidad
                        if (item.contentAvailability != ContentAvailability.AVAILABLE) {
                            return@mapNotNull null
                        }

                        // 2 -- Filtro de tipo y contenido no reproducible
                        val streamType = item.streamType?.name ?: ""
                        if (streamType == "NONE" || streamType.contains("UPCOMING")) {
                            return@mapNotNull null
                        }

                        // 3 -- Filtro por duración
                        val isLive = try {
                            item.javaClass.getMethod("getStreamType").invoke(item).toString().contains("LIVE")
                        } catch (_: Exception) { false }
                        
                        val videoDuration = try {
                            item.javaClass.getMethod("getDuration").invoke(item) as Long
                        } catch (_: Exception) { 0L }
                        
                        if (!isLive && videoDuration <= 0L) {
                            return@mapNotNull null
                        }

                    }

                    val isLive = try {
                        item.javaClass.getMethod("getStreamType").invoke(item)
                            .toString().contains("LIVE")
                    } catch (_: Exception) { false }

                    val viewsCount = try {
                        item.javaClass.getMethod("getViewCount").invoke(item) as Long
                    } catch (_: Exception) { 0L }

                    val videoDuration = try {
                        item.javaClass.getMethod("getDuration").invoke(item) as Long
                    } catch (_: Exception) { 0L }

                    FluxaStreamItem(
                        channelId = channelId,
                        url = item.url,
                        title = item.name,
                        thumbnail = item.thumbnails.firstOrNull()?.url ?: "",
                        uploaderName = channelName,
                        views = viewsCount,
                        duration = videoDuration,
                        isLiveStream = isLive,
                        uploaderAvatar = cleanAvatar,
                        uploadDate = if (item is StreamInfoItem) item.textualUploadDate ?: "" else "",
                        timestamp = if (item is StreamInfoItem) {
                            try { item.uploadDate?.instant?.toEpochMilli() ?: 0L } catch (_: Exception) { 0L }
                        } else 0L
                    )
                } else null
            } catch (_: Exception) { null }
        }

    } catch (e: Exception) {
        Log.e("FluxaExtractor", "Fallo crítico en la pestaña '$tabLabel': ${e.message}")
        emptyList()
    }
}

// Extracción de playlist
fun extractPlaylists(
    service: StreamingService,
    tabs: List<ListLinkHandler>
): List<FluxaPlaylistItem> {
    val tab = tabs.firstOrNull { it.url?.contains("playlist", ignoreCase = true) == true }
        ?: return emptyList()

    return try {
        val extractor = service.getChannelTabExtractor(tab)
        extractor.fetchPage()
        extractor.initialPage.items
            .filterIsInstance<PlaylistInfoItem>()
            .mapNotNull { item ->
                try {
                    FluxaPlaylistItem(
                        url = item.url,
                        name = item.name ?: "",
                        thumbnail = item.thumbnails.firstOrNull()?.url ?: "",
                        videoCount = try {
                            item.streamCount.toInt()
                        } catch (_: Exception) {
                            -1
                        }
                    )
                } catch (_: Exception) { null }
            }
    } catch (e: Exception) {
        Log.e("FluxaExtractor", "Error extrayendo playlists: ${e.message}")
        emptyList()
    }
}

// Extracción de shorts de emergencia
fun extractShortsWithFallback(
    service: StreamingService,
    tabs: List<ListLinkHandler>,
    channelName: String,
    cleanAvatar: String,
    channelId: String = ""
): List<FluxaStreamItem> {

    Log.d("FluxaExtractor", "Extrayendo shorts por pestaña...")
    val shortsNativos = extractStreamsFromTab(service, tabs, "/shorts", "Shorts", channelName, cleanAvatar)

    if (shortsNativos.isNotEmpty()) {
        Log.d("FluxaExtractor", "Se recuperaron ${shortsNativos.size} Shorts de forma nativa.")
        return shortsNativos
    }

    return try {
        val queryAlternativa = "$channelName shorts"
        val searchHandler = service.searchQHFactory.fromQuery(queryAlternativa, listOf("videos"), "")
        val searchExtractor = service.getSearchExtractor(searchHandler)
        searchExtractor.fetchPage()

        val searchItems = searchExtractor.initialPage.items ?: emptyList()

        val shortsFiltrados = searchItems
            .filterIsInstance<StreamInfoItem>()
            .filter { item ->
                val isSameUploader = item.uploaderName?.trim()?.equals(channelName.trim(), ignoreCase = true) == true
                val isShortDuration = item.duration <= 61
                val isPublic = item.contentAvailability == ContentAvailability.AVAILABLE
                
                val isAgeRestricted = try {
                    item.javaClass.getMethod("isAgeLimit").invoke(item) as Boolean
                } catch (_: Exception) { false }
                
                val streamType = item.streamType?.name ?: ""
                val isPlayable = streamType != "NONE" && !streamType.contains("PREMIERE") && !streamType.contains("UPCOMING")
                val hasDuration = item.duration > 0L

                isSameUploader && isShortDuration && isPublic && !isAgeRestricted && isPlayable && hasDuration
            }
            .map { item ->
                FluxaStreamItem(
                    url = item.url ?: "",
                    title = item.name ?: "",
                    thumbnail = item.thumbnails.firstOrNull()?.url ?: "",
                    uploaderName = channelName,
                    views = item.viewCount,
                    duration = item.duration,
                    isLiveStream = false,
                    uploaderAvatar = cleanAvatar,
                    uploadDate = item.textualUploadDate ?: "",
                    timestamp = try { item.uploadDate?.instant?.toEpochMilli() ?: 0L } catch (_: Exception) { 0L },
                    channelId = channelId
                )
            }

        Log.d("FluxaExtractor", "Se rescataron ${shortsFiltrados.size} shorts")
        shortsFiltrados
    } catch (e: Exception) {
        Log.e("FluxaExtractor", "Fallo critico en el Fallback de Shorts: ${e.message}")
        emptyList()
    }
}