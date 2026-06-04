package me.ykrank.s1next.binding

import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.core.net.toFile
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.util.isFile
import com.github.ykrank.androidtools.widget.glide.viewtarget.SubsamplingScaleImageViewTarget
import me.ykrank.s1next.data.pref.DownloadPreferencesManager
import me.ykrank.s1next.widget.image.ImageBiz
import me.ykrank.s1next.widget.image.image

object SubsamplingScaleImageViewBindingAdapter {
    @JvmStatic
    fun loadImage(
        imageView: SubsamplingScaleImageView,
        url: Uri?,
        thumbUrl: Uri?,
        manager: DownloadPreferencesManager,
        show: Boolean
    ) {
        if (!show || url == null) {
            return
        }
        if (url.isFile()) {
            try {
                imageView.setImage(ImageSource.uri(url.toFile().absolutePath))
            } catch (e: Exception) {
                L.e(e)
                if (thumbUrl != null) {
                    loadImage(imageView, thumbUrl, null, manager, true)
                }
            }
            return
        }

        val imageBiz = ImageBiz(manager)
        val builder = Glide.with(imageView)
            .downloadOnly()
            .image(imageBiz, url, forcePass = true)
        builder.into(object : SubsamplingScaleImageViewTarget(imageView) {
            override fun onLoadFailed(errorDrawable: Drawable?) {
                if (thumbUrl != null) {
                    loadImage(imageView, thumbUrl, null, manager, show)
                } else {
                    super.onLoadFailed(errorDrawable)
                }
            }
        })
    }
}
