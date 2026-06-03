package me.ykrank.s1next.view.page.post.adapter

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
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.ApiCacheProvider
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Profile
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.Vote
import me.ykrank.s1next.data.pref.GeneralPreferencesManager
import me.ykrank.s1next.databinding.ItemPostBinding
import me.ykrank.s1next.databinding.ItemRateDetailBinding
import me.ykrank.s1next.view.activity.RateDetailsListActivity
import me.ykrank.s1next.view.activity.UserHomeActivity
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate
import me.ykrank.s1next.view.page.post.share.PostShareSelectionOwner
import me.ykrank.s1next.view.page.post.share.PostShareSelectionPayload
import me.ykrank.s1next.view.page.post.viewmodel.PostViewModel
import me.ykrank.s1next.widget.span.FixedSpannableFactory
import me.ykrank.s1next.widget.span.PostMovementMethod

class PostAdapterDelegate(
    private val fragment: Fragment,
    context: Context,
    private val postShareSelectionOwner: PostShareSelectionOwner? = null
) :
    BaseAdapterDelegate<Post, SimpleRecycleViewHolder<ItemPostBinding>>(context, Post::class.java) {

    private val mEventBus: EventBus = App.preAppComponent.eventBus
    private val mUser: User by lazy { App.appComponent.user }
    private val mApiCache: ApiCacheProvider by lazy { App.appComponent.apiCacheProvider }
    private val mGeneralPreferencesManager: GeneralPreferencesManager =
        App.preAppComponent.generalPreferencesManager
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
        val binding = DataBindingUtil.inflate<ItemPostBinding>(
            mLayoutInflater,
            R.layout.item_post, parent, false
        )
        binding.postViewModel = PostViewModel(fragment.viewLifecycleOwner, mEventBus, mUser)

        binding.tvReply.setSpannableFactory(FixedSpannableFactory())

        setTextSelectable(binding, false)

        return SimpleRecycleViewHolder(binding)
    }

    override fun onBindViewHolder(
        items: MutableList<Any>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<Any>
    ) {

        val viewHolder = holder as? SimpleRecycleViewHolder<ItemPostBinding>
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
            bindShareSelection(viewHolder.binding, post)
            return
        }
        if (payloads.any { it is Profile }) {
            val post = items[position] as? Post ?: return
            val profile = post.authorId?.let { authorProfiles[it] }
            viewHolder.binding.postViewModel?.authorProfile?.set(profile)
            viewHolder.binding.executePendingBindings()
            bindAuthorProfile(viewHolder.binding, profile)
            return
        }
        super.onBindViewHolder(items, position, holder, payloads)
    }

    override fun onBindViewHolderData(
        post: Post,
        position: Int,
        holder: SimpleRecycleViewHolder<ItemPostBinding>,
        payloads: List<Any>
    ) {
        val binding = holder.binding

        binding.quickSidebarEnable = mGeneralPreferencesManager.isQuickSideBarEnable

        val selectable = false
        if (selectable != binding.tvReply.isTextSelectable) {
            setTextSelectable(binding, selectable)
        }

        binding.postViewModel?.let {
            it.thread.set(threadInfo)
            it.pageNum.set(pageNum)
            it.post.set(post)
            it.authorProfile.set(post.authorId?.let { authorProfiles[it] })

            if ("1" == post.number) {
                it.vote.set(voteInfo)
            } else {
                it.vote.set(null)
            }
        }

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
                        val adapter = binding.recycleViewRates.adapter as SimpleRecycleViewAdapter?
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
                binding.recycleViewRates.adapter = SimpleRecycleViewAdapter(
                    context,
                    R.layout.item_rate_detail,
                    true,
                    { _, rateBinding ->
                        val bind = rateBinding as? ItemRateDetailBinding
                        bind?.model?.apply {
                            val uid = this.uid
                            val uname = this.uname
                            bind.avatar.setOnClickListener {
                                if (uid != null && uname != null) {
                                    //个人主页
                                    UserHomeActivity.start(
                                        it.context as FragmentActivity,
                                        uid,
                                        uname,
                                        it
                                    )
                                }
                            }
                        }

                    })
                binding.recycleViewRates.layoutManager = LinearLayoutManager(context)
                binding.recycleViewRates.isNestedScrollingEnabled = false
            }
            val adapter = binding.recycleViewRates.adapter as SimpleRecycleViewAdapter

            if (rates.size > 10) {
                adapter.diffNewDataSet(rates.subList(0, 10), true)
            } else {
                adapter.diffNewDataSet(rates, true)
            }
        } else {
            binding.recycleViewRates.visibility = View.GONE
        }

        binding.executePendingBindings()
        bindAuthorProfile(binding, post.authorId?.let { authorProfiles[it] })
        bindShareSelection(binding, post)
    }

    private fun bindAuthorProfile(binding: ItemPostBinding, profile: Profile?) {
        val goose = profile?.goose
        binding.goose.text = goose
        TextViewBindingAdapter.setGoose(binding.goose, goose)
        TextViewBindingAdapter.setRegistrationAge(binding.registrationAge, profile?.regDate)
    }

    private fun bindShareSelection(binding: ItemPostBinding, post: Post) {
        val shareSelectionState = postShareSelectionOwner?.postShareSelectionState
        val shareSelectionEnabled = shareSelectionState?.enabled == true
        binding.postShareSelectionEnabled = shareSelectionEnabled
        binding.postShareSelected = shareSelectionState?.selectedPostIds?.contains(post.id) == true
        binding.executePendingBindings()
        val headerViews = listOf(
            binding.root,
            binding.threadTitle,
            binding.avatar,
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
            headerViews.forEach {
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
                binding.postViewModel?.onAvatarClick(it)
            }
            binding.postShareScrim.setOnClickListener(null)
            binding.postShareScrim.isClickable = false
            val showPostActionMenu = View.OnLongClickListener {
                binding.postViewModel?.showFloorActionMenu(it)
                true
            }
            headerViews.forEach {
                it.setOnLongClickListener(showPostActionMenu)
            }
            binding.tvFloor.setOnClickListener {
                binding.postViewModel?.showFloorActionMenu(it)
            }
        }
    }

    // Bug workaround for losing text selection ability, see:
    // https://code.google.com/p/android/issues/detail?id=208169
    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)
        if (false) {
            val binding = (holder as SimpleRecycleViewHolder<ItemPostBinding>).binding
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
