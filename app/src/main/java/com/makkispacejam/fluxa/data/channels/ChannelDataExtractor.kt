@file:Suppress("DEPRECATION", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.makkispacejam.fluxa.data.channels

import android.util.Log
import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.data.newpipe.FluxaChannelContainer
import com.makkispacejam.fluxa.data.newpipe.FluxaStreamItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem
import org.schabi.newpipe.extractor.stream.StreamInfoItem

object ChannelDataExtractor {

    // Resolver url de canales
    suspend fun resolveChannelUrl(channelName: String): String? = withContext(Dispatchers.IO) {
        try {
            val service = ServiceList.YouTube
            val cleanName = channelName.removePrefix("@").trim()

            val searchHandler = service.searchQHFactory.fromQuery(cleanName, listOf("channels"), "")
            val searchExtractor = service.getSearchExtractor(searchHandler)
            searchExtractor.fetchPage()

            val channels = searchExtractor.initialPage.items.filterIsInstance<ChannelInfoItem>()

            if (channels.isEmpty()) {
                Log.w("FluxaExtractor", "No se encontró canal para: $cleanName")
                return@withContext null
            }

            val bestMatch = channels.firstOrNull { it.name?.trim().equals(cleanName, ignoreCase = true) }
                ?: channels.firstOrNull { it.name?.trim()?.contains(cleanName, ignoreCase = true) == true }
                ?: channels.first()

            Log.d("FluxaExtractor", "Canal seleccionado: '${bestMatch.name}' → ${bestMatch.url}")

            val channelExtractor = service.getChannelExtractor(bestMatch.url)
            channelExtractor.fetchPage()
            channelExtractor.url.also {
                Log.d("FluxaExtractor", "URL final: $it")
            }

        } catch (e: Exception) {
            Log.e("FluxaExtractor", "Error resolviendo URL: $channelName", e)
            null
        }
    }

    // Obtener datos de canal de manera limpia
    suspend fun getChannelDataClean(channelUrl: String): FluxaChannelContainer? =
        withContext(Dispatchers.IO) {
            try {
                val service = ServiceList.YouTube
                val channelExtractor = service.getChannelExtractor(channelUrl)
                channelExtractor.fetchPage()

                fun String.fixProtocol() = if (startsWith("//")) "https:$this" else this
                val cleanAvatar = channelExtractor.avatars.firstOrNull()?.url?.fixProtocol() ?: ""
                val cleanBanner = channelExtractor.banners.firstOrNull()?.url?.fixProtocol() ?: ""

                val tabs = channelExtractor.tabs
                Log.d("FluxaExtractor", "Tabs detectadas: ${tabs.map { it.url }}")

                val videos  = extractStreamsFromTab(
                    service,
                    tabs,
                    "/videos",
                    "Videos",
                    channelExtractor.name,
                    cleanAvatar
                )
                val lives   = extractStreamsFromTab(
                    service,
                    tabs,
                    "/streams",
                    "En Vivo",
                    channelExtractor.name,
                    cleanAvatar
                )
                val playlists = extractPlaylists(service, tabs)
                val shorts =
                    extractShortsWithFallback(service, tabs, channelExtractor.name, cleanAvatar)

                Log.d("FluxaExtractor", "Videos: ${videos.size} | Lives: ${lives.size} | Playlists: ${playlists.size}")

                FluxaChannelContainer(
                    id = channelExtractor.id,
                    avatarUrl = cleanAvatar,
                    bannerUrl = cleanBanner,
                    subscriberCount = channelExtractor.subscriberCount,
                    streams = videos,
                    playlists = playlists,
                    lives = lives,
                    shorts = shorts
                )

            } catch (e: Exception) {
                Log.e("FluxaExtractor", "Error crítico cargando canal: ${e.message}")
                e.printStackTrace()
                null
            }
        }

    // Obtener videos de canal
    suspend fun getChannelVideos(channelUrl: String): List<FluxaStreamItem> =
        withContext(Dispatchers.IO) {
            try {
                val service = ServiceList.YouTube
                val channelExtractor = service.getChannelExtractor(channelUrl)
                channelExtractor.fetchPage()

                val cleanAvatar = channelExtractor.avatars.firstOrNull()?.url ?: ""
                extractStreamsFromTab(
                    service,
                    channelExtractor.tabs,
                    "/videos",
                    "Videos",
                    channelExtractor.name,
                    cleanAvatar,
                    channelExtractor.id
                )
            } catch (_: Exception) {
                emptyList()
            }
        }

    // Obtener shorts de canal
    suspend fun getChannelShorts(channelUrl: String, channelName: String = "", channelId: String = "", channelAvatar: String = ""): List<FluxaStreamItem> =
        withContext(Dispatchers.IO) {
            val service = ServiceList.YouTube

            val cleanId = channelId.ifBlank {
                channelUrl.substringAfterLast("/channel/").substringBefore("/").substringBefore("?")
            }

            try {
                val channelExtractor = service.getChannelExtractor(channelUrl)
                channelExtractor.fetchPage()

                val cleanAvatar = channelAvatar.ifBlank { channelExtractor.avatars.firstOrNull()?.url ?: "" }
                val cleanName = channelName.ifBlank { channelExtractor.name }
                val extractorId = channelId.ifBlank { channelExtractor.id }

                val shortsTab = channelExtractor.tabs.firstOrNull { it.url?.contains("/shorts", ignoreCase = true) == true }
                if (shortsTab != null) {
                    val tabExtractor = service.getChannelTabExtractor(shortsTab)
                    tabExtractor.fetchPage()

                    val skipPages = (0..3).random()
                    var currentItems = tabExtractor.initialPage.items ?: emptyList()
                    var nextUrl = if (tabExtractor.initialPage.hasNextPage()) tabExtractor.initialPage.nextPage else null

                    var skipped = 0
                    while (skipped < skipPages && nextUrl != null) {
                        try {
                            val skipPage = tabExtractor.getPage(nextUrl)
                            currentItems = skipPage.items ?: emptyList()
                            nextUrl = if (skipPage.hasNextPage()) skipPage.nextPage else null
                            skipped++
                        } catch (_: Exception) { break }
                    }

                    val allItems = mutableListOf<org.schabi.newpipe.extractor.InfoItem>()
                    allItems.addAll(currentItems)

                    var pageAttempts = 0
                    while (allItems.size < 15 && pageAttempts < 2 && nextUrl != null) {
                        try {
                            val nextPage = tabExtractor.getPage(nextUrl)
                            nextPage.items?.let { allItems.addAll(it) }
                            nextUrl = if (nextPage.hasNextPage()) nextPage.nextPage else null
                            pageAttempts++
                        } catch (_: Exception) { break }
                    }

                    val shorts = allItems.filterIsInstance<StreamInfoItem>()
                        .filter { it.contentAvailability == org.schabi.newpipe.extractor.stream.ContentAvailability.AVAILABLE }
                        .map { item ->
                            val vidId = VideoExtractor.cleanVideoId(item.url ?: "")
                            FluxaStreamItem(
                                channelId = extractorId,
                                url = vidId,
                                title = item.name ?: "",
                                thumbnail = item.thumbnails.firstOrNull()?.url ?: "",
                                uploaderName = cleanName,
                                views = try { item.viewCount } catch (_: Exception) { 0L },
                                duration = try { item.duration } catch (_: Exception) { 0L },
                                isLiveStream = false,
                                uploaderAvatar = cleanAvatar,
                                uploadDate = item.textualUploadDate ?: "",
                                timestamp = try { item.uploadDate?.instant?.toEpochMilli() ?: 0L } catch (_: Exception) { 0L }
                            )
                        }
                        .filter { it.url.isNotBlank() }
                        .shuffled()
                    if (shorts.isNotEmpty()) return@withContext shorts
                }

                val videos = extractStreamsFromTab(
                    service,
                    channelExtractor.tabs,
                    "/videos",
                    "Videos",
                    cleanName,
                    cleanAvatar,
                    extractorId
                )
                val shortVideos = videos.filter { it.duration in 1..100 }
                if (shortVideos.isNotEmpty()) return@withContext shortVideos
            } catch (_: Exception) {}

            val nameForSearch = channelName.ifBlank {
                channelUrl.substringAfterLast("/").substringBefore("?").replace("channel/", "")
            }
            if (nameForSearch.isNotBlank()) {
                try {
                    val searchHandler = service.searchQHFactory.fromQuery("$nameForSearch shorts", listOf("videos"), "")
                    val searchExtractor = service.getSearchExtractor(searchHandler)
                    searchExtractor.fetchPage()
                    val searchItems = searchExtractor.initialPage.items ?: emptyList()
                    val allResults = searchItems.filterIsInstance<StreamInfoItem>()
                        .filter { it.contentAvailability == org.schabi.newpipe.extractor.stream.ContentAvailability.AVAILABLE }
                        .filter { try { it.duration in 1..100 } catch (_: Exception) { false } }

                    val queryNorm = nameForSearch.trim().lowercase()

                    val byId = allResults.filter { item ->
                        val itemChannelId = item.uploaderUrl?.substringAfter("/channel/")?.substringBefore("/")?.substringBefore("?") ?: ""
                        cleanId.isNotBlank() && itemChannelId == cleanId
                    }

                    val byName = if (byId.isEmpty()) {
                        allResults.filter { item ->
                            val uploader = (item.uploaderName ?: "").trim().lowercase()
                            uploader == queryNorm ||
                                uploader.startsWith("$queryNorm ") ||
                                uploader.endsWith(" $queryNorm") ||
                                (queryNorm.length >= 4 && uploader.contains(queryNorm))
                        }
                    } else emptyList()

                    val matched = byId + byName
                    val shorts = matched.map { item ->
                        val vidId = VideoExtractor.cleanVideoId(item.url ?: "")
                        val itemChannelId = item.uploaderUrl?.substringAfter("/channel/")?.substringBefore("/")?.substringBefore("?") ?: cleanId
                        FluxaStreamItem(
                            channelId = itemChannelId,
                            url = vidId,
                            title = item.name ?: "",
                            thumbnail = item.thumbnails.firstOrNull()?.url ?: "",
                            uploaderName = item.uploaderName ?: nameForSearch,
                            views = try { item.viewCount } catch (_: Exception) { 0L },
                            duration = try { item.duration } catch (_: Exception) { 0L },
                            isLiveStream = false,
                            uploaderAvatar = item.uploaderAvatars.firstOrNull()?.url ?: channelAvatar,
                            uploadDate = item.textualUploadDate ?: "",
                            timestamp = try { item.uploadDate?.instant?.toEpochMilli() ?: 0L } catch (_: Exception) { 0L }
                        )
                    }
                    .filter { it.url.isNotBlank() }
                    if (shorts.isNotEmpty()) return@withContext shorts
                } catch (_: Exception) {}
            }

            emptyList()
        }
}
