package com.makkispacejam.fluxa.data.avatars

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfo

object AvatarUtils {

    suspend fun fetchChannelAvatar(channelId: String?): String? = withContext(Dispatchers.IO) {
        if (channelId.isNullOrEmpty()) return@withContext null

        val channelUrl = when {
            channelId.startsWith("http") -> channelId
            channelId.startsWith("@") -> "https://www.youtube.com/$channelId"
            else -> "https://www.youtube.com/channel/$channelId"
        }

        try {
            val info = ChannelInfo.getInfo(ServiceList.YouTube, channelUrl)
            info.avatars.firstOrNull()?.url
        } catch (e: Exception) {
            Log.e("FluxaAvatars", "Error fetching avatar via ChannelInfo for $channelId: ${e.message}")
            null
        }
    }
}
