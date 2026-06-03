package me.ykrank.s1next.view.page.post.prefetch

import android.os.Bundle
import android.view.View
import androidx.annotation.MainThread
import androidx.lifecycle.lifecycleScope
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ykrank.androidtools.extension.toast
import com.github.ykrank.androidtools.util.L
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.ApiCacheProvider
import me.ykrank.s1next.data.api.model.wrapper.PostsWrapper
import me.ykrank.s1next.data.cache.api.ApiCacheConstants
import me.ykrank.s1next.data.cache.api.ApiCacheFlow
import me.ykrank.s1next.data.cache.biz.CacheBiz
import me.ykrank.s1next.util.ErrorUtil
import me.ykrank.s1next.view.activity.HistoryActivity
import me.ykrank.s1next.view.dialog.BaseLoadProgressDialogFragment
import kotlin.math.max
import javax.inject.Inject


/**
 * A dialog lets user load website blacklist.
 */
@AndroidEntryPoint
class ThreadPrefetchDialogFragment : BaseLoadProgressDialogFragment() {
    @Inject
    internal lateinit var mUser: User

    @Inject
    internal lateinit var apiCache: ApiCacheProvider

    @Inject
    internal lateinit var cacheBiz: CacheBiz

    @Inject
    internal lateinit var jsonMapper: ObjectMapper

    private lateinit var threadId: String
    private var pageStart: Int = 1
    private var backupMode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        threadId = requireArguments().getString(ARG_THREAD_ID)!!
        pageStart = requireArguments().getInt(ARG_PAGE_START, 1)
        backupMode = requireArguments().getBoolean(ARG_BACKUP_MODE, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadNextPage(pageStart)
    }

    private fun updateProgress(page: Int, max: Int) {
        if (max > 0) {
            binding.max = max
        }
        binding.progress = page
    }

    @MainThread
    private fun loadNextPage(page: Int) {
        lifecycleScope.launch {
            val cache = withContext(Dispatchers.IO + L.report) {
                val key = ApiCacheFlow.getKey(
                    mUser.uid,
                    ApiCacheConstants.CacheType.Posts,
                    listOf(threadId, page)
                )
                cacheBiz.getTextZipByKey(key)?.decodeZipString?.let { json ->
                    runCatching {
                        jsonMapper.readValue(json, PostsWrapper::class.java)
                    }.onFailure {
                        L.report(it)
                    }.getOrNull()
                }
            }
            val thread = cache?.data?.postListInfo
            if (thread != null && page < thread.pageCount) {
                if (backupMode) {
                    savePostBackup(page, cache)
                }
                // 已预加载，而且非最后一页的数据，不用重新拉取
                loadNextPage(page + 1)
                updateProgress(page, max(binding.max, thread.pageCount))
                return@launch
            }
            apiCache.getPostsWrapper(
                threadId,
                page,
                ignoreCache = true
            ).onCompletion {
                val max = binding.max
                if (max > 0) {
                    if (max > page) {
                        loadNextPage(page + 1)
                    } else {
                        finishLoad()
                    }
                }
            }.collect {
                if (it.data != null) {
                    val max = it.data?.data?.postListInfo?.pageCount ?: 0
                    updateProgress(page, max)
                    if (backupMode) {
                        savePostBackup(page, it.data!!)
                    }
                } else {
                    requireActivity().apply {
                        toast(ErrorUtil.parse(this, it.error))
                    }
                }
            }
        }
    }

    private suspend fun finishLoad() {
        if (backupMode) {
            requireActivity().toast(me.ykrank.s1next.R.string.post_backup_success)
            delay(300)
            this@ThreadPrefetchDialogFragment.dismiss()
            HistoryActivity.startPostBackups(requireContext())
        } else {
            delay(2000)
            this@ThreadPrefetchDialogFragment.dismiss()
        }
    }

    private suspend fun savePostBackup(page: Int, data: PostsWrapper) {
        withContext(Dispatchers.IO + L.report) {
            val key = ApiCacheFlow.getKey(
                mUser.uid,
                ApiCacheConstants.CacheType.Posts,
                listOf(threadId, page)
            )
            cacheBiz.savePostBackup(
                key,
                mUser.uid?.toIntOrNull(),
                data,
                data.data?.postListInfo?.title,
                threadId,
                page
            )
        }
    }

    companion object {
        val TAG: String = ThreadPrefetchDialogFragment::class.java.simpleName
        const val ARG_THREAD_ID = "thread_id"
        const val ARG_PAGE_START = "page_start"
        const val ARG_BACKUP_MODE = "backup_mode"

        fun newInstance(threadId: String, page: Int?): ThreadPrefetchDialogFragment {
            val fragment = ThreadPrefetchDialogFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_THREAD_ID, threadId)
                putInt(ARG_PAGE_START, page ?: 1)
            }
            return fragment
        }

        fun newBackupInstance(threadId: String): ThreadPrefetchDialogFragment {
            val fragment = ThreadPrefetchDialogFragment()
            fragment.arguments = Bundle().apply {
                putString(ARG_THREAD_ID, threadId)
                putInt(ARG_PAGE_START, 1)
                putBoolean(ARG_BACKUP_MODE, true)
            }
            return fragment
        }
    }
}
