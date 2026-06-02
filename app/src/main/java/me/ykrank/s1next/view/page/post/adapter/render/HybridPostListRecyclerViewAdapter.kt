package me.ykrank.s1next.view.page.post.adapter.render

import android.content.Context
import androidx.fragment.app.Fragment
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Profile
import me.ykrank.s1next.data.api.model.Rate
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.Vote
import me.ykrank.s1next.view.adapter.BaseRecyclerViewAdapter
import me.ykrank.s1next.view.page.post.adapter.PostBlackAdapterDelegate
import me.ykrank.s1next.view.page.post.render.PostRenderIndex
import me.ykrank.s1next.view.page.post.render.PostRenderItem
import me.ykrank.s1next.view.page.post.render.PostRenderMapper
import me.ykrank.s1next.view.page.post.render.PostRenderResult
import me.ykrank.s1next.view.page.post.share.PostShareSelectionOwner
import me.ykrank.s1next.view.page.post.share.PostShareSelectionPayload

class HybridPostListRecyclerViewAdapter(
    fragment: Fragment,
    context: Context,
    postShareSelectionOwner: PostShareSelectionOwner? = null,
) : BaseRecyclerViewAdapter(context, true) {
    private val mapper = PostRenderMapper()
    private val headerDelegate = PostRenderHeaderAdapterDelegate(fragment, context, postShareSelectionOwner)
    private val textDelegate = PostRenderTextAdapterDelegate(fragment, context, postShareSelectionOwner)
    private val imageDelegate = PostRenderImageAdapterDelegate(fragment, context, postShareSelectionOwner) { imageUrls }
    private val fallbackDelegate = PostRenderFallbackAdapterDelegate(fragment, context, postShareSelectionOwner)
    private val footerDelegate = PostRenderFooterAdapterDelegate(fragment, context, postShareSelectionOwner)
    private val postBlackAdapterDelegate = PostBlackAdapterDelegate(fragment, context)

    var renderIndex: PostRenderIndex = PostRenderIndex.EMPTY
        private set

    private var postIds: List<Int> = emptyList()
    private var imageUrls: List<String> = emptyList()

    init {
        addAdapterDelegate(headerDelegate)
        addAdapterDelegate(textDelegate)
        addAdapterDelegate(imageDelegate)
        addAdapterDelegate(fallbackDelegate)
        addAdapterDelegate(footerDelegate)
        addAdapterDelegate(postBlackAdapterDelegate)
    }

    fun buildRenderResult(posts: List<Post>): PostRenderResult {
        return mapper.map(posts)
    }

    fun submitRenderResult(result: PostRenderResult, detectMoves: Boolean, callback: (() -> Unit)? = null) {
        renderIndex = result.index
        postIds = result.items.mapNotNull {
            when (it) {
                is PostRenderItem.Header -> it.post.id
                is Post -> it.id
                else -> null
            }
        }
        imageUrls = result.items.filterIsInstance<PostRenderItem.ImageBlock>().map { it.url }
        diffNewDataSet(result.items, detectMoves, callback)
    }

    fun setThreadInfo(threadInfo: Thread, pageNum: Int) {
        headerDelegate.setThreadInfo(threadInfo, pageNum)
        textDelegate.setThreadInfo(threadInfo, pageNum)
        imageDelegate.setThreadInfo(threadInfo, pageNum)
        fallbackDelegate.setThreadInfo(threadInfo, pageNum)
        footerDelegate.setThreadInfo(threadInfo, pageNum)
        postBlackAdapterDelegate.setThreadInfo(threadInfo, pageNum)
    }

    fun setVoteInfo(voteInfo: Vote?) {
        footerDelegate.setVoteInfo(voteInfo)
        postBlackAdapterDelegate.setVoteInfo(voteInfo)
    }

    fun notifyRatesChanged(pid: Int, rates: List<Rate>) {
        val index = renderIndex.footerIndexByPid[pid] ?: return
        notifyItemChanged(index, rates)
    }

    fun notifyProfileChanged(uid: String, profile: Profile) {
        headerDelegate.setAuthorProfile(uid, profile)
        dataSet.forEachIndexed { index, item ->
            val post = (item as? PostRenderItem)?.post ?: item as? Post ?: return@forEachIndexed
            if (post.authorId == uid) {
                notifyItemChanged(index, profile)
            }
        }
    }

    fun firstPositionForPostId(pid: Int): Int? = renderIndex.firstPosition(pid)

    fun postPositionForAdapterPosition(adapterPosition: Int): Int? {
        val post = when (val item = dataSet.getOrNull(adapterPosition)) {
            is PostRenderItem -> item.post
            is Post -> item
            else -> null
        } ?: return null
        return postIds.indexOf(post.id).takeIf { it >= 0 }
    }

    fun notifyPostShareSelectionChanged(postIds: Set<Int>?) {
        if (postIds == null) {
            notifyItemRangeChanged(0, itemCount, PostShareSelectionPayload)
            return
        }
        postIds.forEach { postId ->
            renderIndex.rangeByPid[postId]?.forEach { position ->
                notifyItemChanged(position, PostShareSelectionPayload)
            }
        }
    }
}
