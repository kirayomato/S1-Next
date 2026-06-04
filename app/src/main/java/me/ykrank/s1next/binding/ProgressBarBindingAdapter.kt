package me.ykrank.s1next.binding

import android.content.res.ColorStateList
import android.widget.ProgressBar
import androidx.annotation.ColorInt

object ProgressBarBindingAdapter {
    @JvmStatic
    fun setProgressBarTint(
        view: ProgressBar,
        @ColorInt oldTintColor: Int,
        @ColorInt tintColor: Int
    ) {
        if (oldTintColor != tintColor) {
            view.progressTintList = ColorStateList.valueOf(tintColor)
        }
    }
}
