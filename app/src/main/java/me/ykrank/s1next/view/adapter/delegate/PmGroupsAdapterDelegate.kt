package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.binding.LibTextViewBindingAdapter
import com.github.ykrank.androidtools.binding.LibViewBindingAdapter
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.model.PmGroup
import me.ykrank.s1next.databinding.ItemPmGroupBinding
import me.ykrank.s1next.view.event.PmGroupClickEvent

class PmGroupsAdapterDelegate(
    context: Context,
    private val mEventBus: EventBus,
    private val mUser: User
) : BaseAdapterDelegate<PmGroup, PmGroupsAdapterDelegate.ViewHolder>(context, PmGroup::class.java) {

    public override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemPmGroupBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(binding, mEventBus, mUser)
    }

    override fun onBindViewHolderData(t: PmGroup, position: Int, holder: ViewHolder, payloads: List<Any>) {
        holder.bind(t)
    }

    class ViewHolder(
        private val binding: ItemPmGroupBinding,
        private val eventBus: EventBus,
        private val user: User
    ) : RecyclerView.ViewHolder(binding.root) {
        private var avatarUid: String? = null

        fun bind(pmGroup: PmGroup) {
            val tint = if (pmGroup.isNew) {
                binding.root.context.getColor(com.github.ykrank.androidtools.R.color.red_A100)
            } else {
                Int.MIN_VALUE
            }
            LibViewBindingAdapter.setCardBackgroundTint(binding.root, null, tint)
            ImageViewBindingAdapter.loadAvatar(binding.avatar, oldUid = avatarUid, newUid = pmGroup.toUid)
            avatarUid = pmGroup.toUid
            TextViewBindingAdapter.setPmAuthorNameDesc(binding.authorName, pmGroup, user)
            LibTextViewBindingAdapter.setRelativeDateTime(binding.lastTime, pmGroup.lastDateline * 1000)
            binding.tvCount.text = pmGroup.pmNum
            binding.tvSummary.text = pmGroup.lastSummary
            binding.content.setOnClickListener {
                eventBus.postDefault(PmGroupClickEvent(pmGroup))
            }
        }
    }
}
