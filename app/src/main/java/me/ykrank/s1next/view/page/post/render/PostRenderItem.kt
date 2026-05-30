package me.ykrank.s1next.view.page.post.render

import com.github.ykrank.androidtools.ui.adapter.StableIdModel
import com.github.ykrank.androidtools.ui.adapter.model.DiffSameItem
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Rate

sealed class PostRenderItem(
    val post: Post,
    private val typeId: Int,
    private val blockIndex: Int = 0
) : StableIdModel, DiffSameItem {

    val postId: Int
        get() = post.id

    override val stableId: Long
        get() = post.id.toLong() * STABLE_ID_FACTOR + typeId * TYPE_FACTOR + blockIndex

    override fun isSameItem(other: Any?): Boolean {
        return other is PostRenderItem && stableId == other.stableId
    }

    override fun isSameContent(other: Any?): Boolean {
        return this == other
    }

    data class Header(
        val data: Post,
    ) : PostRenderItem(data, TYPE_HEADER)

    data class TextBlock(
        val data: Post,
        val html: String,
        val index: Int,
    ) : PostRenderItem(data, TYPE_TEXT, index)

    data class ImageBlock(
        val data: Post,
        val url: String,
        val width: Int?,
        val height: Int?,
        val index: Int,
    ) : PostRenderItem(data, TYPE_IMAGE, index)

    data class FallbackHtmlBlock(
        val data: Post,
        val html: String,
        val index: Int,
    ) : PostRenderItem(data, TYPE_FALLBACK, index)

    data class Footer(
        val data: Post,
    ) : PostRenderItem(data, TYPE_FOOTER)

    companion object {
        private const val STABLE_ID_FACTOR = 10_000L
        private const val TYPE_FACTOR = 1_000
        private const val TYPE_HEADER = 1
        private const val TYPE_TEXT = 2
        private const val TYPE_IMAGE = 3
        private const val TYPE_FALLBACK = 4
        private const val TYPE_FOOTER = 5
    }
}

data class PostRenderIndex(
    val headerIndexByPid: Map<Int, Int>,
    val footerIndexByPid: Map<Int, Int>,
    val rangeByPid: Map<Int, IntRange>,
) {
    fun firstPosition(pid: Int): Int? = headerIndexByPid[pid] ?: rangeByPid[pid]?.first

    companion object {
        val EMPTY = PostRenderIndex(emptyMap(), emptyMap(), emptyMap())
    }
}

data class PostRenderResult(
    val items: List<Any>,
    val index: PostRenderIndex,
)

data class PostRatePayload(val rates: List<Rate>)

