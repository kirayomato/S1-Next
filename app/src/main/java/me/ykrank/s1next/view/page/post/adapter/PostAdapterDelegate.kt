package me.ykrank.s1next.view.page.post.adapter

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.github.ykrank.androidtools.binding.LibTextViewBindingAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewHolder
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.widget.EventBus
import kotlinx.coroutines.launch
import me.ykrank.s1next.R
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.binding.ViewBindingAdapter
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.ApiCacheProvider
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Profile
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.Vote
import me.ykrank.s1next.data.pref.GeneralPreferencesManager
import me.ykrank.s1next.databinding.ItemPostBinding
import me.ykrank.s1next.view.activity.RateDetailsListActivity
import me.ykrank.s1next.view.adapter.RateDetailAdapter
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate
import me.ykrank.s1next.view.page.post.share.PostShareSelectionOwner
import me.ykrank.s1next.view.page.post.share.PostShareSelectionPayload
import me.ykrank.s1next.view.page.post.viewmodel.PostViewModel
import me.ykrank.s1next.widget.span.FixedSpannableFactory
import me.ykrank.s1next.widget.span.PostMovementMethod

class PostAdapterDelegate(
    private val fragment: Fragment,
    context: Context,
    private val postShareSelectionOwner: PostShareSelectionOwner? = null,
    private val mEventBus: EventBus,
    private val mUser: User,
    private val mApiCache: ApiCacheProvider,
    private val mGeneralPreferencesManager: GeneralPreferencesManager
) :
    BaseAdapterDelegate<Post, PostViewHolder>(context, Post::class.java) {

    private var threadInfo: Thread? = null
    private var voteInfo: Vote? = null
    private var pageNum: Int = 1
    private val authorProfiles = mutableMapOf<String, Profile>()

    private fun setTextSelectable(binding: ItemPostBinding, selectable: Boolean) {
        binding.authorName.setTextIsSelectable(selectable)

        binding.tvReply.setTextIsSelectable(selectable)
        binding.tvReply.movementMethod = PostMovementMethod.instance
    }

    override fun isForViewType(items: MutableList<Any>, position: Int): Boolean {
        val item = items[position]
        return item is Post && item.hide == Post.HIDE_NO
    }

    public override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemPostBinding.inflate(mLayoutInflater, parent, false)
        val viewModel = PostViewModel(fragment.viewLifecycleOwner, mEventBus, mUser)
        binding.avatar.setOnClickListener(viewModel::onAvatarClick)
        binding.avatar.setOnLongClickListener(viewModel::onAvatarLongClick)
        binding.tvFloor.setOnClickListener(viewModel::showFloorActionMenu)
        binding.tvShowTrade.setOnClickListener(viewModel::onExtraHtmlClick)
        binding.tvShowVote.setOnClickListener(viewModel::onVoteClick)
        binding.tvCastMagic.setOnClickListener(viewModel::onAppPostClick)
        binding.ivRateAdd.setOnClickListener(viewModel::onRateClick)
        TextViewBindingAdapter.increaseClickingArea(
            binding.tvFloor,
            binding.tvFloor.resources.getDimension(
                com.github.ykrank.androidtools.R.dimen.minimum_touch_target_size
            )
        )

        binding.tvReply.setSpannableFactory(FixedSpannableFactory())

        setTextSelectable(binding, false)

        return PostViewHolder(binding, viewModel)
    }

    override fun onBindViewHolder(
        items: MutableList<Any>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<Any>
    ) {

        val viewHolder = holder as? PostViewHolder
        if (viewHolder == null) {
            super.onBindViewHolder(items, position, holder, payloads)
            return
        }

        if (payloads.isEmpty()) {
            super.onBindViewHolder(items, position, holder, payloads)
            return
        }
        if (payloads.contains(PostShareSelectionPayload)) {
            val post = items[position] as? Post ?: return
            bindShareSelection(viewHolder.binding, viewHolder.viewModel, post)
            return
        }
        if (payloads.any { it is Profile }) {
            val post = items[position] as? Post ?: return
            val profile = post.authorId?.let { authorProfiles[it] }
            viewHolder.viewModel.authorProfile = profile
            bindAuthorProfile(viewHolder.binding, profile)
            return
        }
        super.onBindViewHolder(items, position, holder, payloads)
    }

    override fun onBindViewHolderData(
        post: Post,
        position: Int,
        holder: PostViewHolder,
        payloads: List<Any>
    ) {
        val binding = holder.binding

        ViewBindingAdapter.setMarginEnd(
            binding.contentContainer,
            if (mGeneralPreferencesManager.isQuickSideBarEnable) {
                binding.root.resources.getDimension(com.github.ykrank.androidtools.R.dimen.spacing_normal)
            } else {
                0f
            }
        )

        val selectable = false
        if (selectable != binding.tvReply.isTextSelectable) {
            setTextSelectable(binding, selectable)
        }

        holder.viewModel.thread = threadInfo
        holder.viewModel.pageNum = pageNum
        holder.viewModel.post = post
        holder.viewModel.authorProfile = post.authorId?.let { authorProfiles[it] }
        holder.viewModel.vote = if ("1" == post.number) voteInfo else null
        binding.threadTitle.text = threadInfo?.title
        binding.threadTitle.visibility =
            if (pageNum == 1 && post.isFirst && threadInfo?.title != null) View.VISIBLE else View.GONE
        ImageViewBindingAdapter.loadAvatar(binding.avatar, null, post.authorId)
        binding.authorName.text = post.authorName
        binding.originalPosterTag.visibility = if (post.isOpPost) View.VISIBLE else View.GONE
        LibTextViewBindingAdapter.setRelativeDateTime(binding.tvDatetime, post.dateTime * 1000)
        binding.tvFloor.text = holder.viewModel.floor
        TextViewBindingAdapter.setReply(binding.tvReply, null, null, fragment.viewLifecycleOwner, post)
        binding.tvShowTrade.visibility = if (post.isTrade) View.VISIBLE else View.GONE
        binding.tvShowVote.visibility = if (holder.viewModel.vote != null) View.VISIBLE else View.GONE
        binding.tvCastMagic.visibility = if (post.banned) View.VISIBLE else View.GONE

        val rates = post.rates
        val context = binding.root.context

        if (rates != null) {
            binding.layoutRates.visibility = View.VISIBLE
            binding.tvRateViewAll.setOnClickListener {
                if (rates.isNotEmpty()) {
                    RateDetailsListActivity.start(context, ArrayList(rates))
                } else {
                    fragment.lifecycleScope.launch(L.report) {
                        val newRates =
                            mApiCache.getPostRates(threadInfo?.id ?: "", post.id).data
                                ?: emptyList()
                        post.rates = newRates
                        val adapter = binding.recycleViewRates.adapter as? RateDetailAdapter
                        if (adapter != null) {
                            if (newRates.size > 10) {
                                adapter.diffNewDataSet(newRates.subList(0, 10), true)
                            } else {
                                adapter.diffNewDataSet(newRates, true)
                            }
                        }
                    }
                }
            }
        } else {
            binding.layoutRates.visibility = View.GONE
        }
        if (rates?.isNotEmpty() == true) {
            binding.recycleViewRates.visibility = View.VISIBLE
            if (binding.recycleViewRates.adapter == null) {
                binding.recycleViewRates.adapter = RateDetailAdapter(context, RateDetailAdapter.Mode.COMPACT)
                binding.recycleViewRates.layoutManager = LinearLayoutManager(context)
                binding.recycleViewRates.isNestedScrollingEnabled = false
            }
            val adapter = binding.recycleViewRates.adapter as RateDetailAdapter

            if (rates.size > 10) {
                adapter.diffNewDataSet(rates.subList(0, 10), true)
            } else {
                adapter.diffNewDataSet(rates, true)
            }
        } else {
            binding.recycleViewRates.visibility = View.GONE
        }

        bindAuthorProfile(binding, post.authorId?.let { authorProfiles[it] })
        bindShareSelection(binding, holder.viewModel, post)
    }

    private fun bindAuthorProfile(binding: ItemPostBinding, profile: Profile?) {
        val goose = profile?.goose
        binding.goose.text = goose
        TextViewBindingAdapter.setGoose(binding.goose, goose)
        TextViewBindingAdapter.setRegistrationAge(binding.registrationAge, profile?.regDate)
    }

    private fun bindShareSelection(binding: ItemPostBinding, viewModel: PostViewModel, post: Post) {
        val shareSelectionState = postShareSelectionOwner?.postShareSelectionState
        val shareSelectionEnabled = shareSelectionState?.enabled == true
        val shareSelected = shareSelectionState?.selectedPostIds?.contains(post.id) == true
        binding.postShareScrim.visibility =
            if (shareSelectionEnabled && shareSelected) View.VISIBLE else View.GONE
        binding.postShareCheckboxContainer.visibility =
            if (shareSelectionEnabled) View.VISIBLE else View.GONE
        binding.postShareCheckbox.isChecked = shareSelected
        ViewBindingAdapter.setMarginEnd(
            binding.postShareCheckboxContainer,
            binding.root.resources.getDimension(
                if (mGeneralPreferencesManager.isQuickSideBarEnable) {
                    R.dimen.post_share_checkbox_margin_end_with_quick_sidebar
                } else {
                    R.dimen.post_share_checkbox_margin_end
                }
            )
        )
        val headerViews = listOf(
            binding.root,
            binding.threadTitle,
            binding.authorName,
            binding.goose,
            binding.registrationAge,
            binding.originalPosterTag,
            binding.tvDatetime,
            binding.tvFloor
        )
        if (shareSelectionEnabled) {
            val toggleSelection = View.OnClickListener {
                postShareSelectionOwner?.togglePostShareSelection(post.id)
            }
            (headerViews + binding.avatar).forEach {
                it.setOnClickListener(toggleSelection)
                it.setOnLongClickListener(null)
            }
            binding.postShareScrim.setOnClickListener(toggleSelection)
        } else {
            binding.root.setOnClickListener(null)
            binding.threadTitle.setOnClickListener(null)
            binding.authorName.setOnClickListener(null)
            binding.goose.setOnClickListener(null)
            binding.registrationAge.setOnClickListener(null)
            binding.originalPosterTag.setOnClickListener(null)
            binding.tvDatetime.setOnClickListener(null)
            binding.avatar.setOnClickListener {
                viewModel.onAvatarClick(it)
            }
            binding.avatar.setOnLongClickListener {
                viewModel.onAvatarLongClick(it)
            }
            binding.postShareScrim.setOnClickListener(null)
            binding.postShareScrim.isClickable = false
            val showPostActionMenu = View.OnLongClickListener {
                viewModel.showFloorActionMenu(it)
                true
            }
            headerViews.forEach {
                it.setOnLongClickListener(showPostActionMenu)
            }
            binding.tvFloor.setOnClickListener {
                viewModel.showFloorActionMenu(it)
            }
        }
    }

    // Bug workaround for losing text selection ability, see:
    // https://code.google.com/p/android/issues/detail?id=208169
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (false) {
            val binding = (holder as PostViewHolder).binding
            binding.authorName.isEnabled = false
            binding.tvReply.isEnabled = false
            binding.authorName.isEnabled = true
            binding.tvReply.isEnabled = true
        }
    }

    fun setThreadInfo(threadInfo: Thread, pageNum: Int) {
        this.threadInfo = threadInfo
        this.pageNum = pageNum
    }

    fun setVoteInfo(voteInfo: Vote?) {
        this.voteInfo = voteInfo
    }

    fun setAuthorProfile(uid: String, profile: Profile) {
        authorProfiles[uid] = profile
    }

}

class PostViewHolder(
    binding: ItemPostBinding,
    val viewModel: PostViewModel
) : SimpleRecycleViewHolder<ItemPostBinding>(binding)
