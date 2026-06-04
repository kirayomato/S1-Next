package com.hannesdorfmann.adapterdelegates3

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

open class ListDelegationAdapter<T : MutableList<Any>> : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    protected val delegatesManager = AdapterDelegatesManager<T>()
    @Suppress("UNCHECKED_CAST")
    @JvmField
    protected var items: T = mutableListOf<Any>() as T

    fun getItems(): T = items

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return delegatesManager.getItemViewType(items, position)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return delegatesManager.onCreateViewHolder(parent, viewType)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        delegatesManager.onBindViewHolder(items, position, holder, emptyList())
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        delegatesManager.onBindViewHolder(items, position, holder, payloads)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        delegatesManager.onViewRecycled(holder)
    }

    override fun onFailedToRecycleView(holder: RecyclerView.ViewHolder): Boolean {
        return delegatesManager.onFailedToRecycleView(holder)
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        delegatesManager.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        delegatesManager.onViewDetachedFromWindow(holder)
    }
}
