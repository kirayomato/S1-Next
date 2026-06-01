package me.ykrank.s1next.view.page.post.share

import android.app.Dialog
import android.content.ClipData
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.lifecycle.lifecycleScope
import com.github.ykrank.androidtools.util.L
import kotlinx.coroutines.launch
import me.ykrank.s1next.R
import me.ykrank.s1next.view.dialog.BaseDialogFragment

class PostSharePreviewDialogFragment : BaseDialogFragment() {

    private var request: PostShareRequest? = null
    private var shareCardView: View? = null
    private var shareActionView: View? = null
    private lateinit var renderer: PostShareCardRenderer

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()
        renderer = PostShareCardRenderer(context)
        val request = readRequest()
        this.request = request
        val root = layoutInflater.inflate(R.layout.dialog_post_share_preview, null, false)
        root.findViewById<View>(R.id.post_share_action_cancel).setOnClickListener {
            dismissAllowingStateLoss()
        }
        shareActionView = root.findViewById<View>(R.id.post_share_action_share).apply {
            setOnClickListener {
                shareImage()
            }
        }
        if (request != null) {
            val cardView = renderer.createView(request)
            shareCardView = cardView
            root.findViewById<FrameLayout>(R.id.post_share_preview_container).addView(
                cardView,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }
        return Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(root)
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            window.setGravity(Gravity.BOTTOM)
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window.attributes = window.attributes.apply {
                dimAmount = 0.45f
            }
        }
        if (request == null) {
            showToastText(getString(R.string.post_share_image_failed))
            dismissAllowingStateLoss()
        }
    }

    private fun readRequest(): PostShareRequest? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireArguments().getParcelable(ARG_REQUEST, PostShareRequest::class.java)
        } else {
            @Suppress("DEPRECATION")
            requireArguments().getParcelable(ARG_REQUEST)
        }
    }

    private fun shareImage() {
        val request = request ?: return
        val view = shareCardView ?: return
        shareActionView?.isEnabled = false
        lifecycleScope.launch {
            try {
                val uri = renderer.renderViewToUri(request.threadId, view)
                parentFragmentManager.setFragmentResult(
                    RESULT_REQUEST_KEY,
                    Bundle().apply {
                        putBoolean(RESULT_SHARED, true)
                    }
                )
                dismissAllowingStateLoss()
                shareImage(uri)
            } catch (e: Exception) {
                L.report(e)
                showToastText(getString(R.string.post_share_image_failed))
                shareActionView?.isEnabled = true
            }
        }
    }

    private fun shareImage(uri: Uri) {
        val context = requireContext()
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/png"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.clipData = ClipData.newUri(context.contentResolver, "Post share image", uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(intent, getString(R.string.menu_title_share)))
    }

    companion object {
        val TAG: String = PostSharePreviewDialogFragment::class.java.simpleName
        const val RESULT_REQUEST_KEY = "post_share_preview_result"
        const val RESULT_SHARED = "shared"
        private const val ARG_REQUEST = "request"

        fun newInstance(request: PostShareRequest): PostSharePreviewDialogFragment {
            return PostSharePreviewDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_REQUEST, request)
                }
            }
        }
    }
}
