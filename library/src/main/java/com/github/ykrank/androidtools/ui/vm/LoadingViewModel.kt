package com.github.ykrank.androidtools.ui.vm

import android.os.Parcel
import androidx.annotation.IntDef

class LoadingViewModel {
    private val loadingChangedListeners = mutableSetOf<() -> Unit>()

    @LoadingDef
    var loading: Int = LOADING_FIRST_TIME
        set(value) {
            field = value
            loadingChangedListeners.forEach { it() }
        }


    constructor()
    private constructor(source: Parcel) {
        loading = source.readInt()
    }

    val isSwipeRefresh: Boolean
        get() = loading == LOADING_SWIPE_REFRESH
    val isSwipeRefreshLayoutEnabled: Boolean
        get() = loading != LOADING_FIRST_TIME && loading != LOADING_PULL_UP_TO_REFRESH
    val isLoadingFirstTime: Boolean
        get() = loading == LOADING_FIRST_TIME

    fun addOnLoadingChangedListener(listener: () -> Unit) {
        loadingChangedListeners += listener
    }

    fun removeOnLoadingChangedListener(listener: () -> Unit) {
        loadingChangedListeners -= listener
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(value = [LOADING_FINISH, LOADING_FIRST_TIME, LOADING_SWIPE_REFRESH, LOADING_PULL_UP_TO_REFRESH])
    annotation class LoadingDef
    companion object {
        const val LOADING_FINISH = 0

        /**
         * We show circular indeterminate [android.widget.ProgressBar]
         * for the first time.
         */
        const val LOADING_FIRST_TIME = 1
        const val LOADING_SWIPE_REFRESH = 2
        const val LOADING_PULL_UP_TO_REFRESH = 3
    }
}
