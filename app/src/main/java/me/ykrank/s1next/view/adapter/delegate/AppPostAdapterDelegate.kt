package me.ykrank.s1next.view.adapter.delegate

import android.app.Activity
import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import com.github.ykrank.androidtools.binding.LibTextViewBindingAdapter
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewHolder
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.R
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.app.model.AppPost
import me.ykrank.s1next.data.api.app.model.AppThread
import me.ykrank.s1next.databinding.ItemAppPostBinding
import me.ykrank.s1next.viewmodel.AppPostViewModel
import me.ykrank.s1next.widget.span.PostMovementMethod

class AppPostAdapterDelegate(
    activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val quotePid: String?,
    private val mEventBus: EventBus,
    private val mUser: User
) : BaseAdapterDelegate<AppPost, AppPostViewHolder>(
    activity,
    AppPost::class.java
) {

    private var threadInfo: AppThread? = null

    private fun setTextSelectable(binding: ItemAppPostBinding, selectable: Boolean) {
        binding.authorName.setTextIsSelectable(selectable)
        binding.tvFloor.setTextIsSelectable(selectable)
        binding.tvReply.setTextIsSelectable(selectable)
        binding.authorName.movementMethod = LinkMovementMethod.getInstance()
        binding.tvFloor.movementMethod = LinkMovementMethod.getInstance()
        binding.tvReply.movementMethod = PostMovementMethod.instance
        binding.tvFloor.isLongClickable = false
    }

    public override fun onCreateViewHolder(parent: ViewGroup): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        val binding = ItemAppPostBinding.inflate(mLayoutInflater, parent, false)
        val viewModel = AppPostViewModel(lifecycleOwner, mEventBus, mUser)
        binding.avatar.setOnClickListener(viewModel::onAvatarClick)
        binding.avatar.setOnLongClickListener(viewModel::onLongClick)
        binding.tvFloor.setOnClickListener(viewModel::showFloorActionMenu)
        binding.tvShowTrade.setOnClickListener(viewModel::onTradeHtmlClick)
        TextViewBindingAdapter.increaseClickingArea(
            binding.tvFloor,
            binding.tvFloor.resources.getDimension(
                com.github.ykrank.androidtools.R.dimen.minimum_touch_target_size
            )
        )

        setTextSelectable(binding, false)

        return AppPostViewHolder(binding, viewModel)
    }

    override fun onBindViewHolderData(post: AppPost, position: Int, holder: AppPostViewHolder, payloads: List<Any>) {
        val binding = holder.binding

        val selectable = false
        if (selectable != binding.tvReply.isTextSelectable) {
            setTextSelectable(binding, selectable)
        }

        holder.viewModel.thread.set(threadInfo)
        holder.viewModel.post.set(post)
        binding.authorName.text = post.author
        LibTextViewBindingAdapter.setRelativeDateTime(binding.tvDatetime, post.dateline * 1000)
        binding.tvFloor.text = holder.viewModel.floor.get()
        TextViewBindingAdapter.setReply(binding.tvReply, null, null, lifecycleOwner, post)
        ImageViewBindingAdapter.loadAvatar(binding.avatar, null, post.authorId.toString())
        binding.tvShowTrade.visibility = if (post.trade) View.VISIBLE else View.GONE
        val quote = post.pid == quotePid?.toInt()
        if (quote) {
            binding.container.setBackgroundResource(com.github.ykrank.androidtools.R.drawable.shape_stroke_corners_wide)
        } else {
            binding.container.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    // Bug workaround for losing text selection ability, see:
    // https://code.google.com/p/android/issues/detail?id=208169
    override fun onViewAttachedToWindow(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (false) {
            val binding = (holder as SimpleRecycleViewHolder<ItemAppPostBinding>).binding
            binding.authorName.isEnabled = false
            binding.tvFloor.isEnabled = false
            binding.tvReply.isEnabled = false
            binding.authorName.isEnabled = true
            binding.tvFloor.isEnabled = true
            binding.tvReply.isEnabled = true
        }
    }

    fun setThreadInfo(threadInfo: AppThread) {
        this.threadInfo = threadInfo
    }
}

class AppPostViewHolder(
    binding: ItemAppPostBinding,
    val viewModel: AppPostViewModel
) : SimpleRecycleViewHolder<ItemAppPostBinding>(binding)
