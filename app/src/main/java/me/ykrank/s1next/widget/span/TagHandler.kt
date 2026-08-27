package me.ykrank.s1next.widget.span

import android.text.Editable
import android.text.Html
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ImageSpan
import android.widget.TextView
import me.ykrank.s1next.R
import org.xml.sax.XMLReader

/**
 * Adds [android.view.View.OnClickListener]
 * to [android.text.style.ImageSpan] and
 * handles &lt;strike&gt; tag.
 */
class TagHandler(textView: TextView) : Html.TagHandler {
    private val images = ArrayList<String>()

    init {
        textView.setTag(R.id.tag_text_view_span_images, images)
    }

    override fun handleTag(opening: Boolean, tag: String, output: Editable, xmlReader: XMLReader) {
        if ("img".equals(tag, ignoreCase = true)) {
            handleImg(opening, output)
        } else if ("acfun".equals(tag, ignoreCase = true)) {
            handleAcfun(opening, output, xmlReader)
        } else if ("bilibili".equals(tag, ignoreCase = true)) {
            handleBilibili(opening, output, xmlReader)
        } else if ("attach".equals(tag, ignoreCase = true)) {
            handleAttachment(opening, output, xmlReader)
        }
    }

    /**
     * Replaces [android.view.View.OnClickListener]
     * with [ImageClickableResizeSpan].
     *
     *
     * See android.text.Html.HtmlToSpannedConverter#startImg(android.text.SpannableStringBuilder, org.xml.sax.Attributes, android.text.Html.ImageGetter)
     */
    private fun handleImg(opening: Boolean, output: Editable) {
        if (!opening) {
            val end = output.length

            // \uFFFC: OBJECT REPLACEMENT CHARACTER
            val len = "\uFFFC".length
            val imageSpan = output.getSpans(end - len, end, ImageSpan::class.java)[0]
            var url = imageSpan.source
            if (url == null) {
                url = ""
            }
            // 保留 \uFFFC（OBJECT REPLACEMENT CHARACTER）作为图片占位符，不要替换成普通空格：
            // \uFFFC 是 Html.fromHtml 插入图片的标准占位符，TextLine 会正常测量并绘制其上的
            // ReplacementSpan。若替换成空格（" "），当图片满宽独占一行时，该空格会成为行尾可悬挂
            // 的空白字符，TextLine 不会对其上的 ReplacementSpan 调用 draw()，导致帖子内图片
            // "位置被占但显示为透明"。\uFFFC 既非空白也非换行、且不是图片 URL，复制帖子内容时
            // 不会带出图片链接。
            output.removeSpan(imageSpan)
            // make this ImageSpan clickable
            output.setSpan(
                ImageClickableResizeSpan(imageSpan.getDrawable(), url, images),
                end - len, output.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
    }

    private fun handleAcfun(opening: Boolean, output: Editable, xmlReader: XMLReader) {
        if (opening) {
            val attributes = HtmlTagHandlerCompat.processAttributes(xmlReader)
            AcfunSpan.startAcfun((output as SpannableStringBuilder), attributes)
        } else {
            AcfunSpan.endAcfun((output as SpannableStringBuilder))
        }
    }

    private fun handleBilibili(opening: Boolean, output: Editable, xmlReader: XMLReader) {
        if (opening) {
            BilibiliSpan.startBilibiliSpan((output as SpannableStringBuilder))
        } else {
            BilibiliSpan.endBilibiliSpan((output as SpannableStringBuilder))
        }
    }

    private fun handleAttachment(opening: Boolean, output: Editable, xmlReader: XMLReader) {
        if (opening) {
            val attributes = HtmlTagHandlerCompat.processAttributes(xmlReader)
            AttachmentSpan.startSpan((output as SpannableStringBuilder), attributes)
        } else {
            AttachmentSpan.endSpan(output as SpannableStringBuilder)
        }
    }

    companion object {
        private val TAG = TagHandler::class.java.getCanonicalName()

        /**
         * See android.text.Html.HtmlToSpannedConverter#getLast(android.text.Spanned, java.lang.Class)
         */
        fun <T> getLastSpan(text: Spanned, kind: Class<T>?): T? {
            val spans = text.getSpans(0, text.length, kind)
            return if (spans.isEmpty()) {
                null
            } else {
                spans[spans.size - 1]
            }
        }
    }
}
