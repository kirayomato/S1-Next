package me.ykrank.s1next.view.page.post.adapter.render

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewHolder
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.widget.EventBus
import kotlinx.coroutines.launch
import me.ykrank.s1next.R
import me.ykrank.s1next.binding.ViewBindingAdapter
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.ApiCacheProvider
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.Vote
import me.ykrank.s1next.data.pref.GeneralPreferencesManager
import me.ykrank.s1next.databinding.ItemPostRenderFooterBinding
import me.ykrank.s1next.view.activity.RateDetailsListActivity
import me.ykrank.s1next.view.adapter.RateDetailAdapter
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate
import me.ykrank.s1next.view.page.post.render.PostRenderItem
import me.ykrank.s1next.view.page.post.share.PostShareSelectionOwner
import me.ykrank.s1next.view.page.post.share.PostShareSelectionPayload
import me.ykrank.s1next.view.page.post.viewmodel.PostViewModel

class PostRenderFooterAdapterDelegate(
    private val fragment: Fragment,
    context: Context,
    private val postShareSelectionOwner: PostShareSelectionOwner? = null,
    private val eventBus: EventBus,
    private val user: User,
    private val apiCache: ApiCacheProvider,
    private val generalPreferencesManager: GeneralPreferencesManager
) :
    BaseAdapterDelegate<PostRenderItem.Footer, PostRenderFooterViewHolder>(
        context,
        PostRenderItem.Footer::class.java
    ) {
    private var threadInfo: Thread? = null
    private var voteInfo: Vote? = null
    private var pageNum: Int = 1

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemPostRenderFooterBinding.inflate(mLayoutInflater, parent, false)
        val viewModel = PostViewModel(fragment.viewLifecycleOwner, eventBus, user)
        binding.tvShowTrade.setOnClickListener(viewModel::onExtraHtmlClick)
        binding.tvShowVote.setOnClickListener(viewModel::onVoteClick)
        binding.tvCastMagic.setOnClickListener(viewModel::onAppPostClick)
        return PostRenderFooterViewHolder(binding, viewModel)
    }

    override fun onBindViewHolderData(
        t: PostRenderItem.Footer,
        position: Int,
        holder: PostRenderFooterViewHolder,
        payloads: List<Any>
    ) {
        val binding = holder.binding
        val post = t.post
        if (payloads.contains(PostShareSelectionPayload)) {
            bindShareSelection(binding, holder.viewModel, post)
            return
        }
        ViewBindingAdapter.setMarginEnd(
            binding.contentContainer,
            if (generalPreferencesManager.isQuickSideBarEnable) {
                binding.root.resources.getDimension(com.github.ykrank.androidtools.R.dimen.spacing_normal)
            } else {
                0f
            }
        )
        holder.viewModel.thread.set(threadInfo)
        holder.viewModel.pageNum.set(pageNum)
        holder.viewModel.post.set(post)
        holder.viewModel.vote.set(if ("1" == post.number) voteInfo else null)
        binding.tvShowTrade.visibility = if (post.isTrade) View.VISIBLE else View.GONE
        binding.tvShowVote.visibility = if (holder.viewModel.vote.get() != null) View.VISIBLE else View.GONE
        binding.tvCastMagic.visibility = if (post.banned) View.VISIBLE else View.GONE

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

        bindShareSelection(binding, holder.viewModel, post)
    }

    private fun bindShareSelection(
        binding: ItemPostRenderFooterBinding,
        viewModel: PostViewModel,
        post: Post
    ) {
        val state = postShareSelectionOwner?.postShareSelectionState
        val shareSelectionEnabled = state?.enabled == true
        val shareSelected = state?.selectedPostIds?.contains(post.id) == true
        binding.postShareScrim.visibility =
            if (shareSelectionEnabled && shareSelected) View.VISIBLE else View.GONE
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
            binding.postShareScrim.setOnClickListener(toggleSelection)
        } else {
            binding.root.setOnClickListener(null)
            binding.postShareScrim.setOnClickListener(null)
            binding.postShareScrim.isClickable = false
            binding.tvShowTrade.setOnClickListener {
                viewModel.onExtraHtmlClick(it)
            }
            binding.tvShowVote.setOnClickListener {
                viewModel.onVoteClick(it)
            }
            binding.tvCastMagic.setOnClickListener {
                viewModel.onAppPostClick(it)
            }
            binding.tvRateViewAll.setOnClickListener {
                post.rates?.let { rates ->
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
            }
            binding.recycleViewRates.setOnClickListener(null)
        }
    }

    private fun ensureRatesAdapter(binding: ItemPostRenderFooterBinding) {
        if (binding.recycleViewRates.adapter != null) {
            return
        }
        binding.recycleViewRates.adapter = RateDetailAdapter(binding.root.context, RateDetailAdapter.Mode.COMPACT)
        binding.recycleViewRates.layoutManager = LinearLayoutManager(binding.root.context)
        binding.recycleViewRates.isNestedScrollingEnabled = false
    }

    private fun updateRateList(binding: ItemPostRenderFooterBinding, rates: List<me.ykrank.s1next.data.api.model.Rate>) {
        val adapter = binding.recycleViewRates.adapter as? RateDetailAdapter ?: return
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

class PostRenderFooterViewHolder(
    binding: ItemPostRenderFooterBinding,
    val viewModel: PostViewModel
) : SimpleRecycleViewHolder<ItemPostRenderFooterBinding>(binding)
