package me.ykrank.s1next.view.page.post.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Profile
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.Vote
import me.ykrank.s1next.view.adapter.BaseRecyclerViewAdapter
import me.ykrank.s1next.view.page.post.share.PostShareSelectionOwner

/**
 * This [RecyclerView.Adapter]
 * has another item type [FooterProgressAdapterDelegate]
 * in order to implement pull up to refresh.
 */
class PostListRecyclerViewAdapter(
    fragment: Fragment,
    context: Context,
    postShareSelectionOwner: PostShareSelectionOwner? = null
) :
    BaseRecyclerViewAdapter(context, true) {
    private val postAdapterDelegate = PostAdapterDelegate(fragment, context, postShareSelectionOwner)
    private val postBlackAdapterDelegate = PostBlackAdapterDelegate(fragment, context)

    init {
        addAdapterDelegate(postAdapterDelegate)
        addAdapterDelegate(postBlackAdapterDelegate)
    }

    fun setThreadInfo(threadInfo: Thread, pageNum: Int) {
        postAdapterDelegate.setThreadInfo(threadInfo, pageNum)
    }

    fun setVoteInfo(voteInfo: Vote?) {
        postAdapterDelegate.setVoteInfo(voteInfo)
    }

    fun notifyProfileChanged(uid: String, profile: Profile) {
        postAdapterDelegate.setAuthorProfile(uid, profile)
        dataSet.forEachIndexed { index, item ->
            val post = item as? Post ?: return@forEachIndexed
            if (post.authorId == uid) {
                notifyItemChanged(index, profile)
            }
        }
    }
}
