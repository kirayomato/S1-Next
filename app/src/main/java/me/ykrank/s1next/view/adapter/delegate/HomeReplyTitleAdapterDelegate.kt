package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import me.ykrank.s1next.data.api.model.HomeThread
import me.ykrank.s1next.databinding.ItemHomeReplyTitleBinding
import me.ykrank.s1next.view.page.post.postlist.PostListGatewayActivity

/**
 * Created by ykrank on 2017/2/4.
 */

class HomeReplyTitleAdapterDelegate(context: Context) : BaseAdapterDelegate<HomeThread, HomeReplyTitleAdapterDelegate.ViewHolder>(context, HomeThread::class.java) {

    override fun onBindViewHolderData(t: HomeThread, position: Int, holder: ViewHolder, payloads: List<Any>) {
        holder.bind(t)
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemHomeReplyTitleBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(binding)
    }

    class ViewHolder(private val binding: ItemHomeReplyTitleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(thread: HomeThread) {
            HomeThreadAdapterDelegate.bindHomeThread(
                binding.threadTitle,
                binding.viewCount,
                binding.replyCount,
                binding.lastReplier,
                binding.lastReplyDate,
                thread
            )
            binding.root.setOnClickListener {
                thread.url?.let { url ->
                    PostListGatewayActivity.start(it.context, Uri.parse(url))
                }
            }
        }
    }
}
