package me.ykrank.s1next.view.event

data class EnterPostShareSelectionEvent(
    val threadId: String?,
    val pageNum: Int,
    val postId: Int,
)
