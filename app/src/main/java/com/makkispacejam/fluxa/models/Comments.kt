package com.makkispacejam.fluxa.models

import com.google.gson.annotations.SerializedName

/* Obtención de datos de
comentarios y respuestas*/

data class CommentThread(
    @SerializedName("snippet") val snippet: ThreadSnippet? = null,
    @SerializedName("replies") val replies: CommentReplies? = null
)

data class ThreadSnippet(
    @SerializedName("topLevelComment") val topLevelComment: TopLevelComment? = null
)

data class TopLevelComment(
    @SerializedName("id") val id: String = "",
    @SerializedName("snippet") val snippet: CommentSnippet? = null
)

data class ReplyCommentItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("snippet") val snippet: CommentSnippet? = null
)

data class CommentReplies(
    @SerializedName("comments") val comments: List<ReplyCommentItem>? = emptyList()
)

data class CommentSnippet(
    @SerializedName("authorDisplayName") val authorDisplayName: String = "Usuario",
    @SerializedName("authorProfileImageUrl") val authorProfileImageUrl: String? = null,
    @SerializedName("authorChannelId") val authorChannelId: String = "",
    @SerializedName("textDisplay") val textDisplay: String = "",
    @SerializedName("likeCount") val likeCount: Int = 0,
    @SerializedName("publishedAt") val publishedAt: String = ""
)
