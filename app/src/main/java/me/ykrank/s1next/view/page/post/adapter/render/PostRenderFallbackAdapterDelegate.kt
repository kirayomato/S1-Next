package me.ykrank.s1next.view.page.post.adapter.render

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import me.ykrank.s1next.R
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate
import me.ykrank.s1next.view.page.post.render.PostActionMenuPopup
import me.ykrank.s1next.view.page.post.render.PostRenderActions
import me.ykrank.s1next.view.page.post.render.PostRenderItem
import me.ykrank.s1next.widget.span.FixedSpannableFactory
import me.ykrank.s1next.widget.span.PostMovementMethod

class PostRenderFallbackAdapterDelegate(private val fragment: Fragment, context: Context) :
    BaseAdapterDelegate<PostRenderItem.FallbackHtmlBlock, PostRenderFallbackAdapterDelegate.ViewHolder>(
        context,
        PostRenderItem.FallbackHtmlBlock::class.java
    ) {
    private var threadInfo: Thread? = null
    private var pageNum: Int = 1

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view = mLayoutInflater.inflate(R.layout.item_post_render_text, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolderData(
        t: PostRenderItem.FallbackHtmlBlock,
        position: Int,
        holder: ViewHolder,
        payloads: List<Any>
    ) {
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
        val longClickListener = View.OnLongClickListener {
            PostRenderActions.showPostActionMenu(it, fragment, threadInfo, pageNum, t.post)
        }
        val touchListener = View.OnTouchListener { view, event ->
            PostActionMenuPopup.recordTouchPoint(view, event)
            false
        }
        holder.itemView.setOnTouchListener(touchListener)
        holder.text.setOnTouchListener(touchListener)
        holder.itemView.setOnLongClickListener(longClickListener)
        holder.text.setOnLongClickListener(longClickListener)
    }

    fun setThreadInfo(threadInfo: Thread, pageNum: Int) {
        this.threadInfo = threadInfo
        this.pageNum = pageNum
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.post_text)
        var boundHtml: String? = null
    }
}
