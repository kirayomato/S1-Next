package me.ykrank.s1next.view.page.post.share

import me.ykrank.s1next.data.api.model.Post

data class PostShareRequest(
    val threadId: String,
    val threadTitle: String?,
    val page: Int,
    val posts: List<Post>,
)
