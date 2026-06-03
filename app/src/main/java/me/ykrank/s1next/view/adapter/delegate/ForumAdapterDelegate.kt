package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.data.api.model.Forum
import me.ykrank.s1next.data.pref.ThemeManager
import me.ykrank.s1next.databinding.ItemForumBinding
import me.ykrank.s1next.view.activity.ThreadListActivity

class ForumAdapterDelegate(
    context: Context,
    private val themeManager: ThemeManager
) : BaseAdapterDelegate<Forum, ForumAdapterDelegate.ViewHolder>(context, Forum::class.java) {

    public override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemForumBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(binding, themeManager)
    }

    override fun onBindViewHolderData(t: Forum, position: Int, holder: ViewHolder, payloads: List<Any>) {
        holder.bind(t)
    }

    class ViewHolder(
        private val binding: ItemForumBinding,
        private val themeManager: ThemeManager
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(forum: Forum) {
            TextViewBindingAdapter.setForum(binding.root, forum, themeManager.gentleAccentColor)
            binding.root.setOnClickListener {
                ThreadListActivity.startThreadListActivity(it.context, forum)
            }
        }
    }
}
