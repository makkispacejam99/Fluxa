package com.makkispacejam.fluxa.data.newpipe

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.makkispacejam.fluxa.data.channels.ChannelDataExtractor
import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.R

import kotlinx.coroutines.launch
import java.util.Locale

class ChannelViewModel(application: Application) : AndroidViewModel(application) {

    var channelId by mutableStateOf<String?>(null)
    var channelAvatarUrl by mutableStateOf<String?>(null)
    var channelBannerUrl by mutableStateOf<String?>(null)
    var subscriberCount by mutableStateOf("")
    var isLoadingHeader by mutableStateOf(false)
    var isLoadingContent by mutableStateOf(false)
    val videoList = mutableStateListOf<FluxaStreamItem>()
    val playlistList = mutableStateListOf<FluxaPlaylistItem>()
    val liveList = mutableStateListOf<FluxaStreamItem>()
    val shortsList = mutableStateListOf<FluxaStreamItem>()
    val selectedPlaylistVideos = mutableStateListOf<FluxaStreamItem>()

    // Carga del encabezado del canal
    fun loadChannelHeader(channelName: String) {
        val cleanName = channelName.removePrefix("@").trim()

        Log.d("FluxaDebug", "Resolviendo URL: $cleanName")

        channelId = null
        channelAvatarUrl = null
        channelBannerUrl = null
        subscriberCount = getApplication<Application>().getString(R.string.loading)
        videoList.clear()
        liveList.clear()
        playlistList.clear()
        shortsList.clear()
        isLoadingHeader = true
        isLoadingContent = true

        viewModelScope.launch {
            try {
                val resolvedUrl = ChannelDataExtractor.resolveChannelUrl(cleanName)

                if (resolvedUrl == null) {
                    Log.e("FluxaDebug", "No se pudo obtener la URL del canal")
                    subscriberCount = getApplication<Application>().getString(R.string.channel_not_found)
                    return@launch
                }

                Log.d("FluxaDebug", "URL obtenida: $resolvedUrl")

                val response = ChannelDataExtractor.getChannelDataClean(resolvedUrl)

                if (response != null) {
                    Log.d("FluxaDebug", "¡Extractor respondió con éxito! ID: ${response.id}")

                    channelId = response.id
                    channelAvatarUrl = response.avatarUrl.ifEmpty { null }
                    channelBannerUrl = response.bannerUrl.ifEmpty { null }

                    subscriberCount = if (response.subscriberCount > 0) {
                        when {
                            response.subscriberCount >= 1_000_000 -> "${
                                String.format(Locale.US, "%.1f", response.subscriberCount / 1_000_000f)
                            }M suscriptores"
                            response.subscriberCount >= 1_000 -> "${
                                String.format(Locale.US, "%.1f", response.subscriberCount / 1_000f)
                            }k suscriptores"
                            else -> "${response.subscriberCount} suscriptores"
                        }
                    } else {
                        "Contenido Ordenado"
                    }

                    response.streams.forEach { videoList.add(it) }
                    response.lives.forEach { liveList.add(it) }
                    response.playlists.forEach { playlistList.add(it) }
                    response.shorts.forEach { shortsList.add(it) }

                    Log.d("FluxaDebug", "Canal [$cleanName] cargado con éxito")
                } else {
                    Log.e("FluxaDebug", "Error: El extractor devolvió un valor nulo")
                    subscriberCount = getApplication<Application>().getString(R.string.error_connecting)
                }

            } catch (e: Exception) {
                Log.e("FluxaDebug", "Error crítico en ViewModel", e)
                subscriberCount = getApplication<Application>().getString(R.string.error_connecting)
            } finally {
                isLoadingHeader = false
                isLoadingContent = false
            }
        }
    }

    // Cargar playlist de videos
    fun loadPlaylistVideos(playlistUrl: String) {
        selectedPlaylistVideos.clear()
        isLoadingContent = true
        viewModelScope.launch {
            try {
                val videos = VideoExtractor.extractPlaylistVideos(playlistUrl)
                selectedPlaylistVideos.addAll(videos)
                Log.d("FluxaDebug", "Playlist cargada: ${videos.size} videos")
            } catch (e: Exception) {
                Log.e("FluxaDebug", "Error cargando playlist", e)
            } finally {
                isLoadingContent = false
            }
        }
    }
}
