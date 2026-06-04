package com.github.ykrank.androidtools.ui.internal

import androidx.viewpager.widget.PagerAdapter

interface PagerCallback {

    /**
     * A callback to set actual total pages which used for [PagerAdapter].
     */
    fun setTotalPages(totalPages: Int)
}
