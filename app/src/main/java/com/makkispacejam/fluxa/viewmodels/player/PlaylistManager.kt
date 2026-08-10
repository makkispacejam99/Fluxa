package com.makkispacejam.fluxa.viewmodels.player

import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.data.newpipe.FluxaStreamItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class PlaylistManager(
    private val scope: CoroutineScope,
    private val onPlayVideo: suspend (String, String, String, String, Boolean) -> Unit,
    private val onClosePlayer: () -> Unit,
    private val getState: () -> PlaybackData,
    private val updateState: (PlaybackData) -> Unit
) {
    fun playPlaylist(videos: List<FluxaStreamItem>, startIndex: Int, shuffle: Boolean = false, resetProgress: Boolean = false) {
        val queue = videos.map {
            QueueItem(VideoExtractor.cleanVideoId(it.url), it.title, it.uploaderName, it.thumbnail)
        }
        var finalQueue = queue
        var finalIndex = startIndex

        if (queue.isNotEmpty()) {
            if (shuffle) {
                val current = queue[startIndex]
                val remaining = queue.toMutableList().apply { removeAt(startIndex) }
                remaining.shuffle()
                finalQueue = listOf(current) + remaining
                finalIndex = 0
            } else {
                // Preservar orden original al no aleatorio
                finalIndex = startIndex
            }
        }
        updateState(getState().copy(playlistQueue = finalQueue, currentIndex = finalIndex, isShuffled = shuffle, resetProgress = resetProgress))
        val selected = finalQueue[finalIndex]
        scope.launch { onPlayVideo(selected.videoId, selected.title, selected.channel, selected.thumbnailUrl, true) }
    }

    fun skipToNext() {
        val s = getState()
        if (s.playlistQueue.isEmpty()) return
        val nextIndex = s.currentIndex + 1
        if (nextIndex < s.playlistQueue.size) playFromQueue(nextIndex)
        else if (s.repeatMode == RepeatMode.ALL) playFromQueue(0)
    }

    fun skipToPrevious() {
        val s = getState()
        if (s.playlistQueue.isEmpty()) return
        val prevIndex = s.currentIndex - 1
        if (prevIndex >= 0) playFromQueue(prevIndex)
        else if (s.repeatMode == RepeatMode.ALL) playFromQueue(s.playlistQueue.size - 1)
    }

    fun playFromQueue(index: Int) {
        val s = getState()
        if (index !in s.playlistQueue.indices) return
        updateState(s.copy(currentIndex = index))
        val selected = s.playlistQueue[index]
        scope.launch { onPlayVideo(selected.videoId, selected.title, selected.channel, selected.thumbnailUrl, true) }
    }

    fun removeFromQueue(index: Int) {
        val s = getState()
        val currentQueue = s.playlistQueue.toMutableList()
        if (index !in currentQueue.indices) return
        val isPlayingRemoved = index == s.currentIndex
        currentQueue.removeAt(index)
        var newIndex = s.currentIndex
        if (isPlayingRemoved) {
            if (currentQueue.isEmpty()) { onClosePlayer(); return }
            newIndex = if (index < currentQueue.size) index else 0
            updateState(s.copy(playlistQueue = currentQueue, currentIndex = newIndex))
            playFromQueue(newIndex)
        } else {
            if (index < s.currentIndex) newIndex--
            updateState(s.copy(playlistQueue = currentQueue, currentIndex = newIndex))
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val s = getState()
        val currentQueue = s.playlistQueue.toMutableList()
        if (fromIndex !in currentQueue.indices || toIndex !in currentQueue.indices) return
        val item = currentQueue.removeAt(fromIndex)
        currentQueue.add(toIndex, item)
        var newIndex = s.currentIndex
        if (fromIndex == s.currentIndex) newIndex = toIndex
        else if (s.currentIndex in (fromIndex + 1)..toIndex) newIndex--
        else if (s.currentIndex in toIndex..<fromIndex) newIndex++
        updateState(s.copy(playlistQueue = currentQueue, currentIndex = newIndex))
    }

    fun toggleShuffle() {
        val s = getState()
        if (s.playlistQueue.isEmpty()) return
        val isNowShuffled = !s.isShuffled
        val newQueue = if (isNowShuffled) {
            val current = s.playlistQueue.find { it.videoId == s.currentVideoId } ?: s.playlistQueue[s.currentIndex]
            listOf(current) + s.playlistQueue.filter { it.videoId != s.currentVideoId }.shuffled()
        } else s.playlistQueue.shuffled()
        updateState(s.copy(playlistQueue = newQueue, currentIndex = 0, isShuffled = isNowShuffled))
    }

    fun toggleRepeatMode() {
        val s = getState()
        updateState(s.copy(repeatMode = when (s.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }))
    }
}
