package me.ykrank.s1next.view.page.post.render

import android.content.Intent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.github.ykrank.androidtools.util.ClipboardUtil
import com.github.ykrank.androidtools.ui.internal.CoordinatorLayoutAnchorDelegate
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.view.dialog.PostCopyDialogFragment
import me.ykrank.s1next.view.dialog.ReportErrorDialogFragment

object PostRenderActions {

    fun showPostActionMenu(anchor: View, fragment: Fragment, thread: Thread?, pageNum: Int, post: Post): Boolean {
        val context = anchor.context
        val items = arrayOf(
            context.getString(R.string.post_action_copy),
            context.getString(R.string.menu_share),
            context.getString(R.string.post_action_feedback),
        )
        AlertDialog.Builder(context)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> PostCopyDialogFragment.newInstance(
                        post.authorName,
                        if (post.isTrade) post.extraHtml.orEmpty() else post.reply.orEmpty()
                    ).show(fragment.childFragmentManager, PostCopyDialogFragment.TAG)

                    1 -> sharePost(fragment, thread, pageNum, post)
                    2 -> feedbackPost(fragment, thread, pageNum, post)
                }
            }
            .show()
        return true
    }

    fun floorLink(thread: Thread?, pageNum: Int, post: Post): String {
        val threadId = thread?.id
        return Api.getPostListUrlForBrowser(threadId, pageNum) + "#pid${post.id}"
    }

    private fun sharePost(fragment: Fragment, thread: Thread?, pageNum: Int, post: Post) {
        val context = fragment.requireContext()
        val value = listOfNotNull(
            thread?.title,
            post.authorName?.let { "#${post.number} $it" },
            floorLink(thread, pageNum, post)
        ).joinToString("\n")
        val intent = Intent(Intent.ACTION_SEND)
        intent.putExtra(Intent.EXTRA_TEXT, value)
        intent.type = "text/plain"
        fragment.startActivity(Intent.createChooser(intent, context.getString(R.string.menu_title_share)))
    }

    private fun feedbackPost(fragment: Fragment, thread: Thread?, pageNum: Int, post: Post) {
        val context = fragment.requireContext()
        val link = floorLink(thread, pageNum, post)
        ClipboardUtil.copyText(context, "Post link", link)
        (fragment.activity as? CoordinatorLayoutAnchorDelegate)?.showSnackbar(
            R.string.post_feedback_link_copied
        )
        ReportErrorDialogFragment.newInstance(link)
            .show(fragment.parentFragmentManager, ReportErrorDialogFragment.TAG)
    }
}
