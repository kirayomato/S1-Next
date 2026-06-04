package me.ykrank.s1next.view.adapter

import android.app.Activity
import android.database.Cursor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.binding.ViewBindingAdapter
import me.ykrank.s1next.data.db.biz.HistoryBiz
import me.ykrank.s1next.data.db.biz.ReadProgressBiz
import me.ykrank.s1next.data.db.dbmodel.History
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.databinding.ItemHistoryBinding
import me.ykrank.s1next.viewmodel.HistoryViewModel

class HistoryCursorRecyclerViewAdapter(
    activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val readPreferencesManager: ReadPreferencesManager,
    private val readProgressBiz: ReadProgressBiz,
    private val historyBiz: HistoryBiz
) :
    CursorRecyclerViewAdapter<HistoryCursorRecyclerViewAdapter.ViewHolder>(activity, null) {
    private val mLayoutInflater: LayoutInflater

    init {
        mLayoutInflater = activity.layoutInflater
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ItemHistoryBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(
            binding,
            HistoryViewModel(lifecycleOwner, readPreferencesManager, readProgressBiz)
        )
    }

    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        cursor: Cursor
    ) {
        viewHolder.bind(historyBiz.fromCursor(cursor))
    }

    class ViewHolder(
        private val binding: ItemHistoryBinding,
        private val model: HistoryViewModel
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            ViewBindingAdapter.setOnViewBind(binding.root, model.onBind())
        }

        fun bind(history: History) {
            model.history = history
            TextViewBindingAdapter.setHomeThread(binding.root, history)
        }
    }
}
