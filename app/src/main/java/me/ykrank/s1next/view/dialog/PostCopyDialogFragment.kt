package me.ykrank.s1next.view.dialog

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import androidx.appcompat.app.AlertDialog
import me.ykrank.s1next.R
import me.ykrank.s1next.databinding.DialogPostCopyBinding
import me.ykrank.s1next.widget.span.HtmlCompat
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

class PostCopyDialogFragment : BaseDialogFragment() {

    private lateinit var binding: DialogPostCopyBinding

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        binding = DialogPostCopyBinding.inflate(
            activity.layoutInflater,
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
        document.select("img").remove()
        return document.body().toPlainText()
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

    private fun Element.toPlainText(): String {
        val builder = StringBuilder()
        appendPlainText(this, builder)
        return builder.toString()
            .replace(Regex("[ \\t\\x0B\\f\\r]+\\n"), "\n")
            .replace(Regex("\\n[ \\t\\x0B\\f\\r]+"), "\n")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun appendPlainText(node: Node, builder: StringBuilder) {
        when (node) {
            is TextNode -> builder.append(node.wholeText)
            is Element -> {
                val tagName = node.normalName()
                if (tagName == "br") {
                    builder.append('\n')
                    return
                }
                val block = node.isPlainTextBlock()
                if (block) {
                    builder.trimTrailingInlineSpaces()
                    builder.ensureLineBreak()
                }
                node.childNodes().forEach { appendPlainText(it, builder) }
                if (block) {
                    builder.trimTrailingInlineSpaces()
                    builder.ensureLineBreak()
                }
            }
            else -> node.childNodes().forEach { appendPlainText(it, builder) }
        }
    }

    private fun Element.isPlainTextBlock(): Boolean {
        return normalName() in BLOCK_TAGS
    }

    private fun StringBuilder.ensureLineBreak() {
        if (isNotEmpty() && last() != '\n') {
            append('\n')
        }
    }

    private fun StringBuilder.trimTrailingInlineSpaces() {
        while (isNotEmpty() && last() != '\n' && last().isWhitespace()) {
            deleteAt(length - 1)
        }
    }

    companion object {
        val TAG: String = PostCopyDialogFragment::class.java.simpleName
        private const val ARG_TITLE = "title"
        private const val ARG_HTML = "html"
        private val BLOCK_TAGS = setOf(
            "address",
            "article",
            "aside",
            "blockquote",
            "dd",
            "div",
            "dl",
            "dt",
            "fieldset",
            "figcaption",
            "figure",
            "footer",
            "form",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "header",
            "hr",
            "li",
            "main",
            "nav",
            "ol",
            "p",
            "pre",
            "section",
            "table",
            "tbody",
            "td",
            "tfoot",
            "th",
            "thead",
            "tr",
            "ul"
        )

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
