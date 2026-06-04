package me.ykrank.s1next.view.dialog

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.ykrank.s1next.databinding.ItemVoteBinding
import me.ykrank.s1next.viewmodel.ItemVoteViewModel

class VoteOptionsAdapter(
    private val onOptionClick: (Int) -> Unit,
) : RecyclerView.Adapter<VoteOptionsAdapter.VoteOptionViewHolder>() {
    private val items = mutableListOf<ItemVoteViewModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoteOptionViewHolder {
        val binding = ItemVoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VoteOptionViewHolder(binding, onOptionClick)
    }

    override fun onBindViewHolder(holder: VoteOptionViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun swapDataSet(data: List<ItemVoteViewModel>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    class VoteOptionViewHolder(
        private val binding: ItemVoteBinding,
        private val onOptionClick: (Int) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            val clickListener = View.OnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onOptionClick(position)
                }
            }
            binding.radio.setOnClickListener(clickListener)
            binding.checkBox.setOnClickListener(clickListener)
        }

        fun bind(model: ItemVoteViewModel) {
            val isSelected = model.selected
            val option = model.option
            binding.radio.isChecked = isSelected
            binding.radio.visibility = if (model.isSingleVotable) View.VISIBLE else View.GONE
            binding.checkBox.isChecked = isSelected
            binding.checkBox.visibility = if (model.isMultiVotable) View.VISIBLE else View.GONE
            binding.textView.text = option.option
            binding.tvPercent.text = "${option.percentStr}%"
            binding.tvCount.text = "(${option.votes})"
            binding.tvCount.setTextColor(option.getColorInt())
            binding.progress.progress = option.percent.toInt()
            binding.progress.progressTintList = ColorStateList.valueOf(option.getColorInt())
        }
    }
}
