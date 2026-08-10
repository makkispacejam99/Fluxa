package com.makkispacejam.fluxa.data.avatars

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.ChannelInfoItem

class AvatarRepository(private val scope: CoroutineScope) {

    private val avatarCache = mutableStateMapOf<String, String>()
    private val pendingFetches = mutableSetOf<String>()
    private val failedFetches = mutableMapOf<String, Long>()
    private val FAILED_RETRY_MS = 60_000L

    val cachedAvatars: Map<String, String> get() = avatarCache

    private fun isFailed(key: String): Boolean {
        val ts = failedFetches[key] ?: return false
        return System.currentTimeMillis() - ts < FAILED_RETRY_MS
    }

    fun getAvatar(channelId: String, fallback: String = ""): String {
        if (channelId.isEmpty()) return fallback
        avatarCache[channelId]?.let { return it }

        if (fallback.isNotEmpty() && (fallback.startsWith("http") || fallback.startsWith("//"))) {
            return fallback
        }

        if (pendingFetches.add(channelId)) {
            scope.launch(Dispatchers.IO) {
                try {
                    val avatar = AvatarUtils.fetchChannelAvatar(channelId)
                    if (!avatar.isNullOrEmpty()) {
                        withContext(Dispatchers.Main) { avatarCache[channelId] = avatar }
                    }
                } catch (_: Exception) {
                } finally {
                    withContext(Dispatchers.Main) { pendingFetches.remove(channelId) }
                }
            }
        }
        return fallback
    }

    fun getAvatarForChannel(channelName: String, channelId: String = ""): String {
        val cacheKey = channelId.ifEmpty { channelName.lowercase() }
        if (cacheKey.isEmpty()) return ""
        avatarCache[cacheKey]?.let { return it }
        if (isFailed(cacheKey)) return ""
        if (!pendingFetches.add(cacheKey)) return ""
        scope.launch(Dispatchers.IO) {
            try {
                val avatar = if (channelId.isNotEmpty()) {
                    AvatarUtils.fetchChannelAvatar(channelId)
                } else {
                    val service = ServiceList.YouTube
                    val cleanName = channelName.removePrefix("@").trim()
                    val searchHandler = service.searchQHFactory.fromQuery(cleanName, listOf("channels"), "")
                    val searchExtractor = service.getSearchExtractor(searchHandler)
                    searchExtractor.fetchPage()
                    val match = searchExtractor.initialPage.items
                        .asSequence()
                        .filterIsInstance<ChannelInfoItem>()
                        .firstOrNull { it.name?.trim().equals(cleanName, ignoreCase = true) }
                        ?: searchExtractor.initialPage.items
                            .asSequence()
                            .filterIsInstance<ChannelInfoItem>()
                            .firstOrNull()
                    val resolvedUrl = match?.url
                    if (!resolvedUrl.isNullOrEmpty()) AvatarUtils.fetchChannelAvatar(resolvedUrl) else null
                }
                if (!avatar.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { avatarCache[cacheKey] = avatar }
                } else {
                    withContext(Dispatchers.Main) { failedFetches[cacheKey] = System.currentTimeMillis() }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) { failedFetches[cacheKey] = System.currentTimeMillis() }
            } finally {
                withContext(Dispatchers.Main) { pendingFetches.remove(cacheKey) }
            }
        }
        return ""
    }

    fun getCachedAvatar(channelId: String): String? = avatarCache[channelId]

    suspend fun fetchAvatarForChannel(channelName: String, channelId: String = ""): String {
        val cacheKey = channelId.ifEmpty { channelName.lowercase() }
        if (cacheKey.isEmpty()) return ""
        avatarCache[cacheKey]?.let { return it }
        if (isFailed(cacheKey)) return ""
        if (!pendingFetches.add(cacheKey)) {
            repeat(40) {
                if (avatarCache[cacheKey] != null) return avatarCache[cacheKey]!!
                kotlinx.coroutines.delay(50)
            }
            return avatarCache[cacheKey] ?: ""
        }
        return try {
            val avatar = withContext(Dispatchers.IO) {
                if (channelId.isNotEmpty()) {
                    AvatarUtils.fetchChannelAvatar(channelId)
                } else {
                    val service = ServiceList.YouTube
                    val cleanName = channelName.removePrefix("@").trim()
                    val searchHandler = service.searchQHFactory.fromQuery(cleanName, listOf("channels"), "")
                    val searchExtractor = service.getSearchExtractor(searchHandler)
                    searchExtractor.fetchPage()
                    val match = searchExtractor.initialPage.items
                        .asSequence()
                        .filterIsInstance<ChannelInfoItem>()
                        .firstOrNull { it.name?.trim().equals(cleanName, ignoreCase = true) }
                        ?: searchExtractor.initialPage.items
                            .asSequence()
                            .filterIsInstance<ChannelInfoItem>()
                            .firstOrNull()
                    val resolvedUrl = match?.url
                    if (!resolvedUrl.isNullOrEmpty()) AvatarUtils.fetchChannelAvatar(resolvedUrl) else null
                }
            }
            if (!avatar.isNullOrEmpty()) {
                avatarCache[cacheKey] = avatar
                avatar
            } else {
                failedFetches[cacheKey] = System.currentTimeMillis()
                ""
            }
        } catch (_: Exception) {
            failedFetches[cacheKey] = System.currentTimeMillis()
            ""
        } finally {
            pendingFetches.remove(cacheKey)
        }
    }
}
