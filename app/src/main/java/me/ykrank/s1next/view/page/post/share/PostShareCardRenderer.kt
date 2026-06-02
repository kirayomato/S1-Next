package me.ykrank.s1next.view.page.post.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.Html
import android.text.format.DateUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.ykrank.s1next.App
import me.ykrank.s1next.R
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.view.page.post.render.PostImageSizeCache
import me.ykrank.s1next.view.page.post.render.PostRenderItem
import me.ykrank.s1next.view.page.post.render.PostRenderMapper
import me.ykrank.s1next.widget.image.ImageBiz
import me.ykrank.s1next.widget.image.image
import me.ykrank.s1next.widget.span.HtmlCompat
import me.ykrank.s1next.widget.span.TagHandler
import me.ykrank.s1next.widget.span.replaceQuoteSpans
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

class PostShareCardRenderer(private val context: Context) {

    private val qrCodeBitmapFactory = QrCodeBitmapFactory()
    private val renderMapper = PostRenderMapper()
    private val imageBiz = ImageBiz(App.preAppComponent.downloadPreferencesManager)
    private val afternoonBackgroundColor = context.getColor(com.github.ykrank.androidtools.R.color.saraba_background)
    private val divideLineColor = context.getColor(com.github.ykrank.androidtools.R.color.black_12p)
    private val textPrimaryColor = context.getColor(com.github.ykrank.androidtools.R.color.black_87p)
    private val textSecondaryColor = context.getColor(com.github.ykrank.androidtools.R.color.black_54p)
    private val accentColor = context.getColor(com.github.ykrank.androidtools.R.color.light_blue_A400)

    fun createView(request: PostShareRequest): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(COLOR_PAGE_GRADIENT_START, COLOR_PAGE_GRADIENT_END)
            ).apply {
                cornerRadius = dp(24).toFloat()
            }
            outlineProvider = roundRectOutlineProvider(dp(24))
            clipToOutline = true
            setPadding(dp(8), 0, dp(8), 0)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            addView(brandContainer(brandRow(iconSize = dp(28), textSizeSp = 18f, strong = true)))
            addView(contentCard(request))
            addView(brandContainer(brandRow(iconSize = dp(20), textSizeSp = 13f, strong = false)))
        }
    }

    suspend fun renderViewToUri(threadId: String, view: View): Uri {
        val bitmap = renderViewToBitmap(view)
        val file = withContext(Dispatchers.IO) {
            writeBitmap(threadId, bitmap)
        }
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    private fun contentCard(request: PostShareRequest): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(12), dp(6), dp(12))
            background = rounded(afternoonBackgroundColor, dp(16))

            request.threadTitle?.takeIf { it.isNotBlank() }?.let { title ->
                addView(
                    textView(title, 16f, textPrimaryColor, Typeface.DEFAULT_BOLD).apply {
                        maxLines = 3
                    }
                )
                addView(divider(), marginParams(height = dp(1), top = dp(8), bottom = dp(8)))
            }

            request.posts.forEachIndexed { index, post ->
                addView(postCard(post), marginParams(top = if (index == 0) 0 else dp(2)))
            }

            addView(divider(), marginParams(height = dp(1), top = dp(8), bottom = dp(8)))
            addView(footerRow(request), marginParams(left = dp(16)))
        }
    }

    private fun postSection(post: Post): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(postHeader(post))

            val renderItems = renderMapper.map(listOf(post)).items
            var hasContent = false
            renderItems.forEach { item ->
                when (item) {
                    is PostRenderItem.TextBlock -> {
                        addView(htmlTextBlock(item.html), marginParams(top = dp(6)))
                        hasContent = true
                    }

                    is PostRenderItem.FallbackHtmlBlock -> {
                        addView(htmlTextBlock(item.html), marginParams(top = dp(6)))
                        hasContent = true
                    }

                    is PostRenderItem.ImageBlock -> {
                        addView(imageBlock(item), marginParams(top = dp(6)))
                        hasContent = true
                    }
                }
            }
            if (!hasContent) {
                addView(textView(" ", BODY_TEXT_SP, textPrimaryColor, Typeface.DEFAULT))
            }
        }
    }

    private fun postCard(post: Post): View {
        return CardView(context).apply {
            radius = dp(10).toFloat()
            cardElevation = dpFloat(0.5f)
            maxCardElevation = dpFloat(0.5f)
            useCompatPadding = true
            setCardBackgroundColor(COLOR_POST_CARD_BACKGROUND)
            setContentPadding(dp(8), dp(6), dp(8), dp(6))
            addView(postSection(post))
        }
    }

    private fun postHeader(post: Post): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(ImageView(context).apply {
                contentDescription = context.getString(R.string.avatar_content_desc)
                ImageViewBindingAdapter.loadAvatar(this, null, post.authorId)
            }, LinearLayout.LayoutParams(dp(32), dp(32)))

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                addView(textView(
                    post.authorName.orEmpty(),
                    13f,
                    textPrimaryColor,
                    Typeface.DEFAULT_BOLD
                ).apply {
                    maxLines = 1
                })

                addView(textView(
                    postMeta(post),
                    10f,
                    textSecondaryColor,
                    Typeface.DEFAULT
                ), marginParams(top = dp(2)))
            }, marginParams(width = 0, left = dp(8)).apply {
                weight = 1f
            })

            addView(textView(
                post.number?.let { "#$it" }.orEmpty(),
                12f,
                accentColor,
                Typeface.DEFAULT_BOLD
            ).apply {
                gravity = Gravity.CENTER_VERTICAL
            }, marginParams(width = ViewGroup.LayoutParams.WRAP_CONTENT, left = dp(8)))
        }
    }

    private fun htmlTextBlock(html: String): TextView {
        return textView("", BODY_TEXT_SP, textPrimaryColor, Typeface.DEFAULT).apply {
            text = runCatching {
                HtmlCompat.fromHtml(html, ShareHtmlImageGetter(), TagHandler(this)).replaceQuoteSpans(context)
            }.getOrElse {
                HtmlCompat.fromHtml(html).replaceQuoteSpans(context)
            }
        }
    }

    private fun imageBlock(item: PostRenderItem.ImageBlock): View {
        val imageView = SharePostImageView(context, dp(96)).apply {
            setSourceSize(
                item.width ?: PostImageSizeCache.get(item.url)?.width,
                item.height ?: PostImageSizeCache.get(item.url)?.height,
            )
            contentDescription = context.getString(R.string.picture_content_desc)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(afternoonBackgroundColor)
        }
        Glide.with(imageView)
            .image(imageBiz, item.url)
            .placeholder(R.drawable.ic_image_loading_placeholder)
            .error(R.drawable.ic_image_loading_placeholder)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean = false

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    if (item.width == null || item.height == null) {
                        PostImageSizeCache.put(item.url, resource.intrinsicWidth, resource.intrinsicHeight)
                        imageView.setSourceSize(resource.intrinsicWidth, resource.intrinsicHeight)
                    }
                    return false
                }
            })
            .into(imageView)
        return imageView
    }

    private fun footerRow(request: PostShareRequest): View {
        val firstPost = request.posts.firstOrNull()
        val link = postLink(request, firstPost)
        val code = shareCode(request, firstPost)
        val qrBitmap = qrCodeBitmapFactory.create(link, moduleSize = dp(2))
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER

            addView(ImageView(context).apply {
                setImageBitmap(qrBitmap)
                contentDescription = context.getString(R.string.post_share_qr_content_desc)
            }, LinearLayout.LayoutParams(dp(58), dp(58)))

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER_VERTICAL
                addView(
                    textView(
                        context.getString(R.string.post_share_scan_hint),
                        12f,
                        textPrimaryColor,
                        Typeface.DEFAULT_BOLD
                    )
                )
                addView(
                    textView(
                        context.getString(
                            R.string.post_share_detail_line,
                            request.threadId,
                            request.page,
                            firstPost?.number.orEmpty()
                        ),
                        9f,
                        textSecondaryColor,
                        Typeface.DEFAULT
                    ),
                    marginParams(top = dp(2))
                )
                addView(
                    textView(
                        context.getString(R.string.post_share_code, code),
                        9f,
                        textSecondaryColor,
                        Typeface.DEFAULT
                    ),
                    marginParams(top = dp(1))
                )
            }, marginParams(left = dp(8)))
        }
    }

    private fun brandRow(iconSize: Int, textSizeSp: Float, strong: Boolean): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER

            addView(ImageView(context).apply {
                setImageResource(R.mipmap.ic_launcher)
                scaleType = ImageView.ScaleType.CENTER_CROP
                outlineProvider = roundRectOutlineProvider(dp(if (strong) 6 else 5))
                clipToOutline = true
            }, LinearLayout.LayoutParams(iconSize, iconSize))

            addView(
                if (strong) {
                    textView(
                        context.getString(
                            R.string.post_share_brand_title,
                            context.getString(R.string.app_name)
                        ),
                        textSizeSp,
                        textPrimaryColor,
                        Typeface.DEFAULT_BOLD
                    )
                } else {
                    sloganView(textSizeSp)
                },
                marginParams(width = ViewGroup.LayoutParams.WRAP_CONTENT, left = dp(6))
            )
        }
    }

    private fun brandContainer(content: View): View {
        return FrameLayout(context).apply {
            addView(
                content,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(BRAND_SECTION_HEIGHT_DP)
            )
        }
    }

    private fun sloganView(textSizeSp: Float): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(
                textView(
                    context.getString(R.string.post_share_brand_slogan_prefix),
                    textSizeSp,
                    textSecondaryColor,
                    Typeface.DEFAULT
                )
            )
            addView(assetImage(ASSET_GOOSE_EMOTICON, dp(18)), marginParams(width = dp(18), height = dp(18), left = dp(1), right = dp(1)))
            addView(
                textView(
                    context.getString(R.string.post_share_brand_slogan_suffix),
                    textSizeSp,
                    textSecondaryColor,
                    Typeface.DEFAULT
                )
            )
        }
    }

    private fun renderViewToBitmap(view: View): Bitmap {
        ensureViewLayout(view)
        if (view.height >= MAX_CARD_HEIGHT) {
            throw IllegalStateException("Share image is too tall")
        }
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private fun ensureViewLayout(view: View) {
        if (view.width > 0 && view.height > 0) {
            return
        }
        val width = min(
            context.resources.displayMetrics.widthPixels - dp(32),
            MAX_CARD_WIDTH
        ).coerceAtLeast(MIN_CARD_WIDTH)
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
    }

    private fun textView(text: CharSequence, textSizeSp: Float, color: Int, typeface: Typeface): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(color)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            this.typeface = typeface
            includeFontPadding = true
            setLineSpacing(0f, 1.14f)
        }
    }

    private fun divider(): View {
        return View(context).apply {
            setBackgroundColor(divideLineColor)
        }
    }

    private fun rounded(color: Int, radius: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
        }
    }

    private fun roundRectOutlineProvider(radius: Int): ViewOutlineProvider {
        return object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius.toFloat())
            }
        }
    }

    private fun marginParams(
        width: Int = ViewGroup.LayoutParams.MATCH_PARENT,
        height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
        left: Int = 0,
        top: Int = 0,
        right: Int = 0,
        bottom: Int = 0,
    ): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(width, height).apply {
            setMargins(left, top, right, bottom)
        }
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

    private fun postMeta(post: Post): CharSequence {
        val time = if (post.dateTime > 0) {
            DateUtils.getRelativeDateTimeString(
                context,
                post.dateTime * 1000,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.DAY_IN_MILLIS,
                0
            ).toString()
        } else {
            ""
        }
        return time
    }

    private fun assetImage(assetPath: String, targetSize: Int): ImageView {
        return ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(loadAssetDrawable(assetPath, targetSize))
        }
    }

    private fun loadAssetDrawable(assetPath: String, targetHeight: Int): Drawable? {
        return runCatching {
            context.assets.open(assetPath).use { input ->
                Drawable.createFromStream(input, assetPath)?.apply {
                    val ratio = if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                        intrinsicWidth.toFloat() / intrinsicHeight
                    } else {
                        1f
                    }
                    val width = (targetHeight * ratio).roundToInt().coerceAtLeast(1)
                    setBounds(0, 0, width, targetHeight)
                }
            }
        }.getOrNull()
    }

    private fun postLink(request: PostShareRequest, firstPost: Post? = request.posts.firstOrNull()): String {
        return Api.getPostListUrlForBrowser(request.threadId, request.page) +
            (firstPost?.let { "#pid${it.id}" } ?: "")
    }

    private fun shareCode(request: PostShareRequest, firstPost: Post? = request.posts.firstOrNull()): String {
        return listOfNotNull(request.threadId, request.page.toString(), firstPost?.number)
            .joinToString("-")
    }

    private fun dp(value: Int): Int {
        return (value * context.resources.displayMetrics.density).roundToInt()
    }

    private fun dpFloat(value: Float): Float {
        return value * context.resources.displayMetrics.density
    }

    private inner class ShareHtmlImageGetter : Html.ImageGetter {
        override fun getDrawable(source: String?): Drawable? {
            val emoticonName = parseEmoticonName(source) ?: return null
            return loadAssetDrawable("$ASSET_EMOTICON_DIR/$emoticonName", dp(20))
        }
    }

    private fun parseEmoticonName(source: String?): String? {
        if (source.isNullOrBlank()) {
            return null
        }
        val normalized = source.removePrefix("/")
        return Api.parseEmoticonName(source)
            ?: Api.parseEmoticonName(normalized)
            ?: Api.parseEmoticonName(Api.BASE_URL + normalized)
            ?: normalized.substringAfter(URL_EMOTICON_MARKER, missingDelimiterValue = "")
                .takeIf { it.isNotBlank() && normalized.contains(URL_EMOTICON_MARKER) }
    }

    private class SharePostImageView(
        context: Context,
        private val minHeightPx: Int,
    ) : AppCompatImageView(context) {
        private var sourceWidth: Int? = null
        private var sourceHeight: Int? = null

        fun setSourceSize(width: Int?, height: Int?) {
            if (width == sourceWidth && height == sourceHeight) {
                return
            }
            sourceWidth = width?.takeIf { it > 0 }
            sourceHeight = height?.takeIf { it > 0 }
            requestLayout()
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            if (width <= 0) {
                super.onMeasure(widthMeasureSpec, heightMeasureSpec)
                return
            }
            val ratio = if (sourceWidth != null && sourceHeight != null) {
                sourceHeight!!.toFloat() / sourceWidth!!
            } else {
                DEFAULT_IMAGE_RATIO
            }
            setMeasuredDimension(width, (width * ratio).roundToInt().coerceAtLeast(minHeightPx))
        }
    }

    companion object {
        private const val MAX_CARD_WIDTH = 1080
        private const val MIN_CARD_WIDTH = 720
        private const val MAX_CARD_HEIGHT = 16000
        private const val MAX_CACHE_FILES = 8
        private const val CACHE_DIR = "post_share"
        private const val BODY_TEXT_SP = 13f
        private const val DEFAULT_IMAGE_RATIO = 0.75f
        private const val BRAND_SECTION_HEIGHT_DP = 48
        private const val ASSET_EMOTICON_DIR = "image/emoticon"
        private const val ASSET_GOOSE_EMOTICON = "$ASSET_EMOTICON_DIR/goose2017/001.png"
        private const val URL_EMOTICON_MARKER = "image/smiley/"
        private const val COLOR_PAGE_GRADIENT_START = 0xFFDFF8FE.toInt()
        private const val COLOR_PAGE_GRADIENT_END = 0xFFEEE9FF.toInt()
        private const val COLOR_POST_CARD_BACKGROUND = 0xFFFAFBF0.toInt()

    }
}
