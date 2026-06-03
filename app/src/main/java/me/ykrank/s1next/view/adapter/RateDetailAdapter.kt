package me.ykrank.s1next.view.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.ui.adapter.LibBaseRecyclerViewAdapter
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.data.api.model.Rate
import me.ykrank.s1next.databinding.ItemRateDetailBinding
import me.ykrank.s1next.databinding.ItemRateDetailMultiBinding
import me.ykrank.s1next.view.activity.UserHomeActivity
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate

class RateDetailAdapter(
    context: Context,
    mode: Mode,
) : LibBaseRecyclerViewAdapter(context, true) {

    enum class Mode {
        COMPACT,
        MULTI,
    }

    init {
        addAdapterDelegate(RateDetailAdapterDelegate(context, mode))
    }

    private class RateDetailAdapterDelegate(
        context: Context,
        private val mode: Mode,
    ) : BaseAdapterDelegate<Rate, RateDetailViewHolder>(context, Rate::class.java) {

        override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            return when (mode) {
                Mode.COMPACT -> {
                    val binding = ItemRateDetailBinding.inflate(mLayoutInflater, parent, false)
                    RateDetailViewHolder(
                        binding.root,
                        binding.avatar,
                        binding.tvUsername,
                        binding.tvScore,
                        binding.tvContent,
                        useBlacklistText = true,
                    )
                }

                Mode.MULTI -> {
                    val binding = ItemRateDetailMultiBinding.inflate(mLayoutInflater, parent, false)
                    RateDetailViewHolder(
                        binding.root,
                        binding.avatar,
                        binding.tvUsername,
                        binding.tvScore,
                        binding.tvContent,
                        useBlacklistText = false,
                    )
                }
            }
        }

        override fun onBindViewHolderData(
            t: Rate,
            position: Int,
            holder: RateDetailViewHolder,
            payloads: List<Any>,
        ) {
            holder.bind(t)
        }
    }

    private class RateDetailViewHolder(
        root: View,
        private val avatar: ImageView,
        private val username: TextView,
        private val score: TextView,
        private val content: TextView,
        private val useBlacklistText: Boolean,
    ) : RecyclerView.ViewHolder(root) {
        private var avatarUid: String? = null

        fun bind(rate: Rate) {
            username.text = rate.uname
            score.text = if (useBlacklistText) rate.blacklistScore else rate.symbolScore
            content.text = if (useBlacklistText) rate.blacklistContent(content.context) else rate.content
            ImageViewBindingAdapter.loadAvatar(avatar, oldUid = avatarUid, newUid = rate.uid)
            avatarUid = rate.uid

            avatar.setOnClickListener {
                val uid = rate.uid
                val uname = rate.uname
                if (uid != null && uname != null) {
                    UserHomeActivity.start(it.context, uid, uname, it)
                }
            }
        }
    }
}
