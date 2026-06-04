package me.ykrank.s1next.view.page.post.postlist

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.annotation.MainThread
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.extension.throttleFirst
import com.github.ykrank.androidtools.ui.dialog.PageJumpDialogFragment
import com.github.ykrank.androidtools.ui.internal.CoordinatorLayoutAnchorDelegate
import com.github.ykrank.androidtools.util.*
import com.github.ykrank.androidtools.widget.EventBus
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.collection.Posts
import me.ykrank.s1next.data.api.model.link.ThreadLink
import me.ykrank.s1next.data.db.biz.HistoryBiz
import me.ykrank.s1next.data.db.biz.ReadProgressBiz
import me.ykrank.s1next.data.db.biz.ThreadBiz
import me.ykrank.s1next.data.db.dbmodel.DbThread
import me.ykrank.s1next.data.db.dbmodel.History
import me.ykrank.s1next.data.db.dbmodel.ReadProgress
import me.ykrank.s1next.data.pref.DownloadPreferencesManager
import me.ykrank.s1next.data.pref.GeneralPreferencesManager
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.util.IntentUtil
import me.ykrank.s1next.view.activity.BaseActivity
import me.ykrank.s1next.view.activity.NewRateActivity
import me.ykrank.s1next.view.activity.NewReportActivity
import me.ykrank.s1next.view.activity.ReplyActivity
import me.ykrank.s1next.view.dialog.*
import me.ykrank.s1next.view.event.*
import me.ykrank.s1next.view.fragment.BaseViewPagerFragment
import me.ykrank.s1next.view.internal.PagerScrollState
import me.ykrank.s1next.view.internal.RequestCode
import me.ykrank.s1next.view.page.edit.EditPostActivity
import me.ykrank.s1next.view.page.post.prefetch.ThreadPrefetchDialogFragment
import me.ykrank.s1next.widget.track.event.ViewThreadTrackEvent
import javax.inject.Inject


/**
 * A Fragment includes [android.support.v4.view.ViewPager]
 * to represent each page of post lists.
 */
@AndroidEntryPoint
class PostListFragment : BaseViewPagerFragment(), PostListPagerFragment.PagerCallback,
    View.OnClickListener {

    @Inject
    internal lateinit var mEventBus: EventBus

    @Inject
    internal lateinit var mGeneralPreferencesManager: GeneralPreferencesManager

    @Inject
    internal lateinit var mReadPrefManager: ReadPreferencesManager

    @Inject
    internal lateinit var mDownloadPrefManager: DownloadPreferencesManager

    @Inject
    internal lateinit var historyBiz: HistoryBiz

    @Inject
    internal lateinit var readProgressBiz: ReadProgressBiz

    @Inject
    internal lateinit var threadBiz: ThreadBiz

    private lateinit var mThreadId: String
    private var mThreadTitle: String? = null
    private var mForumId: Int? = null

    private var mThreadAttachment: Posts.ThreadAttachment? = null
    private var mMenuThreadAttachment: MenuItem? = null
    private var toolbarPageJumpView: TextView? = null
    private var toolbarScrollFlags: Int? = null
    private var postShareBackPressedCallback: OnBackPressedCallback? = null
    private var isPostShareSelectionMode = false
    private var postShareSelectedCount = 0

    private var readProgress: ReadProgress? = null
    private var tempReadProgress: ReadProgress? = null
    private val scrollState = PagerScrollState()
    private var pendingSearchResult: SearchResultJump? = null

    private val mLastThreadInfoFlow by lazy {
        MutableSharedFlow<Int>(
            1, onBufferOverflow = BufferOverflow.DROP_OLDEST
        )
    }

    private val mPostListPagerAdapter: PostListPagerAdapter by lazy {
        PostListPagerAdapter(
            childFragmentManager
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbarPageJump()

        val bundle = requireArguments()
        val type = bundle.getInt(ARG_TYPE)
        val thread: Thread = bundle.getParcelable(ARG_THREAD)!!
        val authorId = bundle.getString(ARG_AUTHOR_ID)
        // thread title is null if this thread comes from ThreadLink
        mThreadTitle = thread.title
        mThreadId = thread.id!!
        mForumId = thread.fid?.toIntOrNull()

        trackAgent.post(
            ViewThreadTrackEvent(
                mThreadTitle, mThreadId, hashMapOf(
                    Pair("Type", type.toString()),
                    Pair("Theme", mGeneralPreferencesManager.themeIndex.toString()),
                    Pair("Dark Theme", mGeneralPreferencesManager.darkThemeIndex.toString()),
                    Pair("FontScale", mGeneralPreferencesManager.fontScale.toString()),
                    Pair(
                        "SignatureEnabled",
                        mGeneralPreferencesManager.isSignatureEnabled.toString()
                    ),
                    Pair("HybridPostRender", mReadPrefManager.hybridPostRender.toString()),
                    Pair(
                        "QuickSideBarEnable",
                        mGeneralPreferencesManager.isQuickSideBarEnable.toString()
                    ),
                    Pair("SaveAuto", mReadPrefManager.isSaveAuto.toString()),
                    Pair("LoadAuto", mReadPrefManager.isLoadAuto.toString()),
                    Pair(
                        "TotalImageCacheSize",
                        mDownloadPrefManager.totalImageCacheSize.toString()
                    ),
                    Pair("TotalDataCacheSize", mDownloadPrefManager.totalDataCacheSize.toString()),
                    Pair("NetCacheEnable", mDownloadPrefManager.netCacheEnable.toString()),
                    Pair("AvatarsDownload", mDownloadPrefManager.isAvatarsDownload.toString()),
                    Pair("ImagesDownload", mDownloadPrefManager.isImagesDownload.toString())
                )
            )
        )
        leavePageMsg("PostListFragment##ThreadTitle:$mThreadTitle,ThreadId:$mThreadId,Type:$type")

        //when seeing one's post, the authorId isn't null. Skip initialization in this case.
        if (savedInstanceState == null && authorId == null) {
            val jumpPage: Int
            //读取进度
            readProgress = bundle.getParcelable(ARG_READ_PROGRESS)
            if (readProgress != null) {
                scrollState.state = PagerScrollState.BEFORE_SCROLL_POSITION
                jumpPage = readProgress?.page ?: 0
            } else {
                jumpPage = bundle.getInt(ARG_JUMP_PAGE, 0)
            }

            // +1 for original post
            val threadPage = thread.pageCount
            setTotalPages(Math.max(jumpPage, threadPage))

            if (jumpPage != 0) {
                currentPage = jumpPage - 1
            } else {
                if (bundle.getBoolean(ARG_SHOULD_GO_TO_LAST_PAGE, false)) {
                    currentPage = getTotalPages() - 1
                }
            }
            saveHistory()
        }
        setTitleWithPosition(currentPage)

        (activity as CoordinatorLayoutAnchorDelegate).setupFloatingActionButton(
            R.drawable.ic_insert_comment_black_24dp, this
        )
        setupPostActionObservers()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        postShareBackPressedCallback = requireActivity().onBackPressedDispatcher.addCallback(
            this,
            enabled = false
        ) {
            handlePostShareSelectionBackPressed()
        }
        lifecycleScope.launch(L.report) {
            mLastThreadInfoFlow
                .throttleFirst(1000L)
                .collectLatest {
                    withContext(Dispatchers.IO) {
                        val dbThread = DbThread(Integer.valueOf(mThreadId), it)
                        threadBiz.saveThread(dbThread)
                    }
                }
        }
    }

    override fun onPause() {
        //save last read progress
        val fragment = curPostPageFragment
        if (fragment != null) {
            tempReadProgress = fragment.curReadProgress
            val readProgressToSave = tempReadProgress
            if (readProgressToSave != null) {
                lifecycleScope.launch(L.report) {
                    delay(5_000)
                    mReadPrefManager.saveLastReadProgress(readProgressToSave)
                    L.i("Save last read progress:$readProgressToSave")
                }
            }
        } else {
            tempReadProgress = null
        }
        super.onPause()
    }

    override fun onDestroy() {
        mReadPrefManager.saveLastReadProgress(null)

        //Auto save read progress
        if (mReadPrefManager.isSaveAuto) {
            tempReadProgress?.let { readProgressBiz.saveReadProgressAsync(it) }
        }
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        menu.findItem(R.id.menu_page_jump)?.isVisible = false
        inflater.inflate(R.menu.fragment_post, menu)

        mMenuThreadAttachment = menu.findItem(R.id.menu_thread_attachment)
        if (mThreadAttachment == null) {
            mMenuThreadAttachment?.isVisible = false
        }

        if (mReadPrefManager.isSaveAuto) {
            val saveMenu = menu.findItem(R.id.menu_save_progress)
            saveMenu?.isVisible = false
        }
    }

    private fun setupPostActionObservers() {
        viewLifecycleOwner.lifecycleScope.launch(L.report) {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                launch {
                    mEventBus.getClsFlow<QuoteEvent>()
                        .collect { quoteEvent ->
                            startReplyActivity(
                                quoteEvent.quotePostId,
                                quoteEvent.quotePostCount
                            )
                        }
                }
                launch {
                    mEventBus.getClsFlow<RateEvent>()
                        .collect { event -> startRateActivity(event.threadId, event.postId) }
                }
                launch {
                    mEventBus.getClsFlow<ReportEvent>()
                        .collect { event ->
                            startReportActivity(event.threadId, event.postId, event.pageNum)
                        }
                }
                launch {
                    mEventBus.getClsFlow<EditPostEvent>()
                        .collect {
                            EditPostActivity.startActivityForResult(
                                this@PostListFragment,
                                RequestCode.REQUEST_CODE_EDIT_POST,
                                it.thread,
                                it.post
                            )
                        }
                }
                launch {
                    mEventBus.getClsFlow<VotePostEvent>()
                        .collect {
                            if (!LoginPromptDialogFragment.showAppLoginPromptDialogIfNeeded(
                                    childFragmentManager,
                                    mUser
                                )
                            ) {
                                VoteDialogFragment.newInstance(it.threadId, it.vote)
                                    .show(childFragmentManager, VoteDialogFragment.TAG)
                            }
                        }
                }
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val mMenuQuickSideBarEnable = menu.findItem(R.id.menu_quick_side_bar_enable)
        mMenuQuickSideBarEnable?.isChecked = mGeneralPreferencesManager.isQuickSideBarEnable
        applyPostShareSelectionMenuState(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_thread_attachment -> {
                ThreadAttachmentDialogFragment.newInstance(mThreadAttachment).show(
                    requireActivity().supportFragmentManager,
                    ThreadAttachmentDialogFragment.TAG
                )

                return true
            }

            R.id.menu_favourites_add -> {
                if (!LoginPromptDialogFragment.showLoginPromptDialogIfNeeded(
                        childFragmentManager,
                        mUser
                    )
                ) {
                    ThreadFavouritesAddDialogFragment.newInstance(mThreadId, mThreadTitle).show(
                        requireActivity().supportFragmentManager,
                        ThreadFavouritesAddDialogFragment.TAG
                    )
                }

                return true
            }

            R.id.menu_link -> {
                ClipboardUtil.copyText(
                    requireContext(), "Url of $mThreadTitle", Api.getPostListUrlForBrowser(
                        mThreadId,
                        currentPage
                    )
                )
                (activity as CoordinatorLayoutAnchorDelegate).showSnackbar(
                    R.string.message_link_copied
                )

                return true
            }

            R.id.menu_share -> {
                curPostPageFragment?.startPostShareSelection(null)
                    ?: showSnackbar(R.string.post_share_no_data)
                return true
            }

            R.id.menu_post_share_confirm -> {
                curPostPageFragment?.confirmPostShareSelection()
                return true
            }

            R.id.menu_browser -> {
                IntentUtil.startViewIntentExcludeOurApp(
                    requireContext(), Uri.parse(
                        Api.getPostListUrlForBrowser(mThreadId, currentPage + 1)
                    )
                )

                return true
            }

            R.id.menu_search_current_page -> {
                startPageSearch()
                return true
            }

            R.id.menu_save_progress -> {
                if (curPostPageFragment != null) {
                    curPostPageFragment?.saveReadProgress()
                }
                return true
            }

            R.id.menu_load_progress -> {
                loadReadProgress()
                return true
            }

            R.id.menu_quick_side_bar_enable -> {
                item.isChecked = !item.isChecked
                mGeneralPreferencesManager.isQuickSideBarEnable = item.isChecked
                mEventBus.postDefault(QuickSidebarEnableChangeEvent())
                return true
            }

            R.id.menu_prefetch_after_posts -> {
                ThreadPrefetchDialogFragment.newInstance(mThreadId, curPostPageFragment?.pageNum)
                    .show(childFragmentManager, LoadBlackListFromWebDialogFragment.TAG)
                return true
            }

            R.id.menu_backup_thread -> {
                ThreadPrefetchDialogFragment.newBackupInstance(mThreadId)
                    .show(childFragmentManager, LoadBlackListFromWebDialogFragment.TAG)
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun applyPostShareSelectionMenuState(menu: Menu) {
        val ordinaryPostMenuIds = intArrayOf(
            R.id.menu_thread_attachment,
            R.id.menu_search_current_page,
            R.id.menu_favourites_add,
            R.id.menu_link,
            R.id.menu_share,
            R.id.menu_browser,
            R.id.menu_save_progress,
            R.id.menu_load_progress,
            R.id.menu_quick_side_bar_enable,
            R.id.menu_prefetch_after_posts,
            R.id.menu_backup_thread,
        )
        ordinaryPostMenuIds.forEach { menu.findItem(it)?.isVisible = !isPostShareSelectionMode }
        menu.findItem(R.id.menu_post_share_confirm)?.let {
            it.isVisible = isPostShareSelectionMode
            tintPostShareConfirmMenuItem(it)
        }
        if (!isPostShareSelectionMode) {
            menu.findItem(R.id.menu_thread_attachment)?.isVisible = mThreadAttachment != null
            if (mReadPrefManager.isSaveAuto) {
                menu.findItem(R.id.menu_save_progress)?.isVisible = false
            }
        }
    }

    private fun tintPostShareConfirmMenuItem(item: MenuItem) {
        val icon = item.icon ?: return
        val wrapped = DrawableCompat.wrap(icon.mutate())
        DrawableCompat.setTint(
            wrapped,
            ContextCompat.getColor(requireContext(), R.color.post_share_selection_warning)
        )
        item.icon = wrapped
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.REQUEST_CODE_EDIT_POST -> {
                if (resultCode == Activity.RESULT_OK) {
                    val msg = data?.getStringExtra(BaseActivity.EXTRA_MESSAGE)
                    showSnackbar(msg)
                    val fragment = curPostPageFragment
                    fragment?.startSwipeRefresh()
                }
            }

            RequestCode.REQUEST_CODE_POST_PAGE_SEARCH -> {
                if (resultCode == Activity.RESULT_OK) {
                    val page = data?.getIntExtra(
                        PostPageSearchActivity.EXTRA_PAGE,
                        currentPage + 1
                    ) ?: currentPage + 1
                    val position = data?.getIntExtra(
                        PostPageSearchActivity.EXTRA_POSITION,
                        RecyclerView.NO_POSITION
                    ) ?: RecyclerView.NO_POSITION
                    jumpToSearchResult(page, position)
                }
            }

            else -> {
                super.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun getPagerAdapter(fragmentManager: FragmentManager): FragmentStatePagerAdapter<*> {
        return mPostListPagerAdapter
    }

    override fun getTitleWithoutPosition(): CharSequence? {
        return mThreadTitle
    }

    override fun setTitleWithPosition(position: Int) {
        if (isPostShareSelectionMode) {
            activity?.title = getString(R.string.post_share_selected_count, postShareSelectedCount)
            toolbarPageJumpView?.visibility = View.GONE
            return
        }
        activity?.title = mThreadTitle
        renderToolbarPageJump(position)
    }

    override fun setThreadInfo(thread: Thread?) {
        if (thread != null) {
            setTotalPageByPosts(thread.reliesCount + 1)
            setThreadTitle(thread.title)
        }
    }

    override fun consumePendingSearchResultPosition(pageNum: Int): Int? {
        val jump = pendingSearchResult ?: return null
        if (jump.pageNum != pageNum) {
            return null
        }
        pendingSearchResult = null
        return jump.position
    }

    override fun onPostShareSelectionChanged(enabled: Boolean, selectedCount: Int) {
        isPostShareSelectionMode = enabled
        postShareSelectedCount = selectedCount
        postShareBackPressedCallback?.isEnabled = enabled
        if (enabled) {
            toolbarPageJumpView?.visibility = View.GONE
            activity?.title = getString(R.string.post_share_selected_count, selectedCount)
        } else {
            setTitleWithPosition(currentPage)
        }
        setTitleBarScrollEnabled(!enabled)
        setReplyFabVisible(!enabled)
        activity?.invalidateOptionsMenu()
    }

    override fun onDestroyView() {
        setTitleBarScrollEnabled(true)
        setReplyFabVisible(true)
        super.onDestroyView()
    }

    private fun setTitleBarScrollEnabled(enabled: Boolean) {
        val toolbar = activity?.findViewById<View>(R.id.toolbar) ?: return
        val layoutParams = toolbar.layoutParams as? AppBarLayout.LayoutParams ?: return
        if (toolbarScrollFlags == null) {
            toolbarScrollFlags = layoutParams.scrollFlags
        }
        val targetFlags = if (enabled) {
            toolbarScrollFlags ?: layoutParams.scrollFlags
        } else {
            0
        }
        if (layoutParams.scrollFlags != targetFlags) {
            layoutParams.scrollFlags = targetFlags
            toolbar.layoutParams = layoutParams
        }
        if (!enabled) {
            (toolbar.parent as? AppBarLayout)?.setExpanded(true, false)
        }
    }

    private fun setReplyFabVisible(visible: Boolean) {
        val fab = activity?.findViewById<FloatingActionButton>(R.id.floating_action_button) ?: return
        if (visible) {
            fab.show()
        } else {
            fab.hide()
        }
    }

    private fun jumpToSearchResult(pageNum: Int, position: Int) {
        if (pageNum <= 0 || position == RecyclerView.NO_POSITION || position < 0) {
            return
        }
        val targetPageIndex = pageNum - 1
        setTotalPages(maxOf(getTotalPages(), pageNum))
        pendingSearchResult = SearchResultJump(pageNum, position)
        val fragment = mPostListPagerAdapter.getCachedFragment(targetPageIndex)
        if (currentPage != targetPageIndex) {
            currentPage = targetPageIndex
        }
        if (fragment != null) {
            if (fragment.scrollToPostPosition(position, true)) {
                pendingSearchResult = null
            }
        }
    }

    private fun startPageSearch() {
        val currentSnapshot = curPostPageFragment?.buildPageSearchSnapshot()
        if (currentSnapshot == null || currentSnapshot.posts.isEmpty()) {
            showSnackbar(R.string.post_page_search_no_data)
            return
        }

        val memoryPages = ArrayList<PostPageSearchActivity.PageSnapshot>()
        for (pageIndex in 0 until getTotalPages()) {
            mPostListPagerAdapter.getCachedFragment(pageIndex)
                ?.buildPageSearchSnapshot()
                ?.let { memoryPages += it }
        }

        PostPageSearchActivity.startForResult(
            this,
            RequestCode.REQUEST_CODE_POST_PAGE_SEARCH,
            currentSnapshot.thread,
            mThreadId,
            currentSnapshot.pageNum,
            getTotalPages(),
            currentSnapshot.posts,
            memoryPages
        )
    }

    private fun setTotalPageByPosts(threads: Int) {
        setTotalPages(MathUtil.divide(threads, Api.POSTS_PER_PAGE))
        renderToolbarPageJump()
        //save reply count in database
        mLastThreadInfoFlow.tryEmit(threads - 1)
    }

    private fun setThreadTitle(title: CharSequence?) {
        if (!title.isNullOrEmpty() && mThreadTitle != title.toString()) {
            mThreadTitle = title.toString()
            setTitleWithPosition(currentPage)
        }
        saveHistory()
    }

    private fun setupToolbarPageJump() {
        toolbarPageJumpView = activity?.findViewById<TextView>(R.id.toolbar_page_jump)?.apply {
            setOnClickListener {
                showPageJumpDialog()
            }
        }
        renderToolbarPageJump()
    }

    private fun renderToolbarPageJump(position: Int = currentPage) {
        toolbarPageJumpView?.apply {
            if (isPostShareSelectionMode) {
                visibility = View.GONE
                return
            }
            visibility = View.VISIBLE
            text = "${position + 1}/${maxOf(getTotalPages(), 1)}"
        }
    }

    private fun showPageJumpDialog() {
        if (getTotalPages() <= 1) {
            return
        }
        PageJumpDialogFragment.newInstance(getTotalPages(), currentPage)
            .show(childFragmentManager, PageJumpDialogFragment.TAG)
    }

    override fun setupThreadAttachment(threadAttachment: Posts.ThreadAttachment) {
        this.mThreadAttachment = threadAttachment

        // mMenuThreadAttachment = null when configuration changes (like orientation changes)
        // but we don't need to care about the visibility of mMenuThreadAttachment
        // because mThreadAttachment != null and we won't invoke
        // mMenuThreadAttachment.setVisible(false) during onCreateOptionsMenu(Menu)
        mMenuThreadAttachment?.isVisible = true
    }

    override fun onClick(v: View) {
        startReplyActivity(null, null)
    }

    /**
     * 获取当前的具体帖子fragment
     */
    internal val curPostPageFragment: PostListPagerFragment?
        get() = mPostListPagerAdapter.currentFragment

    fun dispatchPostShareSelectionBackPressed(): Boolean {
        if (!isPostShareSelectionMode) {
            return false
        }
        requireActivity().onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun handlePostShareSelectionBackPressed() {
        activePostShareSelectionFragment()?.cancelPostShareSelection()
            ?: onPostShareSelectionChanged(false, 0)
    }

    private fun activePostShareSelectionFragment(): PostListPagerFragment? {
        curPostPageFragment
            ?.takeIf { it.postShareSelectionState.enabled }
            ?.let { return it }
        for (page in 0 until getTotalPages()) {
            val fragment = mPostListPagerAdapter.getCachedFragment(page)
            if (fragment?.postShareSelectionState?.enabled == true) {
                return fragment
            }
        }
        return null
    }

    /**
     * 读取阅读进度
     */
    internal fun loadReadProgress() {
        lifecycleScope.launch(L.report) {
            val progress = withContext(Dispatchers.IO) {
                readProgressBiz.getWithThreadId(mThreadId.toInt())
            }
            if (progress != null) {
                scrollState.state = PagerScrollState.BEFORE_SCROLL_PAGE
                afterLoadReadProgress(progress)
            }
        }
    }

    /**
     * 读取阅读进度后的操作，主线程
     */
    @MainThread
    private fun afterLoadReadProgress(progress: ReadProgress?) {
        if (progress != null && scrollState.state == PagerScrollState.BEFORE_SCROLL_PAGE) {
            val targetPosition = progress.page - 1
            if (targetPosition < 0) {
                //readProgress page error
                return
            }
            val fragment = mPostListPagerAdapter.getCachedFragment(targetPosition)
            if (fragment != null) {
                if (currentPage != targetPosition) {
                    fragment.loadReadProgressInRecycleView(progress, false)
                    currentPage = progress.page - 1
                } else {
                    fragment.loadReadProgressInRecycleView(progress, true)
                }
                scrollState.state = PagerScrollState.FREE
            } else {
                scrollState.state = PagerScrollState.BEFORE_SCROLL_POSITION
                currentPage = progress.page - 1
            }
        }
    }

    private fun startReplyActivity(quotePostId: String?, quotePostCount: String?) {
        val fm = fragmentManager ?: return
        val activity = activity ?: return
        if (LoginPromptDialogFragment.showLoginPromptDialogIfNeeded(fm, mUser)) {
            return
        }

        ReplyActivity.startReplyActivityForResultMessage(
            activity, mThreadId, mThreadTitle,
            quotePostId, quotePostCount, mForumId, currentPage + 1
        )
    }

    private fun startRateActivity(threadId: String, postId: String) {
        val fm = fragmentManager ?: return
        val activity = activity ?: return
        if (LoginPromptDialogFragment.showLoginPromptDialogIfNeeded(fm, mUser)) {
            return
        }

        NewRateActivity.start(activity, threadId, postId)
    }

    private fun startReportActivity(threadId: String, postId: String, pageNum: Int) {
        val fm = fragmentManager ?: return
        val activity = activity ?: return
        if (LoginPromptDialogFragment.showLoginPromptDialogIfNeeded(fm, mUser)) {
            return
        }

        NewReportActivity.start(activity, threadId, postId, pageNum)
    }

    private fun saveHistory() {
        val threadId = mThreadId.toInt()
        if (threadId > 0 && !TextUtils.isEmpty(mThreadTitle)) {
            lifecycleScope.launch(Dispatchers.IO) {
                historyBiz.addNewHistory(History(threadId, mThreadTitle))
            }
        }
    }

    /**
     * Returns a Fragment corresponding to one of the pages of posts.
     */
    private inner class PostListPagerAdapter constructor(fm: FragmentManager) :
        FragmentStatePagerAdapter<PostListPagerFragment>(fm) {

        override fun getItem(i: Int): PostListPagerFragment {
            val progress = readProgress
            val bundle = arguments!!
            val jumpPage = bundle.getInt(ARG_JUMP_PAGE, -1)
            val quotePostId: String? = bundle.getString(ARG_QUOTE_POST_ID)
            val authorId = bundle.getString(ARG_AUTHOR_ID)
            if (jumpPage == i + 1 && !quotePostId.isNullOrEmpty()) {
                // clear this arg string because we only need to tell PostListPagerFragment once
                arguments?.putString(ARG_QUOTE_POST_ID, null)
                return PostListPagerFragment.newInstance(mThreadId, jumpPage, quotePostId)
            } else if (progress != null && progress.page == i + 1
                && scrollState.state == PagerScrollState.BEFORE_SCROLL_POSITION
            ) {
                return PostListPagerFragment.newInstance(mThreadId, i + 1, progress, scrollState)
            } else {
                return PostListPagerFragment.newInstance(
                    mThreadId,
                    i + 1,
                    authorId,
                    null,
                    null,
                    null
                )
            }
        }
    }

    private data class SearchResultJump(
        val pageNum: Int,
        val position: Int
    )

    companion object {
        val TAG = PostListFragment::class.java.simpleName

        const val Type_Thread = 0
        const val Type_Thread_Link = 1
        const val Type_Thread_Read_Progress = 2
        const val Type_Thread_One_Author = 3

        private const val ARG_TYPE = "type"
        private const val ARG_THREAD = "thread"
        private const val ARG_SHOULD_GO_TO_LAST_PAGE = "should_go_to_last_page"

        /**
         * Only see this author post
         */
        private const val ARG_AUTHOR_ID = "author_id"

        /**
         * ARG_JUMP_PAGE takes precedence over [.ARG_SHOULD_GO_TO_LAST_PAGE].
         */
        private const val ARG_JUMP_PAGE = "jump_page"
        private const val ARG_QUOTE_POST_ID = "quote_post_id"

        private const val ARG_READ_PROGRESS = "read_progress"

        fun newInstance(thread: Thread, shouldGoToLastPage: Boolean): PostListFragment {
            val fragment = PostListFragment()
            val bundle = Bundle()
            bundle.putInt(ARG_TYPE, Type_Thread)
            bundle.putParcelable(ARG_THREAD, thread)
            bundle.putBoolean(ARG_SHOULD_GO_TO_LAST_PAGE, shouldGoToLastPage)
            fragment.arguments = bundle

            return fragment
        }

        fun newInstance(threadLink: ThreadLink): PostListFragment {
            val thread = Thread()
            thread.id = threadLink.threadId

            val fragment = PostListFragment()
            val bundle = Bundle()
            bundle.putInt(ARG_TYPE, Type_Thread_Link)
            bundle.putParcelable(ARG_THREAD, thread)
            bundle.putInt(ARG_JUMP_PAGE, threadLink.jumpPage)
            val quotePostId = threadLink.quotePostId
            if (quotePostId != null) {
                bundle.putString(ARG_QUOTE_POST_ID, quotePostId)
            }
            fragment.arguments = bundle

            return fragment
        }

        fun newInstance(thread: Thread, progress: ReadProgress): PostListFragment {
            val fragment = PostListFragment()
            val bundle = Bundle()
            bundle.putInt(ARG_TYPE, Type_Thread_Read_Progress)
            bundle.putParcelable(ARG_THREAD, thread)
            bundle.putParcelable(ARG_READ_PROGRESS, progress)
            fragment.arguments = bundle

            return fragment
        }

        fun newInstance(thread: Thread, authorId: String?): PostListFragment {
            val fragment = PostListFragment()
            val bundle = Bundle()
            bundle.putInt(ARG_TYPE, Type_Thread_One_Author)
            bundle.putParcelable(ARG_THREAD, thread)
            bundle.putString(ARG_AUTHOR_ID, authorId)
            fragment.arguments = bundle

            return fragment
        }
    }
}
