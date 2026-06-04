package com.hannesdorfmann.adapterdelegates3

import android.view.ViewGroup
import android.util.SparseArray
import androidx.recyclerview.widget.RecyclerView

class AdapterDelegatesManager<T> {
    private val delegates = SparseArray<AdapterDelegate<T>>()

    fun addDelegate(delegate: AdapterDelegate<T>): AdapterDelegatesManager<T> {
        var viewType = delegates.size()
        while (delegates.get(viewType) != null) {
            viewType++
        }
        return addDelegate(viewType, false, delegate)
    }

    fun addDelegate(viewType: Int, delegate: AdapterDelegate<T>): AdapterDelegatesManager<T> {
        return addDelegate(viewType, false, delegate)
    }

    fun addDelegate(
        viewType: Int,
        allowReplacingDelegate: Boolean,
        delegate: AdapterDelegate<T>,
    ): AdapterDelegatesManager<T> {
        if (!allowReplacingDelegate && delegates.get(viewType) != null) {
            throw IllegalArgumentException("An AdapterDelegate is already registered for viewType=$viewType.")
        }
        delegates.put(viewType, delegate)
        return this
    }

    fun getViewType(delegate: AdapterDelegate<T>): Int {
        val index = delegates.indexOfValue(delegate)
        return if (index >= 0) delegates.keyAt(index) else -1
    }

    fun getItemViewType(items: T, position: Int): Int {
        for (index in 0 until delegates.size()) {
            val delegate = delegates.valueAt(index)
            if (delegate.isForViewType(items, position)) {
                return delegates.keyAt(index)
            }
        }
        throw NullPointerException("No AdapterDelegate added that matches position=$position.")
    }

    fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val delegate = delegates.get(viewType)
            ?: throw NullPointerException("No AdapterDelegate added for viewType=$viewType.")
        return delegate.onCreateViewHolder(parent)
    }

    fun onBindViewHolder(
        items: T,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<Any>,
    ) {
        val delegate = delegates.get(holder.itemViewType)
            ?: throw NullPointerException("No AdapterDelegate found for viewType=${holder.itemViewType}.")
        delegate.onBindViewHolder(items, position, holder, payloads)
    }

    fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        delegates.get(holder.itemViewType)?.onViewRecycled(holder)
    }

    fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        return delegates.get(holder.itemViewType)?.onFailedToRecycleView(holder) ?: false
    }

    fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        delegates.get(holder.itemViewType)?.onViewAttachedToWindow(holder)
    }

    fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        delegates.get(holder.itemViewType)?.onViewDetachedFromWindow(holder)
    }
}
