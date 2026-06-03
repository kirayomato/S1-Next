package me.ykrank.s1next.view.page.post.adapter.render

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewHolder
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.R
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.model.Profile
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.pref.GeneralPreferencesManager
import me.ykrank.s1next.databinding.ItemPostRenderHeaderBinding
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate
import me.ykrank.s1next.view.page.post.render.PostRenderItem
import me.ykrank.s1next.view.page.post.share.PostShareSelectionOwner
import me.ykrank.s1next.view.page.post.share.PostShareSelectionPayload
import me.ykrank.s1next.view.page.post.viewmodel.PostViewModel

class PostRenderHeaderAdapterDelegate(
    private val fragment: Fragment,
    context: Context,
    private val postShareSelectionOwner: PostShareSelectionOwner? = null,
    private val eventBus: EventBus,
    private val user: User,
    private val generalPreferencesManager: GeneralPreferencesManager
) :
    BaseAdapterDelegate<PostRenderItem.Header, SimpleRecycleViewHolder<ItemPostRenderHeaderBinding>>(
        context,
        PostRenderItem.Header::class.java
    ) {
    private var threadInfo: Thread? = null
    private var pageNum: Int = 1
    private val authorProfiles = mutableMapOf<String, Profile>()

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
        if (payloads.contains(PostShareSelectionPayload)) {
            bindShareSelection(holder.binding, t.post)
            return
        }
        if (payloads.any { it is Profile }) {
            val profile = t.post.authorId?.let { authorProfiles[it] }
            holder.binding.postViewModel?.authorProfile?.set(profile)
            holder.binding.executePendingBindings()
            bindAuthorProfile(holder.binding, profile)
            return
        }
        holder.binding.quickSidebarEnable = generalPreferencesManager.isQuickSideBarEnable
        holder.binding.postViewModel?.let {
            it.thread.set(threadInfo)
            it.pageNum.set(pageNum)
            it.post.set(t.post)
            it.authorProfile.set(t.post.authorId?.let { authorProfiles[it] })
        }
        holder.binding.executePendingBindings()
        bindAuthorProfile(holder.binding, t.post.authorId?.let { authorProfiles[it] })
        bindShareSelection(holder.binding, t.post)
    }

    private fun bindAuthorProfile(binding: ItemPostRenderHeaderBinding, profile: Profile?) {
        val goose = profile?.goose
        binding.goose.text = goose
        TextViewBindingAdapter.setGoose(binding.goose, goose)
        TextViewBindingAdapter.setRegistrationAge(binding.registrationAge, profile?.regDate)
    }

    private fun bindShareSelection(binding: ItemPostRenderHeaderBinding, post: me.ykrank.s1next.data.api.model.Post) {
        val shareSelectionState = postShareSelectionOwner?.postShareSelectionState
        val shareSelectionEnabled = shareSelectionState?.enabled == true
        binding.postShareSelectionEnabled = shareSelectionEnabled
        binding.postShareSelected = shareSelectionState?.selectedPostIds?.contains(post.id) == true
        binding.executePendingBindings()
        val headerViews = listOf(
            binding.root,
            binding.threadTitle,
            binding.avatar,
            binding.authorName,
            binding.goose,
            binding.registrationAge,
            binding.originalPosterTag,
            binding.tvDatetime,
            binding.tvFloor
        )
        if (shareSelectionEnabled) {
            val toggleSelection = View.OnClickListener {
                postShareSelectionOwner?.togglePostShareSelection(post.id)
            }
            headerViews.forEach {
                it.setOnClickListener(toggleSelection)
                it.setOnLongClickListener(null)
            }
            binding.postShareScrim.setOnClickListener(toggleSelection)
        } else {
            binding.root.setOnClickListener(null)
            binding.threadTitle.setOnClickListener(null)
            binding.authorName.setOnClickListener(null)
            binding.goose.setOnClickListener(null)
            binding.registrationAge.setOnClickListener(null)
            binding.originalPosterTag.setOnClickListener(null)
            binding.tvDatetime.setOnClickListener(null)
            binding.avatar.setOnClickListener {
                binding.postViewModel?.onAvatarClick(it)
            }
            binding.postShareScrim.setOnClickListener(null)
            binding.postShareScrim.isClickable = false
            val showPostActionMenu = View.OnLongClickListener {
                binding.postViewModel?.showFloorActionMenu(it)
                true
            }
            headerViews.forEach {
                it.setOnLongClickListener(showPostActionMenu)
            }
            binding.tvFloor.setOnClickListener {
                binding.postViewModel?.showFloorActionMenu(it)
            }
        }
    }

    fun setThreadInfo(threadInfo: Thread, pageNum: Int) {
        this.threadInfo = threadInfo
        this.pageNum = pageNum
    }

    fun setAuthorProfile(uid: String, profile: Profile) {
        authorProfiles[uid] = profile
    }
}
