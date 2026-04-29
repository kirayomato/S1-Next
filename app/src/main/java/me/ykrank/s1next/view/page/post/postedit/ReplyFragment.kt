package me.ykrank.s1next.view.page.post.postedit

import android.os.Bundle
import android.view.View
import android.widget.EditText
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

        ReplyRequestDialogFragment.newInstance(mThreadId, mQuotePostId,
                stringBuilder.toString()).show(childFragmentManager,
                ReplyRequestDialogFragment.TAG)

        return true
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
