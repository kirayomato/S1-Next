package me.ykrank.s1next.view.page.post.adapter.render

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.R
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate
import me.ykrank.s1next.view.page.post.render.PostActionMenuPopup
import me.ykrank.s1next.view.page.post.render.PostRenderActions
import me.ykrank.s1next.view.page.post.render.PostRenderItem
import me.ykrank.s1next.view.page.post.share.PostShareSelectionOwner
import me.ykrank.s1next.view.page.post.share.PostShareSelectionPayload
import me.ykrank.s1next.widget.span.FixedSpannableFactory
import me.ykrank.s1next.widget.span.PostMovementMethod

class PostRenderTextAdapterDelegate(
    private val fragment: Fragment,
    context: Context,
    private val postShareSelectionOwner: PostShareSelectionOwner? = null,
    private val eventBus: EventBus,
    private val user: User
) :
    BaseAdapterDelegate<PostRenderItem.TextBlock, PostRenderTextAdapterDelegate.ViewHolder>(
        context,
        PostRenderItem.TextBlock::class.java
    ) {
    private var threadInfo: Thread? = null
    private var pageNum: Int = 1

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = mLayoutInflater.inflate(R.layout.item_post_render_text, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolderData(
        t: PostRenderItem.TextBlock,
        position: Int,
        holder: ViewHolder,
        payloads: List<Any>
    ) {
        if (payloads.contains(PostShareSelectionPayload)) {
            bindShareSelection(t, holder)
            return
        }
        holder.text.setSpannableFactory(FixedSpannableFactory())
        holder.text.movementMethod = PostMovementMethod.instance
        TextViewBindingAdapter.setHtmlWithImage(
            holder.text,
            null,
            holder.boundHtml,
            fragment.viewLifecycleOwner,
            t.html
        )
        holder.boundHtml = t.html
        bindShareSelection(t, holder)
    }

    private fun bindShareSelection(t: PostRenderItem.TextBlock, holder: ViewHolder) {
        holder.shareScrim.bindPostShareSelectionScrim(postShareSelectionOwner, t.post.id)
        if (postShareSelectionOwner?.postShareSelectionState?.enabled == true) {
            val clickListener = View.OnClickListener {
                postShareSelectionOwner.togglePostShareSelection(t.post.id)
            }
            holder.itemView.setOnTouchListener(null)
            holder.text.setOnTouchListener(null)
            holder.itemView.setOnClickListener(clickListener)
            holder.text.setOnClickListener(clickListener)
            holder.shareScrim.setOnClickListener(clickListener)
            holder.itemView.setOnLongClickListener(null)
            holder.text.setOnLongClickListener(null)
        } else {
            val longClickListener = View.OnLongClickListener {
                PostRenderActions.showPostActionMenu(it, fragment, eventBus, user, threadInfo, pageNum, t.post)
            }
            val touchListener = View.OnTouchListener { view, event ->
                PostActionMenuPopup.recordTouchPoint(view, event)
                false
            }
            holder.itemView.setOnTouchListener(touchListener)
            holder.text.setOnTouchListener(touchListener)
            holder.itemView.setOnClickListener(null)
            holder.text.setOnClickListener(null)
            holder.shareScrim.setOnClickListener(null)
            holder.shareScrim.isClickable = false
            holder.itemView.setOnLongClickListener(longClickListener)
            holder.text.setOnLongClickListener(longClickListener)
        }
    }

    fun setThreadInfo(threadInfo: Thread, pageNum: Int) {
        this.threadInfo = threadInfo
        this.pageNum = pageNum
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.post_text)
        val shareScrim: View = itemView.findViewById(R.id.post_share_scrim)
        var boundHtml: String? = null
    }
}
