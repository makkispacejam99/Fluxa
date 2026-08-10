package com.makkispacejam.fluxa.data.newpipe

// Videos normales
data class FluxaStreamItem(
    val url: String,
    val title: String,
    val thumbnail: String,
    val uploaderName: String,
    val views: Long,
    val duration: Long,
    val isLiveStream: Boolean,
    val uploaderAvatar: String,
    val uploadDate: String = "",
    val timestamp: Long = 0L,
    val channelId: String = ""
)

// Playlist
data class FluxaPlaylistItem(
    val url: String,
    val name: String,
    val thumbnail: String,
    val videoCount: Int
)

// Shorts
data class FluxaChannelContainer(
    val id: String,
    val avatarUrl: String,
    val bannerUrl: String,
    val subscriberCount: Long,
    val streams: List<FluxaStreamItem>,
    val playlists: List<FluxaPlaylistItem> = emptyList(),
    val lives: List<FluxaStreamItem> = emptyList(),
    val shorts: List<FluxaStreamItem> = emptyList()
)