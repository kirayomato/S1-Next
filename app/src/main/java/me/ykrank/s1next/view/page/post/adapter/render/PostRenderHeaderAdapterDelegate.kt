package me.ykrank.s1next.view.page.post.adapter.render

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.binding.LibTextViewBindingAdapter
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewHolder
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.R
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.binding.ViewBindingAdapter
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.model.Post
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
    BaseAdapterDelegate<PostRenderItem.Header, PostRenderHeaderViewHolder>(
        context,
        PostRenderItem.Header::class.java
    ) {
    private var threadInfo: Thread? = null
    private var pageNum: Int = 1
    private val authorProfiles = mutableMapOf<String, Profile>()

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemPostRenderHeaderBinding.inflate(mLayoutInflater, parent, false)
        val viewModel = PostViewModel(fragment.viewLifecycleOwner, eventBus, user)
        binding.avatar.setOnClickListener(viewModel::onAvatarClick)
        binding.avatar.setOnLongClickListener(viewModel::onAvatarLongClick)
        binding.tvFloor.setOnClickListener(viewModel::showFloorActionMenu)
        TextViewBindingAdapter.increaseClickingArea(
            binding.tvFloor,
            binding.tvFloor.resources.getDimension(
                com.github.ykrank.androidtools.R.dimen.minimum_touch_target_size
            )
        )
        return PostRenderHeaderViewHolder(binding, viewModel)
    }

    override fun onBindViewHolderData(
        t: PostRenderItem.Header,
        position: Int,
        holder: PostRenderHeaderViewHolder,
        payloads: List<Any>
    ) {
        if (payloads.contains(PostShareSelectionPayload)) {
            bindShareSelection(holder.binding, holder.viewModel, t.post)
            return
        }
        if (payloads.any { it is Profile }) {
            val profile = t.post.authorId?.let { authorProfiles[it] }
            holder.viewModel.authorProfile = profile
            bindAuthorProfile(holder.binding, profile)
            return
        }
        bindHeader(holder.binding, holder.viewModel, t.post)
        bindAuthorProfile(holder.binding, t.post.authorId?.let { authorProfiles[it] })
        bindShareSelection(holder.binding, holder.viewModel, t.post)
    }

    private fun bindHeader(binding: ItemPostRenderHeaderBinding, viewModel: PostViewModel, post: Post) {
        ViewBindingAdapter.setMarginEnd(
            binding.contentContainer,
            if (generalPreferencesManager.isQuickSideBarEnable) {
                binding.root.resources.getDimension(com.github.ykrank.androidtools.R.dimen.spacing_normal)
            } else {
                0f
            }
        )
        viewModel.thread = threadInfo
        viewModel.pageNum = pageNum
        viewModel.post = post
        viewModel.authorProfile = post.authorId?.let { authorProfiles[it] }
        binding.threadTitle.text = threadInfo?.title
        binding.threadTitle.visibility =
            if (pageNum == 1 && post.isFirst && threadInfo?.title != null) View.VISIBLE else View.GONE
        ImageViewBindingAdapter.loadAvatar(binding.avatar, null, post.authorId)
        binding.authorName.text = post.authorName
        binding.originalPosterTag.visibility = if (post.isOpPost) View.VISIBLE else View.GONE
        LibTextViewBindingAdapter.setRelativeDateTime(binding.tvDatetime, post.dateTime * 1000)
        binding.tvFloor.text = viewModel.floor
    }

    private fun bindAuthorProfile(binding: ItemPostRenderHeaderBinding, profile: Profile?) {
        val goose = profile?.goose
        binding.goose.text = goose
        TextViewBindingAdapter.setGoose(binding.goose, goose)
        TextViewBindingAdapter.setRegistrationAge(binding.registrationAge, profile?.regDate)
    }

    private fun bindShareSelection(
        binding: ItemPostRenderHeaderBinding,
        viewModel: PostViewModel,
        post: Post
    ) {
        val shareSelectionState = postShareSelectionOwner?.postShareSelectionState
        val shareSelectionEnabled = shareSelectionState?.enabled == true
        val shareSelected = shareSelectionState?.selectedPostIds?.contains(post.id) == true
        binding.postShareScrim.visibility =
            if (shareSelectionEnabled && shareSelected) View.VISIBLE else View.GONE
        binding.postShareCheckboxContainer.visibility =
            if (shareSelectionEnabled) View.VISIBLE else View.GONE
        binding.postShareCheckbox.isChecked = shareSelected
        ViewBindingAdapter.setMarginEnd(
            binding.postShareCheckboxContainer,
            binding.root.resources.getDimension(
                if (generalPreferencesManager.isQuickSideBarEnable) {
                    R.dimen.post_share_checkbox_margin_end_with_quick_sidebar
                } else {
                    R.dimen.post_share_checkbox_margin_end
                }
            )
        )
        val headerViews = listOf(
            binding.root,
            binding.threadTitle,
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
            (headerViews + binding.avatar).forEach {
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
                viewModel.onAvatarClick(it)
            }
            binding.avatar.setOnLongClickListener {
                viewModel.onAvatarLongClick(it)
            }
            binding.postShareScrim.setOnClickListener(null)
            binding.postShareScrim.isClickable = false
            val showPostActionMenu = View.OnLongClickListener {
                viewModel.showFloorActionMenu(it)
                true
            }
            headerViews.forEach {
                it.setOnLongClickListener(showPostActionMenu)
            }
            binding.tvFloor.setOnClickListener {
                viewModel.showFloorActionMenu(it)
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

class PostRenderHeaderViewHolder(
    binding: ItemPostRenderHeaderBinding,
    val viewModel: PostViewModel
) : SimpleRecycleViewHolder<ItemPostRenderHeaderBinding>(binding)
