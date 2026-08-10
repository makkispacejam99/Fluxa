package com.makkispacejam.fluxa.models

data class VideoModel(
    val id: String,
    val title: String,
    val channelName: String,
    val channelId: String? = null,
    var channelAvatarUrl: String? = "",
    val imageUrl: String,
    val videoUrl: String,
    val timestamp: Long = 0L,
    val viewCount: Long = 0L,
)



