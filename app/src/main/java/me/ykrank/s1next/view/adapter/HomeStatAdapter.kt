package me.ykrank.s1next.view.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.ui.adapter.LibBaseRecyclerViewAdapter
import me.ykrank.s1next.databinding.ItemHomeStatBinding
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate

class HomeStatAdapter(context: Context) : LibBaseRecyclerViewAdapter(context, false) {

    init {
        addAdapterDelegate(HomeStatAdapterDelegate(context))
    }

    private class HomeStatAdapterDelegate(
        context: Context,
    ) : BaseAdapterDelegate<Pair<String, String>, HomeStatViewHolder>(context, null) {

        override fun isForViewType(items: MutableList<Any>, position: Int): Boolean {
            return items[position] is Pair<*, *>
        }

        override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            val binding = ItemHomeStatBinding.inflate(mLayoutInflater, parent, false)
            return HomeStatViewHolder(binding)
        }

        override fun onBindViewHolderData(
            t: Pair<String, String>,
            position: Int,
            holder: HomeStatViewHolder,
            payloads: List<Any>,
        ) {
            holder.bind(t)
        }
    }

    private class HomeStatViewHolder(
        private val binding: ItemHomeStatBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stat: Pair<String, String>) {
            binding.tvStatTitle.text = stat.first
            binding.tvStatValue.text = stat.second
        }
    }
}
