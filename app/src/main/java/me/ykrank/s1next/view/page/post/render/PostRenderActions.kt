package me.ykrank.s1next.view.page.post.render

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.github.ykrank.androidtools.util.ClipboardUtil
import com.github.ykrank.androidtools.ui.internal.CoordinatorLayoutAnchorDelegate
import com.github.ykrank.androidtools.util.ContextUtils
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.R
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.view.dialog.PostCopyDialogFragment
import me.ykrank.s1next.view.dialog.ReportErrorDialogFragment
import me.ykrank.s1next.view.event.EditPostEvent
import me.ykrank.s1next.view.event.EnterPostShareSelectionEvent
import me.ykrank.s1next.view.event.QuoteEvent
import me.ykrank.s1next.view.event.RateEvent
import me.ykrank.s1next.view.event.ReportEvent
import me.ykrank.s1next.view.page.post.postlist.PostListActivity

object PostRenderActions {

    fun showPostActionMenu(
        anchor: View,
        fragment: Fragment,
        eventBus: EventBus,
        user: User,
        thread: Thread?,
        pageNum: Int,
        post: Post
    ): Boolean {
        return showPostActionMenu(
            anchor = anchor,
            fragment = fragment,
            user = user,
            thread = thread,
            pageNum = pageNum,
            post = post,
            onReply = {
                val postId = post.id.toString()
                val count = post.number
                if (count != null) {
                    eventBus.postDefault(QuoteEvent(postId, count))
                }
            },
            onRate = {
                val tid = thread?.id
                val pid = post.id.toString()
                if (tid != null) {
                    eventBus.postDefault(RateEvent(tid, pid))
                }
            },
            onEdit = {
                if (thread != null) {
                    eventBus.postDefault(EditPostEvent(post, thread))
                }
            },
            onReport = {
                val tid = thread?.id
                val pid = post.id.toString()
                if (tid != null) {
                    eventBus.postDefault(ReportEvent(tid, pid, pageNum))
                }
            },
            onShare = {
                eventBus.postDefault(
                    EnterPostShareSelectionEvent(thread?.id, pageNum, post.id)
                )
            },
        )
    }

    fun showPostActionMenu(
        anchor: View,
        fragment: Fragment?,
        user: User,
        thread: Thread?,
        pageNum: Int,
        post: Post,
        onReply: (() -> Unit)?,
        onRate: (() -> Unit)?,
        onEdit: (() -> Unit)?,
        onReport: (() -> Unit)?,
        onShare: () -> Unit,
    ): Boolean {
        val topItems = mutableListOf<PostActionMenuPopup.Item>()
        if (onReply != null) {
            topItems.add(PostActionMenuPopup.Item(R.string.reply, R.drawable.ic_insert_comment_black_24dp, onReply))
        }
        if (onRate != null) {
            topItems.add(PostActionMenuPopup.Item(R.string.rate, R.drawable.ic_menu_grade_black_24dp, onRate))
        }
        topItems.add(
            PostActionMenuPopup.Item(R.string.post_action_copy, R.drawable.ic_content_copy_24dp) {
                showCopyDialog(anchor, fragment, post)
            }
        )

        val listItems = mutableListOf<PostActionMenuPopup.Item>()
        listItems.add(
            PostActionMenuPopup.Item(R.string.menu_share, R.drawable.ic_share_24dp, onShare)
        )
        val authorId = post.authorId
        if (thread != null && !authorId.isNullOrBlank()) {
            listItems.add(
                PostActionMenuPopup.Item(R.string.only_see_him, R.drawable.ic_search_24dp) {
                    PostListActivity.start(anchor.context, thread, authorId)
                }
            )
        }
        listItems.add(
            PostActionMenuPopup.Item(R.string.post_action_app_feedback, R.drawable.ic_feedback_24dp) {
                feedbackPost(anchor, fragment, thread, pageNum, post)
            }
        )
        if (onReport != null) {
            listItems.add(
                PostActionMenuPopup.Item(R.string.post_action_forum_report, R.drawable.ic_menu_advise_black_24dp, onReport)
            )
        }
        if (onEdit != null && user.isLogged && user.uid == post.authorId) {
            listItems.add(PostActionMenuPopup.Item(R.string.menu_edit, R.drawable.ic_create_content_24dp, onEdit))
        }
        PostActionMenuPopup.show(anchor, topItems, listItems)
        return true
    }

    private fun showCopyDialog(anchor: View, fragment: Fragment?, post: Post) {
        val fragmentManager = fragment?.childFragmentManager ?: findFragmentActivity(anchor)?.supportFragmentManager ?: return
        PostCopyDialogFragment.newInstance(
            post.authorName,
            if (post.isTrade) post.extraHtml.orEmpty() else post.reply.orEmpty()
        ).show(fragmentManager, PostCopyDialogFragment.TAG)
    }

    private fun findFragmentActivity(anchor: View): FragmentActivity? {
        return ContextUtils.getBaseContext(anchor.context) as? FragmentActivity
    }

    private fun findFeedbackFragmentManager(anchor: View, fragment: Fragment?): FragmentManager? {
        return fragment?.parentFragmentManager ?: findFragmentActivity(anchor)?.supportFragmentManager
    }

    fun floorLink(thread: Thread?, pageNum: Int, post: Post): String {
        val threadId = thread?.id
        return Api.getPostListUrlForBrowser(threadId, pageNum) + "#pid${post.id}"
    }

    private fun feedbackPost(anchor: View, fragment: Fragment?, thread: Thread?, pageNum: Int, post: Post) {
        val context = anchor.context
        val link = floorLink(thread, pageNum, post)
        ClipboardUtil.copyText(context, "Post link", link)
        (findFragmentActivity(anchor) as? CoordinatorLayoutAnchorDelegate)?.showSnackbar(
            R.string.post_feedback_link_copied
        )
        val fragmentManager = findFeedbackFragmentManager(anchor, fragment) ?: return
        ReportErrorDialogFragment.newInstance(link)
            .show(fragmentManager, ReportErrorDialogFragment.TAG)
    }
}
