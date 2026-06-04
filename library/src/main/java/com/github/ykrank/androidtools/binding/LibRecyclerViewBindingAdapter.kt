package com.github.ykrank.androidtools.binding

import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.ui.adapter.LibBaseRecyclerViewAdapter

object LibRecyclerViewBindingAdapter {
    @JvmStatic
    fun setHasProgress(
        recyclerView: RecyclerView,
        oldIsLoadingFirstTime: Boolean?,
        newIsLoadingFirstTime: Boolean?
    ) {
        if (newIsLoadingFirstTime != oldIsLoadingFirstTime) {
            val adapter = recyclerView.adapter
            if (adapter is LibBaseRecyclerViewAdapter) {
                adapter.setHasProgress(newIsLoadingFirstTime!!)
            }
        }
    }
}
