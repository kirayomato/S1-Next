package com.github.ykrank.androidtools.widget

import com.google.android.material.appbar.AppBarLayout

abstract class AppBarOffsetChangedListener : AppBarLayout.OnOffsetChangedListener {
    private var verticalOffsetTemp = 0

    abstract fun onStateChanged(
        appBarLayout: AppBarLayout,
        oldVerticalOffset: Int,
        verticalOffset: Int
    )

    final override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
        onStateChanged(appBarLayout, verticalOffsetTemp, verticalOffset)
        verticalOffsetTemp = verticalOffset
    }
}
