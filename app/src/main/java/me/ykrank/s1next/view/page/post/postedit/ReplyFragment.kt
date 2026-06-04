package me.ykrank.s1next.view.page.post.postedit

import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.lifecycle.lifecycleScope
import com.github.ykrank.androidtools.extension.await
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.util.RxJavaUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.ykrank.s1next.data.api.S1Service
import me.ykrank.s1next.data.api.model.PostEditor
import me.ykrank.s1next.util.AppDeviceUtil
import me.ykrank.s1next.view.dialog.requestdialog.ReplyRequestDialogFragment
import me.ykrank.s1next.view.event.RequestDialogSuccessEvent
import me.ykrank.s1next.widget.uploadimg.ForumAttachmentUploadTarget
import javax.inject.Inject

/**
 * A Fragment shows [EditText] to let the user enter reply.
 */
@AndroidEntryPoint
class ReplyFragment : BasePostEditFragment() {
    override var cacheKey: String? = null
        private set

    private var mThreadId: String? = null
    private var mQuotePostId: String? = null
    private var mForumId: Int? = null
    private var mPageNum: Int = 1
    private var parsedReplyEditor: PostEditor? = null
    private var parsedForumAttachmentUploadInfo: PostEditor.ForumAttachmentUploadInfo? = null
    private var parsedForumAttachments: List<PostEditor.ForumAttachment> = emptyList()

    @Inject
    internal lateinit var mS1Service: S1Service

    override val forumAttachmentUploadTarget: ForumAttachmentUploadTarget?
        get() = mThreadId?.let { ForumAttachmentUploadTarget.Reply(it, mForumId, mPageNum) }

    override val forumAttachmentUploadInfo: PostEditor.ForumAttachmentUploadInfo?
        get() = parsedForumAttachmentUploadInfo

    override val forumAttachmentFormHash: String?
        get() = parsedReplyEditor?.formHash

    override val forumAttachments: List<PostEditor.ForumAttachment>
        get() = parsedForumAttachments

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bundle = requireArguments()
        mThreadId = bundle.getString(ARG_THREAD_ID)
        mQuotePostId = bundle.getString(ARG_QUOTE_POST_ID)
        mForumId = bundle.getInt(ARG_FORUM_ID, 0).takeIf { it > 0 }
        mPageNum = bundle.getInt(ARG_PAGE_NUM, 1).coerceAtLeast(1)
        super.onViewCreated(view, savedInstanceState)
        cacheKey = String.format(CACHE_KEY_PREFIX, mThreadId, mQuotePostId)
        leavePageMsg("ReplyFragment##mThreadId:$mThreadId,mQuotePostId$mQuotePostId,mForumId:$mForumId,mPageNum:$mPageNum")
        loadReplyEditorInfo()
    }

    override fun onMenuSendClick(): Boolean {
        val stringBuilder = StringBuilder(mReplyView.text)
        if (mGeneralPreferencesManager.isSignatureEnabled) {
            val signature = if (mGeneralPreferencesManager.isDeviceInfoShownInSignature) {
                AppDeviceUtil.getPostSignatureWithDeviceInfo(requireContext())
            } else {
                AppDeviceUtil.getPostSignature(requireContext())
            }
            stringBuilder.append("\n\n").append(signature)
        }

        ReplyRequestDialogFragment.newInstance(
            mThreadId,
            mQuotePostId,
            stringBuilder.toString(),
            parsedReplyEditor?.noticeAuthor,
            parsedReplyEditor?.noticeTrimStr,
            parsedReplyEditor?.noticeAuthorMsg,
            mForumId,
            parsedReplyEditor?.formAction,
            parsedReplyEditor?.formHash,
            parsedReplyEditor?.postTime,
        ).show(childFragmentManager,
                ReplyRequestDialogFragment.TAG)

        return true
    }

    override fun isRequestDialogAccept(event: RequestDialogSuccessEvent): Boolean {
        return event.dialogFragment is ReplyRequestDialogFragment
    }

    private fun loadReplyEditorInfo() {
        val threadId = mThreadId ?: return
        lifecycleScope.launch {
            try {
                val postEditor = mS1Service.getReplyEditorInfo(threadId, mQuotePostId)
                    .map<PostEditor> { PostEditor.fromHtml(it) }
                    .compose(RxJavaUtil.iOSingleTransformer())
                    .await()
                parsedReplyEditor = postEditor
                parsedForumAttachmentUploadInfo = postEditor.forumAttachmentUploadInfo
                parsedForumAttachments = postEditor.forumAttachments
                notifyForumAttachmentsChanged()
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) return@launch
                L.report(throwable)
                showRetrySnackbar(throwable, View.OnClickListener { loadReplyEditorInfo() })
            }
        }
    }

    companion object {

        val TAG: String = ReplyFragment::class.java.simpleName

        private const val ARG_THREAD_ID = "thread_id"
        private const val ARG_QUOTE_POST_ID = "quote_post_id"
        private const val ARG_FORUM_ID = "forum_id"
        private const val ARG_PAGE_NUM = "page_num"

        private val CACHE_KEY_PREFIX = "NewReply_%s_%s"

        fun newInstance(threadId: String, quotePostId: String?, forumId: Int?, pageNum: Int): ReplyFragment {
            val fragment = ReplyFragment()
            val bundle = Bundle()
            bundle.putString(ARG_THREAD_ID, threadId)
            bundle.putString(ARG_QUOTE_POST_ID, quotePostId)
            if (forumId != null && forumId > 0) {
                bundle.putInt(ARG_FORUM_ID, forumId)
            }
            bundle.putInt(ARG_PAGE_NUM, pageNum.coerceAtLeast(1))
            fragment.arguments = bundle

            return fragment
        }
    }
}
