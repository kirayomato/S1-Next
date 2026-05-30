package me.ykrank.s1next.view.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import me.ykrank.s1next.R
import me.ykrank.s1next.databinding.DialogPostCopyBinding
import me.ykrank.s1next.widget.span.HtmlCompat
import org.jsoup.Jsoup

class PostCopyDialogFragment : BaseDialogFragment() {

    private lateinit var binding: DialogPostCopyBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        binding = DataBindingUtil.inflate(
            activity.layoutInflater,
            R.layout.dialog_post_copy,
            null,
            false
        )
        binding.postCopyContent.movementMethod = LinkMovementMethod.getInstance()
        binding.textOnly.setOnCheckedChangeListener { _, _ -> renderContent() }
        renderContent()

        return AlertDialog.Builder(activity)
            .setTitle(R.string.title_post_copy)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    private fun renderContent() {
        val html = requireArguments().getString(ARG_HTML).orEmpty()
        val placeholder = getString(R.string.post_copy_image_placeholder)
        if (binding.textOnly.isChecked) {
            binding.postCopyContent.text = toPlainText(html, placeholder)
        } else {
            binding.postCopyContent.text = HtmlCompat.fromHtml(toCopyHtml(html, placeholder))
        }
    }

    private fun toPlainText(html: String, placeholder: String): String {
        val document = Jsoup.parseBodyFragment(html)
        document.select("img").forEach {
            val src = it.attr("src")
            it.after(" $src")
            it.replaceWith(org.jsoup.nodes.TextNode(placeholder))
        }
        return document.body().text()
    }

    private fun toCopyHtml(html: String, placeholder: String): String {
        val document = Jsoup.parseBodyFragment(html)
        document.select("img").forEach {
            val src = it.attr("src")
            it.after(""" <a href="$src">$src</a>""")
            it.replaceWith(org.jsoup.nodes.TextNode(placeholder))
        }
        return document.body().html()
    }

    companion object {
        val TAG: String = PostCopyDialogFragment::class.java.simpleName
        private const val ARG_TITLE = "title"
        private const val ARG_HTML = "html"

        fun newInstance(title: String?, html: String): PostCopyDialogFragment {
            return PostCopyDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_HTML, html)
                }
            }
        }
    }
}

