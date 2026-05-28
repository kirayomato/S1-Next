package me.ykrank.s1next.view.dialog.requestdialog

import android.os.Bundle
import io.reactivex.Single
import me.ykrank.s1next.BuildConfig
import me.ykrank.s1next.R
import me.ykrank.s1next.widget.uploadimg.ForumAttachmentPostSubmitHelper

/**
 * A dialog requests to reply to post.
 */
class NewThreadRequestDialogFragment : BaseRequestDialogFragment<PostSubmitResult>() {
    override fun getProgressMessage(): CharSequence? {
        return getText(R.string.dialog_progress_message_reply)
    }

    override fun getSourceObservable(): Single<PostSubmitResult> {
        val bundle = arguments
        val forumId = bundle!!.getInt(ARG_FORUM_ID)
        val title = bundle.getString(ARG_TITLE)
        val typeId = bundle.getString(ARG_TYPE_ID)
        val message = bundle.getString(ARG_MESSAGE)
        val normalizedMessage = ForumAttachmentPostSubmitHelper.normalizeMessage(message)
        if (ForumAttachmentPostSubmitHelper.hasForumAttachments(message)) {
            return webNewThread(forumId, typeId, title, normalizedMessage)
        }
        val saveAsDraft = if (BuildConfig.DEBUG) 1 else null
        return flatMappedWithAuthenticityToken { token: String ->
            mS1Service.newThread(
                forumId,
                token,
                System.currentTimeMillis(),
                typeId,
                title,
                message,
                1,
                1,
                saveAsDraft
            ).map { PostSubmitResult.fromAccountResult(it) }
        }
    }

    private fun webNewThread(
        forumId: Int,
        typeId: String?,
        title: String?,
        message: String?,
    ): Single<PostSubmitResult> {
        val formHashArg = requireArguments().getString(ARG_FORM_HASH)?.takeIf { it.isNotBlank() }
        return if (formHashArg != null) {
            submitWebNewThread(forumId, typeId, title, message, formHashArg)
        } else {
            flatMappedWithAuthenticityToken { token: String ->
                submitWebNewThread(forumId, typeId, title, message, token)
            }
        }
    }

    private fun submitWebNewThread(
        forumId: Int,
        typeId: String?,
        title: String?,
        message: String?,
        formHash: String,
    ): Single<PostSubmitResult> {
        val postTime = requireArguments().takeIf { it.containsKey(ARG_POST_TIME) }?.getLong(ARG_POST_TIME)
            ?: (System.currentTimeMillis() / 1000L)
        val saveAsDraft = if (BuildConfig.DEBUG) "1" else null
        val fields = linkedMapOf(
            "formhash" to formHash,
            "posttime" to postTime.toString(),
            "wysiwyg" to "1",
            "subject" to title.orEmpty(),
            "message" to message.orEmpty(),
            "allownoticeauthor" to "1",
            "usesig" to "1",
            "topicsubmit" to "yes",
        )
        fields.addIfNotBlank("typeid", typeId)
        fields.addIfNotBlank("save", saveAsDraft)
        ForumAttachmentPostSubmitHelper.appendAttachNewFields(
            fields,
            ForumAttachmentPostSubmitHelper.collectForumAttachmentIds(message)
        )
        val fallbackAction = "forum.php?mod=post&action=newthread&fid=$forumId&extra=&topicsubmit=yes"
        val submitUrl = ForumAttachmentPostSubmitHelper.webSubmitUrl(
            requireArguments().getString(ARG_FORM_ACTION),
            fallbackAction,
        )
        return mS1Service.submitPostForm(submitUrl, fields).map { PostSubmitResult.fromAjaxHtml(it) }
    }

    protected override fun onNext(data: PostSubmitResult) {
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
        val TAG = NewThreadRequestDialogFragment::class.java.getName()
        private const val ARG_FORUM_ID = "forum_id"
        private const val ARG_TYPE_ID = "type_id"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_CACHE_KEY = "cache_key"
        private const val ARG_FORM_ACTION = "form_action"
        private const val ARG_FORM_HASH = "form_hash"
        private const val ARG_POST_TIME = "post_time"
        fun newInstance(
            forumId: Int,
            typeId: String?,
            title: String?,
            message: String?,
            cacheKey: String?,
            formAction: String? = null,
            formHash: String? = null,
            postTime: Long? = null,
        ): NewThreadRequestDialogFragment {
            val fragment = NewThreadRequestDialogFragment()
            val bundle = Bundle()
            bundle.putInt(ARG_FORUM_ID, forumId)
            bundle.putString(ARG_TYPE_ID, typeId)
            bundle.putString(ARG_TITLE, title)
            bundle.putString(ARG_MESSAGE, message)
            bundle.putString(ARG_CACHE_KEY, cacheKey)
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
