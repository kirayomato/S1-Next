package me.ykrank.s1next.view.page.setting.blacklist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import me.ykrank.s1next.data.db.dbmodel.BlackList
import me.ykrank.s1next.databinding.ItemBlacklistBinding

class BlackListPagingAdapter :
    PagingDataAdapter<BlackList, BlackListPagingAdapter.ViewHolder>(DIFF_CALLBACK) {
    companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<BlackList>() {
            override fun areItemsTheSame(oldItem: BlackList, newItem: BlackList): Boolean =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: BlackList, newItem: BlackList): Boolean =
                oldItem == newItem
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemBlacklistBinding = ItemBlacklistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(itemBlacklistBinding)
    }

    inner class ViewHolder(private val binding: ItemBlacklistBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(blackList: BlackList?) {
            binding.authorId.text = blackList?.authorId?.toString()
            binding.authorName.text = blackList?.author
            blackList?.forumRes?.let(binding.forum::setText) ?: run { binding.forum.text = null }
            blackList?.postRes?.let(binding.post::setText) ?: run { binding.post.text = null }
            binding.time.text = blackList?.time
        }
    }
}
