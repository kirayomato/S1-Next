package com.github.ykrank.androidtools.ui.adapter.model

import com.google.common.base.Objects

interface DiffSameItem {

    /**
     * whether two object of this is same item, use in recycleView to show animate
     *
     * @return same or not
     */
    fun isSameItem(other: Any?): Boolean

    fun isSameContent(other: Any?): Boolean {
        return Objects.equal(this, other)
    }

    fun getChangePayload(other: Any?): Any? {
        return null
    }
}
