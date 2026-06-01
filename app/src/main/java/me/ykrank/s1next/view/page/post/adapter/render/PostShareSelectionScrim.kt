package me.ykrank.s1next.view.page.post.adapter.render

import android.view.View
import me.ykrank.s1next.view.page.post.share.PostShareSelectionOwner

private const val SELECTED_SCRIM_ALPHA = 0.16f

internal fun View.bindPostShareSelectionScrim(
    postShareSelectionOwner: PostShareSelectionOwner?,
    postId: Int
) {
    val state = postShareSelectionOwner?.postShareSelectionState
    val selected = state?.enabled == true && state.selectedPostIds.contains(postId)
    visibility = if (selected) View.VISIBLE else View.GONE
    if (selected) {
        alpha = SELECTED_SCRIM_ALPHA
    }
}
