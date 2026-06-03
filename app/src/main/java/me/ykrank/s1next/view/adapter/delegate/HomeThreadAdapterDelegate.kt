package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import me.ykrank.s1next.R
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.data.api.model.HomeThread
import me.ykrank.s1next.databinding.ItemHomeThreadBinding
import me.ykrank.s1next.view.page.post.postlist.PostListGatewayActivity

/**
 * Created by ykrank on 2017/2/4.
 */

class HomeThreadAdapterDelegate(context: Context) : BaseAdapterDelegate<HomeThread, HomeThreadAdapterDelegate.ViewHolder>(context, HomeThread::class.java) {

    override fun onBindViewHolderData(thread: HomeThread, position: Int, holder: ViewHolder, payloads: List<Any>) {
        holder.bind(thread)
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemHomeThreadBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(binding)
    }

    class ViewHolder(private val binding: ItemHomeThreadBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(thread: HomeThread) {
            bindHomeThread(
                binding.threadTitle,
                binding.viewCount,
                binding.replyCount,
                binding.lastReplier,
                binding.lastReplyDate,
                thread
            )
            binding.content.setOnClickListener {
                thread.url?.let { url ->
                    PostListGatewayActivity.start(it.context, Uri.parse(url))
                }
            }
        }
    }

    companion object {
        fun bindHomeThread(
            title: android.widget.TextView,
            viewCount: android.widget.TextView,
            replyCount: android.widget.TextView,
            lastReplier: android.widget.TextView,
            lastReplyDate: android.widget.TextView,
            thread: HomeThread
        ) {
            TextViewBindingAdapter.setHomeThread(title, thread)
            viewCount.text = viewCount.context.getString(R.string.view) + " " + thread.view
            replyCount.text = replyCount.context.getString(R.string.reply) + " " + thread.reply
            lastReplier.text = lastReplier.context.getString(R.string.last_replier, thread.lastReplier)
            lastReplyDate.text = thread.lastReplyDate
        }
    }
}
