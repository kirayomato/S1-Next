package me.ykrank.s1next.view.page.post.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewHolder
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.R
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.binding.ViewBindingAdapter
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.Vote
import me.ykrank.s1next.data.pref.GeneralPreferencesManager
import me.ykrank.s1next.databinding.ItemPostBlackBinding
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate
import me.ykrank.s1next.view.page.post.viewmodel.PostBlackViewModel
import me.ykrank.s1next.widget.span.FixedSpannableFactory
import me.ykrank.s1next.widget.span.PostMovementMethod

class PostBlackAdapterDelegate(
    private val fragment: Fragment,
    context: Context,
    private val mEventBus: EventBus,
    private val mGeneralPreferencesManager: GeneralPreferencesManager
) :
    BaseAdapterDelegate<Post, PostBlackViewHolder>(
        context,
        Post::class.java
    ) {

    private var threadInfo: Thread? = null
    private var voteInfo: Vote? = null
    private var pageNum: Int = 1

    private fun setTextSelectable(binding: ItemPostBlackBinding, selectable: Boolean) {
        binding.authorName.setTextIsSelectable(selectable)

        binding.tvReply.setTextIsSelectable(selectable)
        binding.tvReply.movementMethod = PostMovementMethod.instance
    }

    override fun isForViewType(items: MutableList<Any>, position: Int): Boolean {
        val item = items[position]
        return item is Post && item.hide != Post.HIDE_NO
    }

    public override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemPostBlackBinding.inflate(mLayoutInflater, parent, false)
        val viewModel = PostBlackViewModel(fragment.viewLifecycleOwner, mEventBus)
        binding.avatar.setOnClickListener(viewModel::onAvatarClick)
        binding.avatar.setOnLongClickListener(viewModel::onAvatarLongClick)
        binding.tvFloor.setOnClickListener(viewModel::onFloorClick)
        TextViewBindingAdapter.increaseClickingArea(
            binding.tvFloor,
            binding.tvFloor.resources.getDimension(
                com.github.ykrank.androidtools.R.dimen.minimum_touch_target_size
            )
        )

        binding.tvReply.setSpannableFactory(FixedSpannableFactory())

        setTextSelectable(binding, false)

        return PostBlackViewHolder(binding, viewModel)
    }

    override fun onBindViewHolderData(
        post: Post,
        position: Int,
        holder: PostBlackViewHolder,
        payloads: List<Any>
    ) {
        val binding = holder.binding

        ViewBindingAdapter.setMarginEnd(
            binding.contentContainer,
            if (mGeneralPreferencesManager.isQuickSideBarEnable) {
                binding.root.resources.getDimension(com.github.ykrank.androidtools.R.dimen.spacing_normal)
            } else {
                0f
            }
        )

        val selectable = false
        if (selectable != binding.tvReply.isTextSelectable) {
            setTextSelectable(binding, selectable)
        }

        holder.viewModel.thread = threadInfo
        holder.viewModel.pageNum = pageNum
        holder.viewModel.post = post
        holder.viewModel.vote = if ("1" == post.number) voteInfo else null
        binding.threadTitle.text = threadInfo?.title
        binding.threadTitle.visibility =
            if (pageNum == 1 && post.isFirst && threadInfo?.title != null) View.VISIBLE else View.GONE
        ImageViewBindingAdapter.loadAvatar(binding.avatar, null, post.authorId)
        binding.authorName.text = post.authorName
        binding.originalPosterTag.visibility = if (post.isOpPost) View.VISIBLE else View.GONE
        binding.tvFloor.text = holder.viewModel.floor
        TextViewBindingAdapter.setReply(binding.tvReply, null, null, fragment.viewLifecycleOwner, post)
    }

    // Bug workaround for losing text selection ability, see:
    // https://code.google.com/p/android/issues/detail?id=208169
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (false) {
            val binding = (holder as SimpleRecycleViewHolder<ItemPostBlackBinding>).binding
            binding.authorName.isEnabled = false
            binding.tvReply.isEnabled = false
            binding.authorName.isEnabled = true
            binding.tvReply.isEnabled = true
        }
    }

    fun setThreadInfo(threadInfo: Thread, pageNum: Int) {
        this.threadInfo = threadInfo
        this.pageNum = pageNum
    }

    fun setVoteInfo(voteInfo: Vote?) {
        this.voteInfo = voteInfo
    }

}

class PostBlackViewHolder(
    binding: ItemPostBlackBinding,
    val viewModel: PostBlackViewModel
) : SimpleRecycleViewHolder<ItemPostBlackBinding>(binding)
