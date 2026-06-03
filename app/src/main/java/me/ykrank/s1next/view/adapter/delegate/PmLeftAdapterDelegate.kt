package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.text.TextUtils
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.binding.LibTextViewBindingAdapter
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.model.Pm
import me.ykrank.s1next.databinding.ItemPmLeftBinding
import me.ykrank.s1next.view.activity.UserHomeActivity
import me.ykrank.s1next.widget.span.PostMovementMethod

class PmLeftAdapterDelegate(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val user: User
) : BaseAdapterDelegate<Pm, PmLeftAdapterDelegate.ViewHolder>(context, Pm::class.java) {

    override fun isForViewType(items: MutableList<Any>, position: Int): Boolean {
        val item = items[position]
        return if (item is Pm) {
            !TextUtils.equals(item.authorId, user.uid)
        } else false
    }

    public override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemPmLeftBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(binding, lifecycleOwner)
    }

    override fun onBindViewHolderData(t: Pm, position: Int, holder: ViewHolder, payloads: List<Any>) {
        holder.bind(t)
    }

    /**
     * make textview selectable
     *
     * @param holder
     */
    override fun onViewAttachedToWindow(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        (holder as ViewHolder).refreshSelectableText()
    }

    class ViewHolder(
        private val binding: ItemPmLeftBinding,
        private val lifecycleOwner: LifecycleOwner
    ) : RecyclerView.ViewHolder(binding.root) {
        private var avatarUid: String? = null
        private var message: String? = null

        init {
            binding.tvMessage.movementMethod = PostMovementMethod.instance
        }

        fun bind(pm: Pm) {
            LibTextViewBindingAdapter.setRelativeDateTime(binding.tvTime, pm.dateline * 1000)
            ImageViewBindingAdapter.loadAvatar(binding.avatar, oldUid = avatarUid, newUid = pm.authorId)
            avatarUid = pm.authorId
            binding.avatar.setOnClickListener {
                pm.authorId?.let { uid ->
                    UserHomeActivity.start(it.context, uid, pm.author, it)
                }
            }
            TextViewBindingAdapter.setHtmlWithImage(
                binding.tvMessage,
                lifecycleOwner,
                message,
                lifecycleOwner,
                pm.message
            )
            message = pm.message
        }

        fun refreshSelectableText() {
            binding.tvMessage.isEnabled = false
            binding.tvMessage.isEnabled = true
        }
    }

}
