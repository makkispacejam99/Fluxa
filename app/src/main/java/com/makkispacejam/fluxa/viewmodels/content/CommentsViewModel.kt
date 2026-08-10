package com.makkispacejam.fluxa.viewmodels.content

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.makkispacejam.fluxa.data.VideoExtractor
import com.makkispacejam.fluxa.data.avatars.AvatarRepository
import com.makkispacejam.fluxa.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.comments.CommentsInfo

class CommentsViewModel : ViewModel() {

    var commentList by mutableStateOf<List<CommentThread>>(emptyList())
        private set

    var isCommentsLoading by mutableStateOf(false)
        private set

    val avatarRepo = AvatarRepository(viewModelScope)
    private var lastLoadedCommentsVideoId: String = ""

    fun getAvatar(channelId: String, fallback: String?): String =
        avatarRepo.getAvatar(channelId, fallback ?: "")

    fun fetchVideoComments(videoId: String) {
        if (videoId == lastLoadedCommentsVideoId && commentList.isNotEmpty()) return

        isCommentsLoading = true
        commentList = emptyList()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val service = ServiceList.YouTube
                val cleanId = VideoExtractor.cleanVideoId(videoId)
                val videoUrl = "https://www.youtube.com/watch?v=$cleanId"
                val commentsInfo = CommentsInfo.getInfo(service, videoUrl)
                val mappedComments = commentsInfo.relatedItems.filterIsInstance<org.schabi.newpipe.extractor.comments.CommentsInfoItem>().map { item ->
                    val channelId = item.uploaderUrl?.substringAfterLast("/") ?: ""
                    val avatarUrl = item.thumbnails.maxByOrNull { it.height }?.url
                        ?: item.uploaderAvatars.maxByOrNull { it.height }?.url
                        ?: ""
                    CommentThread(
                        snippet = ThreadSnippet(
                            topLevelComment = TopLevelComment(
                                id = item.url ?: "",
                                snippet = CommentSnippet(
                                    authorDisplayName = item.uploaderName ?: "Usuario",
                                    authorProfileImageUrl = avatarUrl,
                                    authorChannelId = channelId,
                                    textDisplay = item.commentText.content,
                                    likeCount = item.likeCount.coerceAtLeast(0),
                                    publishedAt = item.textualUploadDate ?: ""
                                )
                            )
                        ),
                        replies = null
                    )
                }

                withContext(Main) {
                    lastLoadedCommentsVideoId = videoId
                    commentList = mappedComments
                }
            } catch (e: Exception) {
                Log.e("FluxaComments", "Error", e)
                withContext(Main) { commentList = emptyList() }
            } finally {
                withContext(Main) { isCommentsLoading = false }
            }
        }
    }
}
