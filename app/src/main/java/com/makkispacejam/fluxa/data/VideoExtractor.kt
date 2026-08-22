@file:Suppress("DEPRECATION", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.makkispacejam.fluxa.data

import android.util.Log
import com.makkispacejam.fluxa.data.newpipe.FluxaStreamItem
import com.makkispacejam.fluxa.data.newpipe.YouTubeDownloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

object VideoExtractor {

    fun cleanVideoId(rawId: String): String {
        return when {
            rawId.contains("v=") -> rawId.substringAfter("v=").substringBefore("&")
            rawId.contains("/shorts/") -> rawId.substringAfter("/shorts/").substringBefore("?")
            rawId.contains("youtu.be/") -> rawId.substringAfter("youtu.be/").substringBefore("?")
            rawId.contains("/") -> rawId.substringAfterLast("/")
            else -> rawId
        }
    }

    // obtener info de video
    suspend fun getStreamInfo(videoId: String): StreamInfo? = withContext(Dispatchers.IO) {
        val service = ServiceList.YouTube
        val cleanId = cleanVideoId(videoId)
        val url = "https://www.youtube.com/watch?v=$cleanId"

        try {
            return@withContext StreamInfo.getInfo(service, url)
        } catch (e: AgeRestrictedContentException) {
            throw e
        } catch (_: ContentNotAvailableException) {
            for (client in listOf("IOS", "WEB_REMIX")) {
                try {
                    YouTubeDownloader.overrideClient.set(client)
                    val info = StreamInfo.getInfo(service, url)
                    return@withContext info
                } catch (_: Exception) { }
            }
            null
        } catch (e: Exception) {
            Log.e("FluxaExtractor", "Error obteniendo StreamInfo para videoId: $videoId, reintentando con otros clientes", e)
            for (client in listOf("IOS", "WEB_REMIX")) {
                try {
                    YouTubeDownloader.overrideClient.set(client)
                    val info = StreamInfo.getInfo(service, url)
                    return@withContext info
                } catch (_: Exception) { }
            }
            null
        } finally {
            YouTubeDownloader.overrideClient.remove()
        }
    }

    // Buscar el archivo con mejor calidad
    fun findBestStream(streamInfo: StreamInfo, targetQuality: Int): String? {
        try {
            if (streamInfo.streamType == StreamType.LIVE_STREAM ||
                streamInfo.streamType == StreamType.AUDIO_LIVE_STREAM
            ) {
                if (!streamInfo.hlsUrl.isNullOrEmpty()) {
                    Log.d("FluxaExtractor", "Live detectado - reproduccion via HLS")
                    return streamInfo.hlsUrl
                }
            }

            val videoOnlyStreams = streamInfo.videoOnlyStreams
                ?.filter { it.url != null }
                ?: emptyList()

            val bestVideo = videoOnlyStreams
                .filter { parseResolution(it.resolution) <= targetQuality }
                .maxByOrNull { parseResolution(it.resolution) }
                ?: videoOnlyStreams.minByOrNull { parseResolution(it.resolution) }

            val systemLanguage = java.util.Locale.getDefault().language
            val audioStreams = streamInfo.audioStreams?.filter { it.url != null } ?: emptyList()
            
            val bestAudio = audioStreams
                .filter { it.audioLocale?.language == systemLanguage }
                .maxByOrNull { it.bitrate }
                ?: audioStreams.maxByOrNull { it.bitrate }

            if (bestVideo != null && bestAudio != null) {
                Log.d("FluxaExtractor", "DASH: ${bestVideo.resolution} | Target: ${targetQuality}p")
                return "${bestVideo.url}|${bestAudio.url}"
            }

            val progressiveStreams = streamInfo.videoStreams
                ?.filter { !it.isVideoOnly && it.url != null }
                ?: emptyList()

            val bestProgressive = progressiveStreams
                .filter { parseResolution(it.resolution) <= targetQuality }
                .maxByOrNull { parseResolution(it.resolution) }
                ?: progressiveStreams.maxByOrNull { parseResolution(it.resolution) }

            if (bestProgressive != null) {
                Log.d("FluxaExtractor", "Progressive fallback: ${bestProgressive.resolution}")
                return bestProgressive.url
            }
        } catch (e: Exception) {
            Log.e("FluxaExtractor", "Error encontrando el mejor stream", e)
        }
        return null
    }

    // Extracción de url de video
    suspend fun extractVideoUrl(videoId: String, targetQuality: Int = 720): String? =
        withContext(Dispatchers.IO) {
            try {
                val info = getStreamInfo(videoId) ?: return@withContext null
                findBestStream(info, targetQuality)
            } catch (e: AgeRestrictedContentException) {
                throw e
            } catch (e: Exception) {
                Log.e("FluxaExtractor", "Error extrayendo URL para $videoId: ${e.message}")
                null
            }
        }

    // Extracción de videos de playlist (todas las páginas)
    @Suppress("unused")
    suspend fun extractPlaylistVideos(playlistUrl: String, maxVideos: Int = 500): List<FluxaStreamItem> =
        withContext(Dispatchers.IO) {
            try {
                val service = ServiceList.YouTube
                val fullUrl = if (playlistUrl.startsWith("http")) playlistUrl 
                else "https://www.youtube.com$playlistUrl"
                
                val extractor = service.getPlaylistExtractor(fullUrl)
                extractor.fetchPage()

                val allItems = mutableListOf<FluxaStreamItem>()
                var currentPage: org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage<StreamInfoItem>? = extractor.initialPage
                var pageCount = 0

                while (currentPage != null && allItems.size < maxVideos && pageCount < 10) {
                    val pageItems = currentPage.items?.filterIsInstance<StreamInfoItem>() ?: emptyList()
                    for (item in pageItems) {
                        if (allItems.size >= maxVideos) break
                        val isAgeRestricted = try {
                            item.javaClass.getMethod("isAgeLimit").invoke(item) as Boolean
                        } catch (_: Exception) { false }
                        
                        val streamType = item.streamType?.name ?: ""
                        val isPlayable = streamType != "NONE" && !streamType.contains("PREMIERE") && !streamType.contains("UPCOMING")
                        val hasDuration = item.duration > 0L

                        if (item.contentAvailability == org.schabi.newpipe.extractor.stream.ContentAvailability.AVAILABLE && 
                            isPlayable && hasDuration && !isAgeRestricted) {
                            val url = item.url ?: ""
                            val vidId = cleanVideoId(url)
                            allItems.add(
                                FluxaStreamItem(
                                    url = vidId.ifEmpty { url },
                                    title = item.name ?: "",
                                    thumbnail = item.thumbnails.firstOrNull()?.url ?: "",
                                    uploaderName = item.uploaderName ?: "",
                                    views = item.viewCount,
                                    duration = item.duration,
                                    isLiveStream = item.streamType?.name == "LIVE",
                                    uploaderAvatar = "",
                                    uploadDate = item.textualUploadDate ?: "",
                                    channelId = item.uploaderUrl?.substringAfter("/channel/")?.substringBefore("/") ?: ""
                                )
                            )
                        }
                    }
                    currentPage = if (currentPage.hasNextPage() && allItems.size < maxVideos) {
                        try {
                            pageCount++
                            extractor.getPage(currentPage.nextPage)
                        } catch (_: Exception) { null }
                    } else null
                }

                // Validar disponibilidad real de cada video en paralelo
                val validatedItems = coroutineScope {
                    allItems.map { item ->
                        async {
                            val videoId = cleanVideoId(item.url)
                            if (videoId.isBlank()) return@async null
                            try {
                                val info = getStreamInfo(videoId)
                                if (info != null) item else null
                            } catch (_: ContentNotAvailableException) { null }
                            catch (_: AgeRestrictedContentException) { null }
                            catch (_: Exception) { item }
                        }
                    }.awaitAll().filterNotNull()
                }

                if (validatedItems.size < allItems.size) {
                    Log.i("FluxaExtractor", "Filtrados ${allItems.size - validatedItems.size} " +
                            "videos no disponibles de ${allItems.size}")
                }
                validatedItems
            } catch (e: Exception) {
                Log.e("FluxaExtractor", "Error extrayendo playlist: ${e.message}")
                emptyList()
            }
        }

    // parseo de resolución
    private fun parseResolution(resolutionStr: String?): Int {
        if (resolutionStr == null) return 0
        return try {
            val matches = Regex("\\d+").findAll(resolutionStr).toList()
            if (matches.isEmpty()) return 0

            if (resolutionStr.contains("x") && matches.size >= 2) {
                matches[1].value.toInt()
            } else {
                matches[0].value.toInt()
            }
        } catch (_: Exception) { 0 }
    }
}
