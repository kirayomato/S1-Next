package me.ykrank.s1next.view.page.post.share

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import me.ykrank.s1next.data.api.model.Post

@Parcelize
data class PostShareRequest(
    val threadId: String,
    val threadTitle: String?,
    val page: Int,
    val posts: ArrayList<Post>,
) : Parcelable
