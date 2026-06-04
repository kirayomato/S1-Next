package com.hannesdorfmann.adapterdelegates3

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

abstract class AdapterDelegate<T> {
    abstract fun isForViewType(items: T, position: Int): Boolean

    abstract fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder

    abstract fun onBindViewHolder(
        items: T,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<Any>,
    )

    open fun onViewRecycled(holder: RecyclerView.ViewHolder) = Unit

    open fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean = false

    open fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) = Unit

    open fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) = Unit
}
