package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.data.api.model.darkroom.DarkRoom
import me.ykrank.s1next.databinding.ItemDarkRoomBinding
import me.ykrank.s1next.view.activity.UserHomeActivity

class DarkRoomAdapterDelegate(context: Context) : BaseAdapterDelegate<DarkRoom, DarkRoomAdapterDelegate.ViewHolder>(context, DarkRoom::class.java) {

    public override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemDarkRoomBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolderData(t: DarkRoom, position: Int, holder: ViewHolder, payloads: List<Any>) {
        holder.bind(t)
    }

    class ViewHolder(private val binding: ItemDarkRoomBinding) : RecyclerView.ViewHolder(binding.root) {
        private var avatarUid: String? = null

        fun bind(darkRoom: DarkRoom) {
            binding.authorName.text = darkRoom.username
            binding.tvExpireTime.text = darkRoom.groupExpiry
            binding.tvReason.text = darkRoom.reason
            binding.tvOperator.text = "--- ${darkRoom.operator}"
            binding.tvOpTime.text = darkRoom.dateline

            ImageViewBindingAdapter.loadAvatar(binding.avatar, oldUid = avatarUid, newUid = darkRoom.uid)
            avatarUid = darkRoom.uid
            binding.avatar.setOnClickListener { v: View ->
                val uid = darkRoom.uid
                if (uid != null) {
                    UserHomeActivity.start(v.context, uid, darkRoom.username, v)
                }
            }
        }
    }
}
