package me.ykrank.s1next.view.page.post.postedit

import android.os.Bundle
import android.view.View
import me.ykrank.s1next.util.AppDeviceUtil
import me.ykrank.s1next.view.dialog.requestdialog.ReplyRequestDialogFragment
import me.ykrank.s1next.view.event.RequestDialogSuccessEvent

class ReplyFragment : BasePostEditFragment() {
    override var cacheKey: String? = null
        private set

    private var mThreadId: String? = null
    private var mQuotePostId: String? = null
    private var mForumId: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val bundle = requireArguments()
        mThreadId = bundle.getString(ARG_THREAD_ID)
        mQuotePostId = bundle.getString(ARG_QUOTE_POST_ID)
        mForumId = bundle.getInt(ARG_FORUM_ID, 0)
        cacheKey = String.format(CACHE_KEY_PREFIX, mThreadId, mQuotePostId)
        leavePageMsg("ReplyFragment##mThreadId:$mThreadId,mQuotePostId$mQuotePostId,mForumId:$mForumId")
    }

    override fun getForumId(): Int = mForumId

    override fun getThreadId(): Int? = mThreadId?.toIntOrNull()

    override fun getPostId(): Int? = mQuotePostId?.toIntOrNull()

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

        // 收集附件ID
        val attachments = buildAttachmentsMap()

        ReplyRequestDialogFragment.newInstance(mThreadId, mQuotePostId,
                stringBuilder.toString(), attachments).show(childFragmentManager,
                ReplyRequestDialogFragment.TAG)

        return true
    }

    /**
     * 从已上传的图片中构建附件字段Map
     */
    private fun buildAttachmentsMap(): Map<String, String>? {
        val images = selectImages
        if (images.isEmpty()) return null

        val attachments = mutableMapOf<String, String>()
        val regex = """\[attachimg](\d+)\[/attachimg]""".toRegex()

        images.forEach { image ->
            val url = image.url
            if (!url.isNullOrEmpty()) {
                regex.find(url)?.groupValues?.get(1)?.let { aid ->
                    attachments["attachupdate[$aid]"] = ""
                    attachments["attachnew[$aid][description]"] = ""
                }
            }
        }

        return if (attachments.isEmpty()) null else attachments
    }

    override fun isRequestDialogAccept(event: RequestDialogSuccessEvent): Boolean {
        return event.dialogFragment is ReplyRequestDialogFragment
    }

    companion object {

        val TAG: String = ReplyFragment::class.java.simpleName

        private const val ARG_THREAD_ID = "thread_id"
        private const val ARG_QUOTE_POST_ID = "quote_post_id"
        private const val ARG_FORUM_ID = "forum_id"

        private val CACHE_KEY_PREFIX = "NewReply_%s_%s"

        fun newInstance(threadId: String, quotePostId: String?, forumId: Int = 0): ReplyFragment {
            val fragment = ReplyFragment()
            val bundle = Bundle()
            bundle.putString(ARG_THREAD_ID, threadId)
            bundle.putString(ARG_QUOTE_POST_ID, quotePostId)
            bundle.putInt(ARG_FORUM_ID, forumId)
            fragment.arguments = bundle

            return fragment
        }
    }
}
