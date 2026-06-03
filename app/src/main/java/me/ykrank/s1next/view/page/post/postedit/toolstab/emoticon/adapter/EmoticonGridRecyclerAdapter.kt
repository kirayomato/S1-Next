package me.ykrank.s1next.view.page.post.postedit.toolstab.emoticon.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup

import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.github.ykrank.androidtools.widget.EventBus

import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.data.api.model.Emoticon
import me.ykrank.s1next.databinding.ItemEmoticonBinding
import me.ykrank.s1next.view.event.EmoticonClickEvent

class EmoticonGridRecyclerAdapter(
    activity: Activity,
    private val mEmoticons: List<Emoticon>,
    private val mEventBus: EventBus
) : androidx.recyclerview.widget.RecyclerView.Adapter<EmoticonGridRecyclerAdapter.BindingViewHolder>() {

    private val mLayoutInflater: LayoutInflater = activity.layoutInflater
    private val mEmoticonRequestBuilder: RequestManager = Glide.with(activity)

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingViewHolder {
        val binding = ItemEmoticonBinding.inflate(mLayoutInflater, parent, false)
        return BindingViewHolder(binding, mEmoticonRequestBuilder, mEventBus)
    }

    override fun onBindViewHolder(holder: BindingViewHolder, position: Int) {
        holder.bind(mEmoticons[position])
    }

    override fun onViewRecycled(holder: BindingViewHolder) {
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int {
        return mEmoticons.size
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    class BindingViewHolder(
        private val binding: ItemEmoticonBinding,
        private val requestManager: RequestManager,
        private val eventBus: EventBus
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(emoticon: Emoticon) {
            ImageViewBindingAdapter.loadEmoticon(binding.image, requestManager, emoticon)
            binding.image.setOnClickListener {
                eventBus.postDefault(EmoticonClickEvent(emoticon.entity))
            }
        }
    }
}
