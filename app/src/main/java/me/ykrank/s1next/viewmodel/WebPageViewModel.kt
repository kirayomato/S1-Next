package me.ykrank.s1next.viewmodel

import android.view.View
import androidx.databinding.BaseObservable

class WebPageViewModel : BaseObservable() {
    var finishedLoading: Boolean = false
        set(value) {
            field = value
            notifyChange()
        }

    val webViewVisibility: Int
        get() = if (finishedLoading) View.VISIBLE else View.INVISIBLE
}
