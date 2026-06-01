package me.ykrank.s1next.view.page.post.adapter.render

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewAdapter
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewHolder
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.widget.EventBus
import kotlinx.coroutines.launch
import me.ykrank.s1next.App
import me.ykrank.s1next.R
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.ApiCacheProvider
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.Vote
import me.ykrank.s1next.data.pref.GeneralPreferencesManager
import me.ykrank.s1next.databinding.ItemPostRenderFooterBinding
import me.ykrank.s1next.databinding.ItemRateDetailBinding
import me.ykrank.s1next.view.activity.RateDetailsListActivity
import me.ykrank.s1next.view.activity.UserHomeActivity
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate
import me.ykrank.s1next.view.page.post.render.PostRenderItem
import me.ykrank.s1next.view.page.post.share.PostShareSelectionOwner
import me.ykrank.s1next.view.page.post.viewmodel.PostViewModel

class PostRenderFooterAdapterDelegate(
    private val fragment: Fragment,
    context: Context,
    private val postShareSelectionOwner: PostShareSelectionOwner? = null
) :
    BaseAdapterDelegate<PostRenderItem.Footer, SimpleRecycleViewHolder<ItemPostRenderFooterBinding>>(
        context,
        PostRenderItem.Footer::class.java
    ) {
    private val eventBus: EventBus = App.preAppComponent.eventBus
    private val user: User = App.appComponent.user
    private val apiCache: ApiCacheProvider = App.appComponent.apiCacheProvider
    private val generalPreferencesManager: GeneralPreferencesManager =
        App.preAppComponent.generalPreferencesManager

    private var threadInfo: Thread? = null
    private var voteInfo: Vote? = null
    private var pageNum: Int = 1

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = DataBindingUtil.inflate<ItemPostRenderFooterBinding>(
            mLayoutInflater,
            R.layout.item_post_render_footer,
            parent,
            false
        )
        binding.postViewModel = PostViewModel(fragment.viewLifecycleOwner, eventBus, user)
        return SimpleRecycleViewHolder(binding)
    }

    override fun onBindViewHolderData(
        t: PostRenderItem.Footer,
        position: Int,
        holder: SimpleRecycleViewHolder<ItemPostRenderFooterBinding>,
        payloads: List<Any>
    ) {
        val binding = holder.binding
        val post = t.post
        val shareSelectionEnabled = postShareSelectionOwner?.postShareSelectionState?.enabled == true
        binding.quickSidebarEnable = generalPreferencesManager.isQuickSideBarEnable
        binding.postViewModel?.let {
            it.thread.set(threadInfo)
            it.pageNum.set(pageNum)
            it.post.set(post)
            if ("1" == post.number) {
                it.vote.set(voteInfo)
            } else {
                it.vote.set(null)
            }
        }

        val rates = post.rates
        if (rates != null) {
            binding.layoutRates.visibility = View.VISIBLE
            binding.tvRateViewAll.setOnClickListener {
                if (rates.isNotEmpty()) {
                    RateDetailsListActivity.start(binding.root.context, ArrayList(rates))
                } else {
                    fragment.lifecycleScope.launch(L.report) {
                        val newRates = apiCache.getPostRates(threadInfo?.id ?: "", post.id).data
                            ?: emptyList()
                        post.rates = newRates
                        updateRateList(binding, newRates)
                    }
                }
            }
        } else {
            binding.layoutRates.visibility = View.GONE
        }

        if (rates?.isNotEmpty() == true) {
            binding.recycleViewRates.visibility = View.VISIBLE
            ensureRatesAdapter(binding)
            updateRateList(binding, rates)
        } else {
            binding.recycleViewRates.visibility = View.GONE
        }

        binding.executePendingBindings()
        if (shareSelectionEnabled) {
            val toggleSelection = View.OnClickListener {
                postShareSelectionOwner?.togglePostShareSelection(post.id)
            }
            binding.root.setOnClickListener(toggleSelection)
            binding.tvShowTrade.setOnClickListener(toggleSelection)
            binding.tvShowVote.setOnClickListener(toggleSelection)
            binding.tvCastMagic.setOnClickListener(toggleSelection)
            binding.tvRateViewAll.setOnClickListener(toggleSelection)
            binding.recycleViewRates.setOnClickListener(toggleSelection)
        } else {
            binding.root.setOnClickListener(null)
            binding.recycleViewRates.setOnClickListener(null)
        }
    }

    private fun ensureRatesAdapter(binding: ItemPostRenderFooterBinding) {
        if (binding.recycleViewRates.adapter != null) {
            return
        }
        binding.recycleViewRates.adapter = SimpleRecycleViewAdapter(
            binding.root.context,
            R.layout.item_rate_detail,
            true,
            { _, rateBinding ->
                val bind = rateBinding as? ItemRateDetailBinding
                bind?.model?.apply {
                    val uid = this.uid
                    val uname = this.uname
                    bind.avatar.setOnClickListener {
                        if (uid != null && uname != null) {
                            UserHomeActivity.start(
                                it.context as FragmentActivity,
                                uid,
                                uname,
                                it
                            )
                        }
                    }
                }
            }
        )
        binding.recycleViewRates.layoutManager = LinearLayoutManager(binding.root.context)
        binding.recycleViewRates.isNestedScrollingEnabled = false
    }

    private fun updateRateList(binding: ItemPostRenderFooterBinding, rates: List<me.ykrank.s1next.data.api.model.Rate>) {
        val adapter = binding.recycleViewRates.adapter as? SimpleRecycleViewAdapter ?: return
        if (rates.size > 10) {
            adapter.diffNewDataSet(rates.subList(0, 10), true)
        } else {
            adapter.diffNewDataSet(rates, true)
        }
    }

    fun setThreadInfo(threadInfo: Thread, pageNum: Int) {
        this.threadInfo = threadInfo
        this.pageNum = pageNum
    }

    fun setVoteInfo(voteInfo: Vote?) {
        this.voteInfo = voteInfo
    }
}
