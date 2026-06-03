package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.ykrank.s1next.R
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.data.api.model.Friend
import me.ykrank.s1next.databinding.ItemFriendBinding
import me.ykrank.s1next.view.activity.UserHomeActivity

/**
 * Created by ykrank on 2017/1/16.
 */

class FriendAdapterDelegate(context: Context) : BaseAdapterDelegate<Friend, FriendAdapterDelegate.ViewHolder>(context, Friend::class.java) {

    override fun onBindViewHolderData(t: Friend, position: Int, holder: ViewHolder, payloads: List<Any>) {
        holder.bind(t)
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemFriendBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(binding)
    }

    class ViewHolder(private val binding: ItemFriendBinding) : RecyclerView.ViewHolder(binding.root) {
        private var avatarUid: String? = null

        fun bind(friend: Friend) {
            binding.tvUid.text = binding.root.context.getString(R.string.uid_content, friend.uid)
            binding.tvName.text = friend.username
            ImageViewBindingAdapter.loadAvatar(binding.avatar, oldUid = avatarUid, newUid = friend.uid)
            avatarUid = friend.uid
            binding.content.setOnClickListener {
                friend.uid?.let { uid ->
                    UserHomeActivity.start(it.context, uid, friend.username, binding.avatar)
                }
            }
        }
    }
}
