package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

import me.ykrank.s1next.data.api.model.HomeReply
import me.ykrank.s1next.databinding.ItemHomeReplyItemBinding
import me.ykrank.s1next.view.page.post.postlist.PostListGatewayActivity

/**
 * Created by ykrank on 2017/2/4.
 */

class HomeReplyItemAdapterDelegate(context: Context) : BaseAdapterDelegate<HomeReply, HomeReplyItemAdapterDelegate.ViewHolder>(context, HomeReply::class.java) {

    override fun onBindViewHolderData(t: HomeReply, position: Int, holder: ViewHolder, payloads: List<Any>) {
        holder.bind(t)
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemHomeReplyItemBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(binding)
    }

    class ViewHolder(private val binding: ItemHomeReplyItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reply: HomeReply) {
            binding.reply.text = reply.reply
            binding.root.setOnClickListener {
                reply.url?.let { url ->
                    PostListGatewayActivity.start(it.context, Uri.parse(url))
                }
            }
        }
    }
}
