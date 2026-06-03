package me.ykrank.s1next.view.adapter

import android.app.Activity
import android.database.Cursor
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewHolder
import me.ykrank.s1next.data.db.biz.HistoryBiz
import me.ykrank.s1next.data.db.biz.ReadProgressBiz
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
    CursorRecyclerViewAdapter<SimpleRecycleViewHolder<ItemHistoryBinding>>(activity, null) {
    private val mLayoutInflater: LayoutInflater

    init {
        mLayoutInflater = activity.layoutInflater
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SimpleRecycleViewHolder<ItemHistoryBinding> {
        val binding = ItemHistoryBinding.inflate(mLayoutInflater, parent, false)
        binding.setModel(HistoryViewModel(lifecycleOwner, readPreferencesManager, readProgressBiz))
        return SimpleRecycleViewHolder(binding)
    }

    override fun onBindViewHolder(
        viewHolder: SimpleRecycleViewHolder<ItemHistoryBinding>,
        cursor: Cursor
    ) {
        val binding = viewHolder.binding
        binding.getModel()?.history?.set(historyBiz.fromCursor(cursor))
    }
}
