package me.ykrank.s1next.view.page.post.adapter.render

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewHolder
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.App
import me.ykrank.s1next.R
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.pref.GeneralPreferencesManager
import me.ykrank.s1next.databinding.ItemPostRenderHeaderBinding
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate
import me.ykrank.s1next.view.page.post.render.PostRenderItem
import me.ykrank.s1next.view.page.post.share.PostShareSelectionOwner
import me.ykrank.s1next.view.page.post.viewmodel.PostViewModel

class PostRenderHeaderAdapterDelegate(
    private val fragment: Fragment,
    context: Context,
    private val postShareSelectionOwner: PostShareSelectionOwner? = null
) :
    BaseAdapterDelegate<PostRenderItem.Header, SimpleRecycleViewHolder<ItemPostRenderHeaderBinding>>(
        context,
        PostRenderItem.Header::class.java
    ) {
    private val eventBus: EventBus = App.preAppComponent.eventBus
    private val user: User = App.appComponent.user
    private val generalPreferencesManager: GeneralPreferencesManager =
        App.preAppComponent.generalPreferencesManager

    private var threadInfo: Thread? = null
    private var pageNum: Int = 1

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = DataBindingUtil.inflate<ItemPostRenderHeaderBinding>(
            mLayoutInflater,
            R.layout.item_post_render_header,
            parent,
            false
        )
        binding.postViewModel = PostViewModel(fragment.viewLifecycleOwner, eventBus, user)
        return SimpleRecycleViewHolder(binding)
    }

    override fun onBindViewHolderData(
        t: PostRenderItem.Header,
        position: Int,
        holder: SimpleRecycleViewHolder<ItemPostRenderHeaderBinding>,
        payloads: List<Any>
    ) {
        holder.binding.quickSidebarEnable = generalPreferencesManager.isQuickSideBarEnable
        val shareSelectionState = postShareSelectionOwner?.postShareSelectionState
        val shareSelectionEnabled = shareSelectionState?.enabled == true
        holder.binding.postShareSelectionEnabled = shareSelectionEnabled
        holder.binding.postShareSelected = shareSelectionState?.selectedPostIds?.contains(t.post.id) == true
        holder.binding.postViewModel?.let {
            it.thread.set(threadInfo)
            it.pageNum.set(pageNum)
            it.post.set(t.post)
        }
        holder.binding.executePendingBindings()
        if (shareSelectionEnabled) {
            val toggleSelection = View.OnClickListener {
                postShareSelectionOwner?.togglePostShareSelection(t.post.id)
            }
            holder.binding.root.setOnClickListener(toggleSelection)
            holder.binding.tvFloor.setOnClickListener(toggleSelection)
        } else {
            holder.binding.root.setOnClickListener(null)
            holder.binding.tvFloor.setOnClickListener {
                holder.binding.postViewModel?.showFloorActionMenu(it)
            }
        }
    }

    fun setThreadInfo(threadInfo: Thread, pageNum: Int) {
        this.threadInfo = threadInfo
        this.pageNum = pageNum
    }
}
