package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.data.api.model.search.UserSearchResult
import me.ykrank.s1next.databinding.ItemSearchUserBinding
import me.ykrank.s1next.view.activity.UserHomeActivity

class SearchUserAdapterDelegate(context: Context) :
    BaseAdapterDelegate<UserSearchResult, SearchUserAdapterDelegate.ViewHolder>(
        context,
        UserSearchResult::class.java
    ) {

    public override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemSearchUserBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolderData(
        t: UserSearchResult,
        position: Int,
        holder: ViewHolder,
        payloads: List<Any>
    ) {
        holder.bind(t)
    }

    class ViewHolder(private val binding: ItemSearchUserBinding) : RecyclerView.ViewHolder(binding.root) {
        private var avatarUid: String? = null

        fun bind(result: UserSearchResult) {
            binding.name.text = result.name
            ImageViewBindingAdapter.loadAvatar(binding.avatar, oldUid = avatarUid, newUid = result.uid)
            avatarUid = result.uid
            binding.content.setOnClickListener {
                result.uid?.let { uid ->
                    UserHomeActivity.start(it.context, uid, result.name, binding.avatar)
                }
            }
        }
    }
}
