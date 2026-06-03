package me.ykrank.s1next.view.adapter

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.ui.adapter.LibBaseRecyclerViewAdapter
import me.ykrank.s1next.databinding.ItemRateReasonBinding
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate

class RateReasonAdapter(
    context: Context,
    onReasonClick: (String) -> Unit,
) : LibBaseRecyclerViewAdapter(context, false) {

    init {
        addAdapterDelegate(RateReasonAdapterDelegate(context, onReasonClick))
    }

    private class RateReasonAdapterDelegate(
        context: Context,
        private val onReasonClick: (String) -> Unit,
    ) : BaseAdapterDelegate<String, RateReasonViewHolder>(context, String::class.java) {

        override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            val binding = ItemRateReasonBinding.inflate(mLayoutInflater, parent, false)
            return RateReasonViewHolder(binding)
        }

        override fun onBindViewHolderData(
            t: String,
            position: Int,
            holder: RateReasonViewHolder,
            payloads: List<Any>,
        ) {
            holder.bind(t, onReasonClick)
        }
    }

    private class RateReasonViewHolder(
        private val binding: ItemRateReasonBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reason: String, onReasonClick: (String) -> Unit) {
            binding.tvReason.text = reason
            binding.root.setOnClickListener {
                onReasonClick(reason)
            }
        }
    }
}
