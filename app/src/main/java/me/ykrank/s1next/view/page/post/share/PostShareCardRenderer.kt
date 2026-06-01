package me.ykrank.s1next.view.page.post.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.widget.span.HtmlCompat
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.util.Date
import kotlin.math.min
import kotlin.math.roundToInt

class PostShareCardRenderer(private val context: Context) {

    suspend fun renderToUri(request: PostShareRequest): Uri {
        val bitmap = withContext(Dispatchers.Default) {
            renderToBitmap(request)
        }
        val file = withContext(Dispatchers.IO) {
            writeBitmap(request.threadId, bitmap)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun renderToBitmap(request: PostShareRequest): Bitmap {
        val width = min(context.resources.displayMetrics.widthPixels.coerceAtLeast(MIN_CARD_WIDTH), MAX_CARD_WIDTH)
        val contentWidth = width - PADDING * 2
        val titlePaint = textPaint(40f, Color.rgb(24, 28, 35), Typeface.DEFAULT_BOLD)
        val subtitlePaint = textPaint(25f, Color.rgb(92, 100, 112), Typeface.DEFAULT)
        val authorPaint = textPaint(31f, Color.rgb(24, 28, 35), Typeface.DEFAULT_BOLD)
        val metaPaint = textPaint(24f, Color.rgb(105, 113, 124), Typeface.DEFAULT)
        val bodyPaint = textPaint(30f, Color.rgb(38, 43, 51), Typeface.DEFAULT)
        val footerPaint = textPaint(24f, Color.rgb(92, 100, 112), Typeface.DEFAULT)

        val titleLayout = layout(request.threadTitle?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.app_name), titlePaint, contentWidth)
        val subtitleLayout = layout(context.getString(R.string.post_share_from_stage1), subtitlePaint, contentWidth)
        val postLayouts = request.posts.map { post ->
            RenderPost(
                post = post,
                author = layout(post.authorName.orEmpty(), authorPaint, contentWidth),
                meta = layout(postMeta(post), metaPaint, contentWidth),
                body = layout(postBody(post), bodyPaint, contentWidth),
            )
        }
        val firstPost = request.posts.firstOrNull()
        val link = Api.getPostListUrlForBrowser(request.threadId, request.page) +
            (firstPost?.let { "#pid${it.id}" } ?: "")
        val code = listOfNotNull(request.threadId, request.page.toString(), firstPost?.number)
            .joinToString("-")
        val footerLayout = layout(
            "$link\n${context.getString(R.string.post_share_code, code)}",
            footerPaint,
            contentWidth
        )

        val height = (
            PADDING +
                titleLayout.height +
                SMALL_GAP +
                subtitleLayout.height +
                SECTION_GAP +
                postLayouts.sumOf { it.height } +
                SECTION_GAP +
                footerLayout.height +
                PADDING
            ).coerceAtMost(MAX_CARD_HEIGHT)
        if (height >= MAX_CARD_HEIGHT) {
            throw IllegalStateException("Share image is too tall")
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        var y = PADDING
        titleLayout.drawAt(canvas, PADDING, y)
        y += titleLayout.height + SMALL_GAP
        subtitleLayout.drawAt(canvas, PADDING, y)
        y += subtitleLayout.height + SECTION_GAP

        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(247, 248, 250)
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 230, 236)
            strokeWidth = 1f
        }
        postLayouts.forEachIndexed { index, item ->
            val top = y
            val bottom = y + item.height - POST_GAP
            canvas.drawRoundRect(
                PADDING.toFloat(),
                top.toFloat(),
                (width - PADDING).toFloat(),
                bottom.toFloat(),
                CARD_RADIUS,
                CARD_RADIUS,
                cardPaint
            )
            var innerY = top + CARD_PADDING
            item.author.drawAt(canvas, PADDING + CARD_PADDING, innerY)
            innerY += item.author.height + SMALL_GAP
            item.meta.drawAt(canvas, PADDING + CARD_PADDING, innerY)
            innerY += item.meta.height + BODY_GAP
            item.body.drawAt(canvas, PADDING + CARD_PADDING, innerY)
            y += item.height
            if (index != postLayouts.lastIndex) {
                canvas.drawLine(
                    (PADDING + CARD_PADDING).toFloat(),
                    (y - POST_GAP / 2).toFloat(),
                    (width - PADDING - CARD_PADDING).toFloat(),
                    (y - POST_GAP / 2).toFloat(),
                    dividerPaint
                )
            }
        }

        y += SECTION_GAP - POST_GAP
        footerLayout.drawAt(canvas, PADDING, y)
        return bitmap
    }

    private fun writeBitmap(threadId: String, bitmap: Bitmap): File {
        val dir = File(context.cacheDir, CACHE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(MAX_CACHE_FILES)?.forEach { it.delete() }
        val file = File(dir, "post_share_${threadId}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
        return file
    }

    private fun postMeta(post: Post): String {
        val floor = post.number?.let { "#$it" }.orEmpty()
        val time = if (post.dateTime > 0) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(post.dateTime * 1000))
        } else {
            ""
        }
        return listOf(floor, time).filter { it.isNotBlank() }.joinToString(" · ")
    }

    private fun postBody(post: Post): String {
        val html = if (post.isTrade) post.extraHtml else post.reply
        val text = HtmlCompat.fromHtml(html.orEmpty()).toString()
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
        return text.ifBlank { " " }
    }

    private fun textPaint(textSizeSp: Float, color: Int, typeface: Typeface): TextPaint {
        return TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = sp(textSizeSp)
            this.typeface = typeface
        }
    }

    private fun layout(text: CharSequence, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.12f)
            .setIncludePad(true)
            .build()
    }

    private fun sp(value: Float): Float {
        return value * context.resources.displayMetrics.scaledDensity
    }

    private fun StaticLayout.drawAt(canvas: Canvas, x: Int, y: Int) {
        canvas.save()
        canvas.translate(x.toFloat(), y.toFloat())
        draw(canvas)
        canvas.restore()
    }

    private data class RenderPost(
        val post: Post,
        val author: StaticLayout,
        val meta: StaticLayout,
        val body: StaticLayout,
    ) {
        val height: Int
            get() = CARD_PADDING * 2 + author.height + SMALL_GAP + meta.height + BODY_GAP + body.height + POST_GAP
    }

    companion object {
        private const val MAX_CARD_WIDTH = 1080
        private const val MIN_CARD_WIDTH = 720
        private const val MAX_CARD_HEIGHT = 16000
        private const val MAX_CACHE_FILES = 8
        private const val CACHE_DIR = "post_share"
        private const val PADDING = 48
        private const val CARD_PADDING = 32
        private const val SMALL_GAP = 8
        private const val BODY_GAP = 20
        private const val SECTION_GAP = 36
        private const val POST_GAP = 24
        private const val CARD_RADIUS = 18f
    }
}
