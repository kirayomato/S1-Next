package me.ykrank.s1next.view.page.post.share

data class PostShareSelectionState(
    val enabled: Boolean = false,
    val selectedPostIds: Set<Int> = emptySet(),
    val sourcePostId: Int? = null,
)

interface PostShareSelectionOwner {
    val postShareSelectionState: PostShareSelectionState

    fun togglePostShareSelection(postId: Int)
}
