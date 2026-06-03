package me.ykrank.s1next.view.page.post.postlist

import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bigkoo.quicksidebar.QuickSideBarView
import com.bigkoo.quicksidebar.listener.OnQuickSideBarTouchListener
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ykrank.androidautodispose.AndroidRxDispose
import com.github.ykrank.androidlifecycle.event.FragmentEvent
import com.github.ykrank.androidtools.data.Resource
import com.github.ykrank.androidtools.ui.internal.LoadingViewModelBindingDelegate
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.util.LooperUtil
import com.github.ykrank.androidtools.util.ResourceUtil
import com.github.ykrank.androidtools.util.RxJavaUtil
import com.github.ykrank.androidtools.widget.recycleview.StartSnapLinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Single
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.ProfileProvider
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Profile
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.collection.Posts
import me.ykrank.s1next.data.api.model.wrapper.PostsWrapper
import me.ykrank.s1next.data.db.biz.ReadProgressBiz
import me.ykrank.s1next.data.db.dbmodel.ReadProgress
import me.ykrank.s1next.data.pref.DownloadPreferencesManager
import me.ykrank.s1next.data.pref.GeneralPreferencesManager
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.databinding.FragmentBaseWithQuickSideBarBinding
import me.ykrank.s1next.view.event.BlackListChangeEvent
import me.ykrank.s1next.view.event.EnterPostShareSelectionEvent
import me.ykrank.s1next.view.event.QuickSidebarEnableChangeEvent
import me.ykrank.s1next.view.adapter.BaseRecyclerViewAdapter
import me.ykrank.s1next.view.fragment.BaseRecyclerViewFragment
import me.ykrank.s1next.view.internal.LoadingViewModelBindingDelegateQuickSidebarImpl
import me.ykrank.s1next.view.internal.PagerScrollState
import me.ykrank.s1next.view.page.app.AppPostListActivity
import me.ykrank.s1next.view.page.post.adapter.PostListRecyclerViewAdapter
import me.ykrank.s1next.view.page.post.adapter.render.HybridPostListRecyclerViewAdapter
import me.ykrank.s1next.view.page.post.share.PostSharePreviewDialogFragment
import me.ykrank.s1next.view.page.post.share.PostShareRequest
import me.ykrank.s1next.view.page.post.share.PostShareSelectionPayload
import me.ykrank.s1next.view.page.post.share.PostShareSelectionOwner
import me.ykrank.s1next.view.page.post.share.PostShareSelectionState
import java.util.*
import javax.inject.Inject

/**
 * A Fragment representing one of the pages of posts.
 *
 *
 * Activity or Fragment containing this must implement [PagerCallback].
 */
@AndroidEntryPoint
class PostListPagerFragment : BaseRecyclerViewFragment<PostsWrapper>(),
    OnQuickSideBarTouchListener,
    PostShareSelectionOwner {

    @Inject
    internal lateinit var mGeneralPreferencesManager: GeneralPreferencesManager

    @Inject
    internal lateinit var mReadPreferencesManager: ReadPreferencesManager

    @Inject
    internal lateinit var objectMapper: ObjectMapper

    @Inject
    internal lateinit var profileProvider: ProfileProvider

    @Inject
    internal lateinit var downloadPreferencesManager: DownloadPreferencesManager

    @Inject
    internal lateinit var readProgressBiz: ReadProgressBiz

    private var mThreadId: String? = null
    private var mPageNum: Int = 0
    private var mThreadInfo: Thread? = null

    /**
     * Only see this author, or all if null
     */
    private var mAuthorId: String? = null

    /**
     * 之前记录的阅读进度
     */
    private var readProgress: ReadProgress? = null
    private var scrollState: PagerScrollState? = null
    private var blacklistChanged = false

    private lateinit var binding: FragmentBaseWithQuickSideBarBinding
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mRecyclerAdapter: BaseRecyclerViewAdapter
    private lateinit var mLayoutManager: StartSnapLinearLayoutManager
    private lateinit var quickSideBarView: QuickSideBarView
    private lateinit var quickSideBarTipsView: TextView
    private val letters = HashMap<String, Int>()
    private val searchHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var highlightedPostPosition = RecyclerView.NO_POSITION
    private val clearSearchHighlightRunnable = Runnable {
        highlightedPostPosition = RecyclerView.NO_POSITION
        if (this::mRecyclerView.isInitialized) {
            mRecyclerView.invalidateItemDecorations()
        }
    }
    private val searchHighlightDecoration = object : RecyclerView.ItemDecoration() {
        override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
            if (highlightedPostPosition == RecyclerView.NO_POSITION) {
                return
            }
            for (i in 0 until parent.childCount) {
                val child = parent.getChildAt(i)
                if (parent.getChildAdapterPosition(child) == highlightedPostPosition) {
                    c.drawRect(
                        child.left.toFloat(),
                        child.top.toFloat(),
                        child.right.toFloat(),
                        child.bottom.toFloat(),
                        searchHighlightPaint
                    )
                    break
                }
            }
        }
    }

    private var mPagerCallback: PagerCallback? = null
    private var currentPosts: List<Post> = emptyList()
    override var postShareSelectionState: PostShareSelectionState = PostShareSelectionState()
        private set

    private var refreshAfterBlacklistChangeDisposable: Disposable? = null

    val pageNum: Int
        get() = mPageNum

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mPagerCallback = parentFragment as PagerCallback

        val bundle = requireArguments()
        mThreadId = bundle.getString(ARG_THREAD_ID)
        mPageNum = bundle.getInt(ARG_PAGE_NUM)
        mAuthorId = bundle.getString(ARG_AUTHOR_ID)
        if (readProgress == null) {
            readProgress = bundle.getParcelable(ARG_READ_PROGRESS)
            scrollState = bundle.getParcelable(ARG_PAGER_SCROLL_STATE)
        }
        leavePageMsg("PostListPagerFragment##ThreadId:$mThreadId,PageNum:$mPageNum")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mRecyclerView = recyclerView
        mLayoutManager = StartSnapLinearLayoutManager(requireActivity())
        mRecyclerView.layoutManager = mLayoutManager
        searchHighlightPaint.color = ColorUtils.setAlphaComponent(
            ResourceUtil.getAttrColorInt(requireContext(), androidx.appcompat.R.attr.colorAccent),
            SEARCH_RESULT_HIGHLIGHT_ALPHA
        )
        mRecyclerView.addItemDecoration(searchHighlightDecoration)
        mRecyclerAdapter = if (mReadPreferencesManager.hybridPostRender) {
            HybridPostListRecyclerViewAdapter(
                this,
                requireContext(),
                this,
                mEventBus,
                mUser,
                apiCacheProvider,
                mGeneralPreferencesManager,
                downloadPreferencesManager
            )
        } else {
            PostListRecyclerViewAdapter(
                this,
                requireContext(),
                this,
                mEventBus,
                mUser,
                apiCacheProvider,
                mGeneralPreferencesManager
            )
        }
        mRecyclerView.adapter = mRecyclerAdapter

        // add pull up to refresh to RecyclerView
        mRecyclerView.addOnScrollListener(object :
            androidx.recyclerview.widget.RecyclerView.OnScrollListener() {

            override fun onScrollStateChanged(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                newState: Int
            ) {
                if (!isPullUpToRefresh
                    && mPageNum == mPagerCallback?.getTotalPages()
                    && !isLoading
                    && mRecyclerAdapter.itemCount != 0
                    && !mRecyclerView.canScrollVertically(1)
                ) {
                    startPullToRefresh()
                }
            }

        })

        quickSideBarView.setOnQuickSideBarTouchListener(this)

        mEventBus.get()
            .ofType(QuickSidebarEnableChangeEvent::class.java)
            .to(AndroidRxDispose.withObservable(this, FragmentEvent.DESTROY_VIEW))
            .subscribe({ invalidateQuickSidebarVisible() }, { super.onError(it) })

        mEventBus.get()
            .ofType(BlackListChangeEvent::class.java)
            .to(AndroidRxDispose.withObservable(this, FragmentEvent.DESTROY_VIEW))
            .subscribe { startBlackListRefresh() }

        mEventBus.get()
            .ofType(EnterPostShareSelectionEvent::class.java)
            .to(AndroidRxDispose.withObservable(this, FragmentEvent.DESTROY_VIEW))
            .subscribe({ event ->
                if ((event.threadId == null || event.threadId == mThreadId) &&
                    event.pageNum == mPageNum &&
                    currentPosts.any { it.id == event.postId }
                ) {
                    startPostShareSelection(event.postId)
                }
            }, { super.onError(it) })

        parentFragmentManager.setFragmentResultListener(
            PostSharePreviewDialogFragment.RESULT_REQUEST_KEY,
            viewLifecycleOwner
        ) { _, result ->
            if (result.getBoolean(PostSharePreviewDialogFragment.RESULT_SHARED, false)) {
                cancelPostShareSelection()
            }
        }
    }

    override fun onDestroy() {
        RxJavaUtil.disposeIfNotNull(refreshAfterBlacklistChangeDisposable)
        if (this::mRecyclerView.isInitialized) {
            mRecyclerView.removeCallbacks(clearSearchHighlightRunnable)
        }
        mPagerCallback = null
        super.onDestroy()
    }

    override fun getLoadingViewModelBindingDelegateImpl(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): LoadingViewModelBindingDelegate {
        binding = FragmentBaseWithQuickSideBarBinding.inflate(inflater, container, false)
        binding.quickSideBarView.visibility = View.GONE
        quickSideBarView = binding.quickSideBarView
        quickSideBarTipsView = binding.quickSideBarViewTips
        return LoadingViewModelBindingDelegateQuickSidebarImpl(binding)
    }

    override fun startPullToRefresh() {
        if (isPullUpToRefreshValid) {
            mRecyclerAdapter.showFooterProgress()
            super.startPullToRefresh()
        }
    }

    /**
     * 黑名单更改后刷新当前帖子列表
     */
    internal fun startBlackListRefresh() {
        blacklistChanged = true
        startPullToRefresh()
    }

    internal fun loadReadProgressInRecycleView(readProgress: ReadProgress, smooth: Boolean) {
        this.readProgress = readProgress
        if (scrollState == null) {
            scrollState = PagerScrollState()
            scrollState!!.state = PagerScrollState.BEFORE_SCROLL_POSITION
        }
        if (!isLoading) {
            var position = readProgress.position
            var offset = readProgress.offset
            if (position <= 0) {
                //if position invalid or first, offset should below zero
                position = 0
                if (offset > 0) {
                    offset = 0
                }
            }
            val totalItemCount = mRecyclerAdapter.itemCount
            if (totalItemCount <= 0) {
                return
            }
            position = resolveReadProgressAdapterPosition(position)
            if (totalItemCount <= position) {
                position = totalItemCount - 1
            }
            if (smooth) {
                mLayoutManager.smoothScrollToPosition(position, offset)
            } else {
                mLayoutManager.scrollToPositionWithOffset(position, offset)
            }
        }
    }

    /**
     * 保存当前阅读进度
     */
    internal fun saveReadProgress() {
        val readProgress = curReadProgress
        if (readProgress != null) {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    readProgressBiz.saveReadProgress(readProgress)
                }
                showShortText(R.string.save_read_progress_success)
            }
        }
    }

    internal fun startPageSearch(fragment: Fragment, requestCode: Int) {
        val snapshot = buildPageSearchSnapshot()
        if (snapshot == null || snapshot.posts.isEmpty()) {
            showSnackbar(R.string.post_page_search_no_data)
            return
        }
        PostPageSearchActivity.startForResult(
            fragment,
            requestCode,
            snapshot.thread,
            mThreadId,
            snapshot.pageNum,
            mPagerCallback?.getTotalPages() ?: mPageNum,
            snapshot.posts,
            arrayListOf(snapshot)
        )
    }

    fun startPostShareSelection(sourcePostId: Int?) {
        val sourcePost = sourcePostId?.let { id ->
            currentPosts.firstOrNull { it.id == id && it.hide == Post.HIDE_NO }
        }
        val defaultPost = sourcePost ?: defaultShareSelectionPost()
        if (defaultPost == null) {
            showSnackbar(R.string.post_share_no_data)
            return
        }

        postShareSelectionState = PostShareSelectionState(
            enabled = true,
            selectedPostIds = setOf(defaultPost.id),
            sourcePostId = sourcePostId,
        )
        notifyPostShareSelectionChanged(null)
    }

    fun cancelPostShareSelection() {
        if (!postShareSelectionState.enabled) {
            return
        }
        postShareSelectionState = PostShareSelectionState()
        notifyPostShareSelectionChanged(null)
    }

    fun confirmPostShareSelection() {
        val posts = selectedSharePosts()
        if (posts.isEmpty()) {
            showSnackbar(R.string.post_share_no_data)
            return
        }
        val threadId = mThreadId ?: mThreadInfo?.id
        if (threadId.isNullOrBlank()) {
            showSnackbar(R.string.post_share_image_failed)
            return
        }
        PostSharePreviewDialogFragment.newInstance(
            PostShareRequest(
                threadId = threadId,
                threadTitle = mThreadInfo?.title,
                page = mPageNum,
                posts = ArrayList(posts),
            )
        ).show(parentFragmentManager, PostSharePreviewDialogFragment.TAG)
    }

    override fun togglePostShareSelection(postId: Int) {
        val state = postShareSelectionState
        if (!state.enabled || currentPosts.none { it.id == postId && it.hide == Post.HIDE_NO }) {
            return
        }
        val selectedPostIds = state.selectedPostIds
        val newSelectedPostIds = if (postId in selectedPostIds) {
            if (selectedPostIds.size == 1) {
                showSnackbar(R.string.post_share_at_least_one)
                return
            }
            selectedPostIds - postId
        } else {
            selectedPostIds + postId
        }
        postShareSelectionState = state.copy(selectedPostIds = newSelectedPostIds)
        notifyPostShareSelectionChanged(setOf(postId))
    }

    private fun selectedSharePosts(): List<Post> {
        val selectedPostIds = postShareSelectionState.selectedPostIds
        if (selectedPostIds.isEmpty()) {
            return emptyList()
        }
        return currentPosts.filter { it.hide == Post.HIDE_NO && it.id in selectedPostIds }
    }

    private fun defaultShareSelectionPost(): Post? {
        val adapterPosition = mLayoutManager.findFirstVisibleItemPosition()
        val postPosition = if (adapterPosition != RecyclerView.NO_POSITION) {
            val hybrid = mRecyclerAdapter as? HybridPostListRecyclerViewAdapter
            hybrid?.postPositionForAdapterPosition(adapterPosition) ?: adapterPosition
        } else {
            RecyclerView.NO_POSITION
        }
        return currentPosts.getOrNull(postPosition)?.takeIf { it.hide == Post.HIDE_NO }
            ?: currentPosts.firstOrNull { it.hide == Post.HIDE_NO }
    }

    private fun notifyPostShareSelectionChanged(changedPostIds: Set<Int>? = null) {
        if (this::mRecyclerAdapter.isInitialized) {
            val hybrid = mRecyclerAdapter as? HybridPostListRecyclerViewAdapter
            if (hybrid != null) {
                hybrid.notifyPostShareSelectionChanged(changedPostIds)
            } else if (changedPostIds == null) {
                mRecyclerAdapter.notifyItemRangeChanged(
                    0,
                    mRecyclerAdapter.itemCount,
                    PostShareSelectionPayload
                )
            } else {
                changedPostIds.forEach { postId ->
                    val index = currentPosts.indexOfFirst { it.id == postId }
                    if (index >= 0) {
                        mRecyclerAdapter.notifyItemChanged(index, PostShareSelectionPayload)
                    }
                }
            }
        }
        mPagerCallback?.onPostShareSelectionChanged(
            postShareSelectionState.enabled,
            postShareSelectionState.selectedPostIds.size
        )
    }

    internal fun buildPageSearchSnapshot(): PostPageSearchActivity.PageSnapshot? {
        val posts = ArrayList(currentPosts)
        if (posts.isEmpty()) {
            return null
        }
        return PostPageSearchActivity.PageSnapshot(mPageNum, mThreadInfo, posts)
    }

    internal fun scrollToPostPosition(position: Int, highlight: Boolean = false): Boolean {
        val adapterPosition = resolveAdapterPosition(position)
        if (adapterPosition == RecyclerView.NO_POSITION ||
            adapterPosition < 0 ||
            adapterPosition >= mRecyclerAdapter.itemCount
        ) {
            return false
        }
        mLayoutManager.scrollToPositionWithOffset(adapterPosition, 0)
        if (highlight) {
            highlightPostPosition(adapterPosition)
        }
        return true
    }

    private fun resolveAdapterPosition(postPosition: Int): Int {
        val hybrid = mRecyclerAdapter as? HybridPostListRecyclerViewAdapter ?: return postPosition
        val post = currentPosts.getOrNull(postPosition) ?: return RecyclerView.NO_POSITION
        return hybrid.firstPositionForPostId(post.id) ?: RecyclerView.NO_POSITION
    }

    private fun resolveReadProgressAdapterPosition(postPosition: Int): Int {
        val resolved = resolveAdapterPosition(postPosition)
        return if (resolved == RecyclerView.NO_POSITION) postPosition else resolved
    }

    private fun highlightPostPosition(position: Int) {
        highlightedPostPosition = position
        mRecyclerView.removeCallbacks(clearSearchHighlightRunnable)
        mRecyclerView.post {
            mRecyclerView.invalidateItemDecorations()
            mRecyclerView.postDelayed(
                clearSearchHighlightRunnable,
                SEARCH_RESULT_HIGHLIGHT_DURATION_MS
            )
        }
    }

    internal val curReadProgress: ReadProgress?
        get() {
            if (isLoading) {
                return null
            }
            val itemPosition = findNowItemPosition()
            return ReadProgress(
                mThreadId?.toInt()
                    ?: 0, mPageNum, itemPosition.first!!, itemPosition.second!!
            )
        }

    /**
     * 现在Item的位置

     * @return
     */
    private fun findNowItemPosition(): Pair<Int, Int> {
        val itemPosition = mLayoutManager.findFirstVisibleItemPosition()
            if (itemPosition == RecyclerView.NO_POSITION) {
                return Pair(0, 0)
            }
        var offset = 0
        val view = mLayoutManager.findViewByPosition(itemPosition)
        if (view != null) {
            //See LinearSmoothScroller#calculateDyToMakeVisible
            val params = view.layoutParams as androidx.recyclerview.widget.RecyclerView.LayoutParams
            val top = mLayoutManager.getDecoratedTop(view) - params.topMargin
            val start = mLayoutManager.paddingTop

            offset = top - start
        }
        val hybrid = mRecyclerAdapter as? HybridPostListRecyclerViewAdapter
        if (hybrid != null) {
            val postPosition = hybrid.postPositionForAdapterPosition(itemPosition)
            if (postPosition != null) {
                val postId = currentPosts.getOrNull(postPosition)?.id
                val postStartPosition = postId?.let { hybrid.firstPositionForPostId(it) }
                return Pair(postPosition, if (postStartPosition == itemPosition) offset else 0)
            }
        }
        return Pair(itemPosition, offset)
    }

    override suspend fun getSource(loading: Int): Flow<Resource<PostsWrapper>> {
        return apiCacheProvider.getPostsWrapper(
            mThreadId ?: "", mPageNum, mAuthorId,
            ignoreCache = isIgnoreCache,
            { pid, rates ->
                currentPosts.forEachIndexed { index, post ->
                    if (post.id == pid) {
                        post.rates = rates
                        val hybrid = mRecyclerAdapter as? HybridPostListRecyclerViewAdapter
                        if (hybrid != null) {
                            hybrid.notifyRatesChanged(pid, rates)
                        } else {
                            mRecyclerAdapter.notifyItemChanged(index)
                        }
                    }
                }
            }
        )
    }

    override fun onNextSuccess(resource: Resource.Success<PostsWrapper>) {
        super.onNextSuccess(resource)
        val data = resource.data
        val pullUpToRefresh = isPullUpToRefresh
        var postList: List<Post>? = null

        val posts = data?.data
        if (posts != null) {
            postList = posts.postList
        }

        // if user has logged out, has no permission to access this thread or this thread is invalid
        if (postList.isNullOrEmpty()) {
            if (pullUpToRefresh) {
                // mRecyclerAdapter.getItemCount() = 0
                // when configuration changes (like orientation changes)
                if (mRecyclerAdapter.itemCount != 0) {
                    mRecyclerAdapter.hideFooterProgress()
                }
            }

            if (resource.source.isNewData()) {
                val threadId = mThreadId ?: posts?.postListInfo?.id
                if (threadId != null) {
                    if (isAdded && userVisibleHint) {
                        showSnackbar(
                            data?.result?.message
                                ?: getString(R.string.message_load_error),
                            Snackbar.LENGTH_INDEFINITE,
                            R.string.click_to_cast_dark_magic, View.OnClickListener {
                                AppPostListActivity.start(
                                    requireContext(),
                                    threadId,
                                    mPageNum,
                                    null
                                )
                            }
                        )
                    }
                } else {
                    consumeResult(data?.result)
                }
            }
        } else {
            //Thread info must not null, or exception
            val postListInfo = posts?.postListInfo as Thread
            currentPosts = postList
            mThreadInfo = postListInfo
            loadAuthorProfiles(postList)
            val submitCallback: () -> Unit = {
                if (blacklistChanged) {
                    blacklistChanged = false
                } else if (pullUpToRefresh) {

                } else {
                    val searchResultPosition =
                        mPagerCallback?.consumePendingSearchResultPosition(mPageNum)
                    if (searchResultPosition != null) {
                        scrollToPostPosition(searchResultPosition, true)
                    } else if (readProgress != null && scrollState?.state == PagerScrollState.BEFORE_SCROLL_POSITION) {
                        mLayoutManager.scrollToPositionWithOffset(
                            resolveReadProgressAdapterPosition(readProgress!!.position),
                            readProgress!!.offset
                        )
                        readProgress = null
                        scrollState!!.state = PagerScrollState.FREE
                    } else {
                        val quotePostId = arguments?.getString(ARG_QUOTE_POST_ID)
                        if (!TextUtils.isEmpty(quotePostId)) {
                            for (i in postList.indices) {
                                if (quotePostId?.toInt() == postList[i].id) {
                                    // scroll to post post
                                    mLayoutManager.scrollToPositionWithOffset(
                                        resolveAdapterPosition(i),
                                        0
                                    )
                                    break
                                }
                            }
                            // clear this argument after redirecting
                            arguments?.putString(ARG_QUOTE_POST_ID, null)
                        }
                    }
                }
            }
            val hybrid = mRecyclerAdapter as? HybridPostListRecyclerViewAdapter
            if (hybrid != null) {
                hybrid.setThreadInfo(postListInfo, mPageNum)
                hybrid.setVoteInfo(posts.vote)
                val renderResult = hybrid.buildRenderResult(postList)
                initQuickSidebar(postList) { pid, fallback ->
                    renderResult.index.firstPosition(pid) ?: fallback
                }
                hybrid.submitRenderResult(renderResult, true, submitCallback)
            } else {
                val legacy = mRecyclerAdapter as PostListRecyclerViewAdapter
                legacy.setThreadInfo(postListInfo, mPageNum)
                posts.vote?.let {
                    legacy.setVoteInfo(it)
                }
                initQuickSidebar(postList) { _, fallback -> fallback }
                legacy.diffNewDataSet(postList, true, submitCallback)
            }

            mPagerCallback?.setThreadInfo(postListInfo)
            posts.threadAttachment?.let {
                mPagerCallback?.setupThreadAttachment(it)
            }
        }
    }

    private fun loadAuthorProfiles(posts: List<Post>) {
        val authorIds = posts.mapNotNull { it.authorId }.distinct()
        if (authorIds.isEmpty()) {
            return
        }
        profileProvider.getProfiles(authorIds) { uid, profile ->
            if (!isAdded || !this::mRecyclerAdapter.isInitialized) {
                return@getProfiles
            }
            notifyAuthorProfileChanged(uid, profile)
        }
    }

    private fun notifyAuthorProfileChanged(uid: String, profile: Profile) {
        val hybrid = mRecyclerAdapter as? HybridPostListRecyclerViewAdapter
        if (hybrid != null) {
            hybrid.notifyProfileChanged(uid, profile)
            return
        }
        (mRecyclerAdapter as? PostListRecyclerViewAdapter)?.notifyProfileChanged(uid, profile)
    }

    override fun onError(throwable: Throwable) {
        //网络请求失败下依然刷新黑名单
        if (blacklistChanged) {
            blacklistChanged = false
            RxJavaUtil.disposeIfNotNull(refreshAfterBlacklistChangeDisposable)
            val posts = if (currentPosts.isNotEmpty()) currentPosts else mRecyclerAdapter.dataSet
            refreshAfterBlacklistChangeDisposable = Single.just(posts)
                .map { filterPostAfterBlacklistChanged(it) }
                .compose(RxJavaUtil.iOSingleTransformer())
                .subscribe({
                    val filteredPosts = it.filterIsInstance<Post>()
                    currentPosts = filteredPosts
                    val hybrid = mRecyclerAdapter as? HybridPostListRecyclerViewAdapter
                    if (hybrid != null) {
                        val result = hybrid.buildRenderResult(filteredPosts)
                        initQuickSidebar(filteredPosts) { pid, fallback ->
                            result.index.firstPosition(pid) ?: fallback
                        }
                        hybrid.submitRenderResult(result, false)
                    } else {
                        initQuickSidebar(filteredPosts) { _, fallback -> fallback }
                        mRecyclerAdapter.diffNewDataSet(it, false)
                    }
                }, { L.report(it) })
        } else if (isPullUpToRefresh) {
            mRecyclerAdapter.hideFooterProgress()
        }

        super.onError(throwable)
    }

    internal fun invalidateQuickSidebarVisible(): Boolean {
        val enable = mGeneralPreferencesManager.isQuickSideBarEnable
        binding.quickSideBarView.visibility = if (enable) View.VISIBLE else View.GONE

        if (!isLoading) {
            //Post notify
            mRecyclerAdapter.notifyDataSetChanged()
        }
        return enable
    }

    private fun initQuickSidebar(posts: List<Post>, positionResolver: (pid: Int, fallback: Int) -> Int) {
        invalidateQuickSidebarVisible()
        letters.clear()
        val customLetters = ArrayList<String>()
        var i = 0
        posts.forEach {
            if (i >= 10 && i % 2 == 0) {
                // noop
            } else {
                it.number?.apply {
                    customLetters.add(this)
                    letters[this] = positionResolver(it.id, i)
                }
            }
            i++
        }
        quickSideBarView.letters = customLetters
    }

    override fun onLetterChanged(letter: String, position: Int, y: Float) {
        quickSideBarTipsView.text = letter
        //有此key则获取位置并滚动到该位置
        if (letters.containsKey(letter)) {
            mLayoutManager.scrollToPositionWithOffset(letters[letter] ?: 0, 0)
        }
    }

    override fun onLetterTouching(touching: Boolean) {
        //        quickSideBarTipsView.setVisibility(touching ? View.VISIBLE : View.INVISIBLE);
    }

    interface PagerCallback {

        /**
         * Gets [android.support.v4.view.PagerAdapter.getCount].
         */
        fun getTotalPages(): Int

        fun setupThreadAttachment(threadAttachment: Posts.ThreadAttachment)

        fun setThreadInfo(thread: Thread?)

        fun consumePendingSearchResultPosition(pageNum: Int): Int?

        fun onPostShareSelectionChanged(enabled: Boolean, selectedCount: Int)
    }

    companion object {
        val TAG = PostListPagerFragment::class.simpleName
        private const val SEARCH_RESULT_HIGHLIGHT_ALPHA = 56
        private const val SEARCH_RESULT_HIGHLIGHT_DURATION_MS = 3_000L
        private const val ARG_THREAD_ID = "thread_id"
        private const val ARG_PAGE_NUM = "page_num"
        private const val ARG_AUTHOR_ID = "author_id"
        private const val ARG_READ_PROGRESS = "read_progress"
        private const val ARG_PAGER_SCROLL_STATE = "pager_scroll_state"

        /**
         * Used for post post redirect.
         */
        private const val ARG_QUOTE_POST_ID = "quote_post_id"

        fun newInstance(threadId: String, pageNum: Int): PostListPagerFragment {
            return newInstance(threadId, pageNum, null, null, null, null)
        }

        fun newInstance(
            threadId: String,
            pageNum: Int,
            progress: ReadProgress,
            scrollState: PagerScrollState
        ): PostListPagerFragment {
            return newInstance(threadId, pageNum, null, null, progress, scrollState)
        }

        fun newInstance(threadId: String, pageNum: Int, postId: String): PostListPagerFragment {
            return newInstance(threadId, pageNum, null, postId, null, null)
        }

        fun newInstance(
            threadId: String, pageNum: Int, authorId: String?,
            postId: String?, progress: ReadProgress?, scrollState: PagerScrollState?
        ): PostListPagerFragment {
            val fragment = PostListPagerFragment()
            val bundle = Bundle()
            bundle.putString(ARG_THREAD_ID, threadId)
            if (!TextUtils.isEmpty(postId)) {
                bundle.putString(ARG_QUOTE_POST_ID, postId)
            }
            bundle.putString(ARG_AUTHOR_ID, authorId)
            bundle.putInt(ARG_PAGE_NUM, pageNum)
            bundle.putParcelable(ARG_READ_PROGRESS, progress)
            bundle.putParcelable(ARG_PAGER_SCROLL_STATE, scrollState)
            fragment.arguments = bundle

            return fragment
        }

        private fun filterPostAfterBlacklistChanged(dataSet: List<Any>): List<Any> {
            LooperUtil.enforceOnWorkThread()
            return dataSet.mapNotNull {
                if (it is Post) {
                    Posts.filterPost(it, true)
                } else {
                    it
                }
            }
        }
    }
}
