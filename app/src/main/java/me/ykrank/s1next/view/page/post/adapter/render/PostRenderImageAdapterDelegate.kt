package me.ykrank.s1next.view.page.post.adapter.render

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import me.ykrank.s1next.App
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.pref.DownloadPreferencesManager
import me.ykrank.s1next.view.activity.GalleryActivity
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate
import me.ykrank.s1next.view.page.post.render.PostImageSizeCache
import me.ykrank.s1next.view.page.post.render.PostRenderActions
import me.ykrank.s1next.view.page.post.render.PostRenderItem
import me.ykrank.s1next.widget.image.ImageBiz
import me.ykrank.s1next.widget.image.image
import me.ykrank.s1next.view.page.post.render.PostActionMenuPopup
import kotlin.math.roundToInt

class PostRenderImageAdapterDelegate(
    private val fragment: Fragment,
    context: Context,
    private val imageUrlsProvider: () -> List<String>,
) : BaseAdapterDelegate<PostRenderItem.ImageBlock, PostRenderImageAdapterDelegate.ViewHolder>(
    context,
    PostRenderItem.ImageBlock::class.java
) {
    private val downloadPreferencesManager: DownloadPreferencesManager =
        App.preAppComponent.downloadPreferencesManager

    private var threadInfo: Thread? = null
    private var pageNum: Int = 1

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = mLayoutInflater.inflate(R.layout.item_post_render_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolderData(
        t: PostRenderItem.ImageBlock,
        position: Int,
        holder: ViewHolder,
        payloads: List<Any>
    ) {
        holder.boundUrl = t.url
        val targetWidth = holder.availableImageWidth()
        val bindHeight = t.displayHeight(targetWidth)
        val hasKnownRatio = t.hasKnownRatio()
        val requestHeight = if (hasKnownRatio) bindHeight else Target.SIZE_ORIGINAL
        holder.setImageHeight(bindHeight)
        holder.image.setOnClickListener {
            val urls = ArrayList(imageUrlsProvider())
            GalleryActivity.start(it.context, urls, urls.indexOf(t.url).coerceAtLeast(0))
        }
        val longClickListener = View.OnLongClickListener {
            PostRenderActions.showPostActionMenu(it, fragment, threadInfo, pageNum, t.post)
        }
        val touchListener = View.OnTouchListener { view, event ->
            PostActionMenuPopup.recordTouchPoint(view, event)
            false
        }
        holder.itemView.setOnTouchListener(touchListener)
        holder.image.setOnTouchListener(touchListener)
        holder.itemView.setOnLongClickListener(longClickListener)
        holder.image.setOnLongClickListener(longClickListener)

        val imageBiz = ImageBiz(downloadPreferencesManager)
        Glide.with(holder.image)
            .image(imageBiz, t.url)
            .placeholder(R.drawable.ic_image_loading_placeholder)
            .error(R.drawable.ic_image_loading_placeholder)
            .override(targetWidth, requestHeight)
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
                    if (holder.boundUrl != t.url) {
                        return false
                    }
                    if (t.width == null || t.height == null) {
                        PostImageSizeCache.put(t.url, resource.intrinsicWidth, resource.intrinsicHeight)
                    }
                    val newHeight = t.displayHeight(holder.availableImageWidth(), resource)
                    if (kotlin.math.abs(holder.image.layoutParams.height - newHeight) > HEIGHT_UPDATE_THRESHOLD) {
                        holder.setImageHeight(newHeight)
                        holder.itemView.requestLayout()
                    }
                    return false
                }
            })
            .into(holder.image)
    }

    fun setThreadInfo(threadInfo: Thread, pageNum: Int) {
        this.threadInfo = threadInfo
        this.pageNum = pageNum
    }

    private fun PostRenderItem.ImageBlock.displayHeight(targetWidth: Int, resource: Drawable? = null): Int {
        val cached = PostImageSizeCache.get(url)
        val sourceWidth = width ?: cached?.width ?: resource?.intrinsicWidth
        val sourceHeight = height ?: cached?.height ?: resource?.intrinsicHeight
        val ratio = if (sourceWidth != null && sourceHeight != null && sourceWidth > 0 && sourceHeight > 0) {
            sourceHeight.toFloat() / sourceWidth
        } else {
            DEFAULT_RATIO
        }
        return (targetWidth * ratio).roundToInt().coerceAtLeast(MIN_HEIGHT_PX)
    }

    private fun PostRenderItem.ImageBlock.hasKnownRatio(): Boolean {
        return (width != null && height != null) || PostImageSizeCache.get(url) != null
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.post_image)
        var boundUrl: String? = null

        fun availableImageWidth(): Int {
            val width = itemView.width.takeIf { it > 0 }
                ?: itemView.resources.displayMetrics.widthPixels
            val horizontalPadding = itemView.resources.getDimensionPixelSize(
                com.github.ykrank.androidtools.R.dimen.item_padding
            ) * 2
            return (width - horizontalPadding).coerceAtLeast(1)
        }

        fun setImageHeight(height: Int) {
            val lp = image.layoutParams
            if (lp.height != height) {
                lp.height = height
                image.layoutParams = lp
            }
        }
    }

    companion object {
        private const val DEFAULT_RATIO = 0.75f
        private const val MIN_HEIGHT_PX = 96
        private const val HEIGHT_UPDATE_THRESHOLD = 12
    }
}
