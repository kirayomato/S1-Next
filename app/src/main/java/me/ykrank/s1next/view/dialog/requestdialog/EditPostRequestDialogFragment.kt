package me.ykrank.s1next.view.dialog.requestdialog

import android.os.Bundle
import io.reactivex.Single
import me.ykrank.s1next.BuildConfig
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.widget.uploadimg.ForumAttachmentPostSubmitHelper

/**
 * A dialog requests to reply to post.
 */
class EditPostRequestDialogFragment : BaseRequestDialogFragment<PostSubmitResult>() {

    override fun getProgressMessage(): CharSequence? {
        return getText(R.string.dialog_progress_message_reply)
    }

    override fun getSourceObservable(): Single<PostSubmitResult> {
        val bundle = requireArguments()
        val mThread = bundle.getParcelable<Thread>(ARG_THREAD)
        val mPost = bundle.getParcelable<Post>(ARG_POST)
        val title = bundle.getString(ARG_TITLE)
        val typeId = bundle.getString(ARG_TYPE_ID)
        val readPerm = bundle.getString(ARG_READ_PERM)
        val message = bundle.getString(ARG_MESSAGE)
        val normalizedMessage = ForumAttachmentPostSubmitHelper.normalizeMessage(message)

        if (mPost == null || mThread == null) {
            return Single.error(NullPointerException())
        }

        if (ForumAttachmentPostSubmitHelper.hasForumAttachments(message)) {
            return webEditPost(mThread, mPost, typeId, readPerm, title, normalizedMessage)
        }

        val saveAsDraft = if (BuildConfig.DEBUG && mPost.isFirst) 1 else null
        return flatMappedWithAuthenticityToken { token ->
            mS1Service.editPost(mThread.fid!!.toInt(), mThread.id!!.toInt(), mPost.id, token, System.currentTimeMillis(),
                typeId,
                title,
                message,
                1,
                1,
                saveAsDraft,
                readPerm
            ).map {
                PostSubmitResult.fromAjaxHtml(it)
            }
        }
    }

    private fun webEditPost(
        thread: Thread,
        post: Post,
        typeId: String?,
        readPerm: String?,
        title: String?,
        message: String?,
    ): Single<PostSubmitResult> {
        val formHashArg = requireArguments().getString(ARG_FORM_HASH)?.takeIf { it.isNotBlank() }
        return if (formHashArg != null) {
            submitWebEditPost(thread, post, typeId, readPerm, title, message, formHashArg)
        } else {
            flatMappedWithAuthenticityToken { token ->
                submitWebEditPost(thread, post, typeId, readPerm, title, message, token)
            }
        }
    }

    private fun submitWebEditPost(
        thread: Thread,
        post: Post,
        typeId: String?,
        readPerm: String?,
        title: String?,
        message: String?,
        formHash: String,
    ): Single<PostSubmitResult> {
        val fid = thread.fid!!.toInt()
        val tid = thread.id!!.toInt()
        val postTime = requireArguments().takeIf { it.containsKey(ARG_POST_TIME) }?.getLong(ARG_POST_TIME)
            ?: (System.currentTimeMillis() / 1000L)
        val saveAsDraft = if (BuildConfig.DEBUG && post.isFirst) "1" else null
        val fields = linkedMapOf(
            "fid" to fid.toString(),
            "tid" to tid.toString(),
            "pid" to post.id.toString(),
            "formhash" to formHash,
            "posttime" to postTime.toString(),
            "wysiwyg" to "1",
            "subject" to title.orEmpty(),
            "message" to message.orEmpty(),
            "allownoticeauthor" to "1",
            "usesig" to "1",
            "editsubmit" to "yes",
            "delete" to "0",
        )
        fields.addIfNotBlank("typeid", typeId)
        fields.addIfNotBlank("readperm", readPerm)
        fields.addIfNotBlank("save", saveAsDraft)
        ForumAttachmentPostSubmitHelper.appendAttachNewFields(
            fields,
            ForumAttachmentPostSubmitHelper.collectForumAttachmentIds(message)
        )
        val fallbackAction = "forum.php?mod=post&action=edit&editsubmit=yes&inajax=yes&wysiwyg=1&delete=0"
        val submitUrl = ForumAttachmentPostSubmitHelper.webSubmitUrl(
            requireArguments().getString(ARG_FORM_ACTION),
            fallbackAction,
        )
        return mS1Service.submitPostForm(submitUrl, fields).map { PostSubmitResult.fromAjaxHtml(it) }
    }

    override fun onNext(data: PostSubmitResult) {
        if (data.success) {
            onRequestSuccess(getString(R.string.edit_post_succeed))
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

        val TAG: String = EditPostRequestDialogFragment::class.java.simpleName

        private const val ARG_THREAD = "thread"
        private const val ARG_POST = "post"
        private const val ARG_TYPE_ID = "type_id"
        private const val ARG_READ_PERM = "read_perm"
        private const val ARG_TITLE = "title"
        private const val ARG_MESSAGE = "message"
        private const val ARG_FORM_ACTION = "form_action"
        private const val ARG_FORM_HASH = "form_hash"
        private const val ARG_POST_TIME = "post_time"

        fun newInstance(thread: Thread, post: Post, typeId: String?, readPerm: String?, title: String,
                        message: String, formAction: String? = null, formHash: String? = null,
                        postTime: Long? = null): EditPostRequestDialogFragment {
            val fragment = EditPostRequestDialogFragment()
            val bundle = Bundle()
            bundle.putParcelable(ARG_THREAD, thread)
            bundle.putParcelable(ARG_POST, post)
            bundle.putString(ARG_TYPE_ID, typeId)
            bundle.putString(ARG_READ_PERM, readPerm)
            bundle.putString(ARG_TITLE, title)
            bundle.putString(ARG_MESSAGE, message)
            bundle.putString(ARG_FORM_ACTION, formAction)
            bundle.putString(ARG_FORM_HASH, formHash)
            if (postTime != null) {
                bundle.putLong(ARG_POST_TIME, postTime)
            }
            fragment.arguments = bundle

            return fragment
        }
    }
}
