package me.ykrank.s1next.view.dialog.requestdialog

import android.os.Bundle
import android.text.TextUtils
import com.github.ykrank.androidtools.util.StringUtils
import io.reactivex.Single
import me.ykrank.s1next.App.Companion.get
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.model.PostEditor
import me.ykrank.s1next.widget.track.event.NewReplyTrackEvent
import me.ykrank.s1next.widget.uploadimg.ForumAttachmentPostSubmitHelper

/**
 * A dialog requests to reply to post.
 */
class ReplyRequestDialogFragment : BaseRequestDialogFragment<PostSubmitResult>() {
    override fun getProgressMessage(): CharSequence? {
        return getText(R.string.dialog_progress_message_reply)
    }

    override fun getSourceObservable(): Single<PostSubmitResult> {
        val threadId = requireArguments().getString(ARG_THREAD_ID)
        val quotePostId = requireArguments().getString(ARG_QUOTE_POST_ID)
        val reply = requireArguments().getString(ARG_REPLY)
        val normalizedReply = ForumAttachmentPostSubmitHelper.normalizeMessage(reply)
        if (ForumAttachmentPostSubmitHelper.hasForumAttachments(reply)) {
            return webReply(threadId, quotePostId, normalizedReply)
        }
        return if (TextUtils.isEmpty(quotePostId)) {
            flatMappedWithAuthenticityToken { s: String ->
                mS1Service.reply(s, threadId, reply).map { PostSubmitResult.fromAccountResult(it) }
            }
        } else {
            val noticeAuthor = requireArguments().getString(ARG_NOTICE_AUTHOR)
            val noticeTrimStr = requireArguments().getString(ARG_NOTICE_TRIM_STR)
            val quoteInfo = if (!noticeAuthor.isNullOrEmpty() && !noticeTrimStr.isNullOrEmpty()) {
                Single.just(noticeAuthor to noticeTrimStr)
            } else {
                mS1Service.getReplyEditorInfo(threadId, quotePostId).map { s ->
                    val postEditor = PostEditor.fromHtml(s)
                    val author = postEditor.noticeAuthor
                    val trimStr = postEditor.noticeTrimStr
                    if (author.isNullOrEmpty() || trimStr.isNullOrEmpty()) {
                        throw IllegalStateException("Cannot get the post information.")
                    }
                    author to trimStr
                }
            }
            quoteInfo.flatMap { quote ->
                flatMappedWithAuthenticityToken { token: String ->
                    mS1Service.replyQuote(
                        token, threadId, reply, quote.first,
                        quote.second, StringUtils.abbreviate(
                            reply,
                            Api.REPLY_NOTIFICATION_MAX_LENGTH
                        )
                    ).map { PostSubmitResult.fromAccountResult(it) }
                }
            }
        }
    }

    private fun webReply(
        threadId: String?,
        quotePostId: String?,
        reply: String?,
    ): Single<PostSubmitResult> {
        val safeThreadId = threadId ?: return Single.error(IllegalStateException("Cannot get thread id."))
        val formHashArg = requireArguments().getString(ARG_FORM_HASH)
        val formActionArg = requireArguments().getString(ARG_FORM_ACTION)
        val postTimeArg = requireArguments().takeIf { it.containsKey(ARG_POST_TIME) }?.getLong(ARG_POST_TIME)
        val needsEditor = formHashArg.isNullOrEmpty() ||
                (!quotePostId.isNullOrEmpty() &&
                        (requireArguments().getString(ARG_NOTICE_AUTHOR).isNullOrEmpty() ||
                                requireArguments().getString(ARG_NOTICE_TRIM_STR).isNullOrEmpty()))
        val editorSingle = if (needsEditor) {
            mS1Service.getReplyEditorInfo(safeThreadId, quotePostId).map { PostEditor.fromHtml(it) }
        } else {
            Single.just(PostEditor())
        }
        return editorSingle.flatMap { editor ->
            val formHash = formHashArg?.takeIf { it.isNotBlank() }
                ?: editor.formHash
            if (formHash.isNullOrEmpty()) {
                flatMappedWithAuthenticityToken { token: String ->
                    submitWebReply(safeThreadId, quotePostId, reply, token, editor, formActionArg, postTimeArg)
                }
            } else {
                submitWebReply(safeThreadId, quotePostId, reply, formHash, editor, formActionArg, postTimeArg)
            }
        }
    }

    private fun submitWebReply(
        threadId: String,
        quotePostId: String?,
        reply: String?,
        formHash: String,
        editor: PostEditor,
        formActionArg: String?,
        postTimeArg: Long?,
    ): Single<PostSubmitResult> {
        val forumId = requireArguments().getInt(ARG_FORUM_ID, 0).takeIf { it > 0 }
            ?: editor.forumAttachmentUploadInfo?.fid
        val fallbackAction = buildString {
            append("forum.php?mod=post&action=reply")
            forumId?.let { append("&fid=").append(it) }
            append("&tid=").append(threadId)
            append("&extra=&replysubmit=yes")
        }
        val fields = linkedMapOf(
            "formhash" to formHash,
            "posttime" to (postTimeArg ?: editor.postTime ?: (System.currentTimeMillis() / 1000L)).toString(),
            "wysiwyg" to "1",
            "subject" to "",
            "message" to reply.orEmpty(),
            "usesig" to "1",
            "replysubmit" to "yes",
        )
        if (!quotePostId.isNullOrEmpty()) {
            val noticeAuthor = requireArguments().getString(ARG_NOTICE_AUTHOR) ?: editor.noticeAuthor
            val noticeTrimStr = requireArguments().getString(ARG_NOTICE_TRIM_STR) ?: editor.noticeTrimStr
            val noticeAuthorMsg = requireArguments().getString(ARG_NOTICE_AUTHOR_MSG)
                ?: editor.noticeAuthorMsg
                ?: StringUtils.abbreviate(reply, Api.REPLY_NOTIFICATION_MAX_LENGTH)
            fields.addIfNotBlank("noticeauthor", noticeAuthor)
            fields.addIfNotBlank("noticetrimstr", noticeTrimStr)
            fields.addIfNotBlank("noticeauthormsg", noticeAuthorMsg)
        }
        ForumAttachmentPostSubmitHelper.appendAttachNewFields(
            fields,
            ForumAttachmentPostSubmitHelper.collectForumAttachmentIds(reply)
        )
        val submitUrl = ForumAttachmentPostSubmitHelper.webSubmitUrl(
            formActionArg?.takeIf { it.isNotBlank() } ?: editor.formAction,
            fallbackAction,
        )
        return mS1Service.submitPostForm(submitUrl, fields).map { PostSubmitResult.fromAjaxHtml(it) }
    }

    override fun onNext(data: PostSubmitResult) {
        if (data.success) {
            onRequestSuccess(data.message)
        } else {
            onRequestError(data.message)
        }
    }

    private fun MutableMap<String, String>.addIfNotBlank(key: String, value: String?) {
        if (!value.isNullOrBlank()) {
            put(key, value)
        }
    }

    companion object {
        val TAG: String = ReplyRequestDialogFragment::class.java.getName()
        private const val ARG_THREAD_ID = "thread_id"
        private const val ARG_REPLY = "reply"
        private const val ARG_QUOTE_POST_ID = "quote_post_id"
        private const val ARG_NOTICE_AUTHOR = "notice_author"
        private const val ARG_NOTICE_TRIM_STR = "notice_trim_str"
        private const val ARG_NOTICE_AUTHOR_MSG = "notice_author_msg"
        private const val ARG_FORUM_ID = "forum_id"
        private const val ARG_FORM_ACTION = "form_action"
        private const val ARG_FORM_HASH = "form_hash"
        private const val ARG_POST_TIME = "post_time"
        fun newInstance(
            threadId: String?,
            quotePostId: String?,
            reply: String?,
            noticeAuthor: String? = null,
            noticeTrimStr: String? = null,
            noticeAuthorMsg: String? = null,
            forumId: Int? = null,
            formAction: String? = null,
            formHash: String? = null,
            postTime: Long? = null,
        ): ReplyRequestDialogFragment {
            get().trackAgent.post(NewReplyTrackEvent(threadId, quotePostId))
            val fragment = ReplyRequestDialogFragment()
            val bundle = Bundle()
            bundle.putString(ARG_THREAD_ID, threadId)
            bundle.putString(ARG_QUOTE_POST_ID, quotePostId)
            bundle.putString(ARG_REPLY, reply)
            bundle.putString(ARG_NOTICE_AUTHOR, noticeAuthor)
            bundle.putString(ARG_NOTICE_TRIM_STR, noticeTrimStr)
            bundle.putString(ARG_NOTICE_AUTHOR_MSG, noticeAuthorMsg)
            if (forumId != null && forumId > 0) {
                bundle.putInt(ARG_FORUM_ID, forumId)
            }
            bundle.putString(ARG_FORM_ACTION, formAction)
            bundle.putString(ARG_FORM_HASH, formHash)
            if (postTime != null) {
                bundle.putLong(ARG_POST_TIME, postTime)
            }
            fragment.setArguments(bundle)
            return fragment
        }
    }
}
