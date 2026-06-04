package me.ykrank.s1next.view.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.MainThread
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.github.ykrank.androidtools.binding.LibTextViewBindingAdapter
import com.github.ykrank.androidtools.util.AnimUtils
import com.github.ykrank.androidtools.util.ContextUtils
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.util.ResourceUtil
import com.github.ykrank.androidtools.widget.AppBarOffsetChangedListener
import com.github.ykrank.androidtools.widget.glide.model.ImageInfo
import com.google.android.material.appbar.AppBarLayout
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ykrank.s1next.R
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.binding.ViewBindingAdapter
import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.ProfileProvider
import me.ykrank.s1next.data.api.model.Profile
import me.ykrank.s1next.data.db.biz.BlackListBiz
import me.ykrank.s1next.databinding.ActivityHomeBinding
import me.ykrank.s1next.view.adapter.HomeStatAdapter
import me.ykrank.s1next.view.dialog.LoginPromptDialogFragment
import me.ykrank.s1next.view.event.BlackListChangeEvent
import me.ykrank.s1next.view.internal.BlacklistMenuAction
import me.ykrank.s1next.widget.glide.AvatarFailUrlsCache
import me.ykrank.s1next.widget.track.event.ViewHomeTrackEvent
import javax.inject.Inject

/**
 * Created by ykrank on 2017/1/8.
 */

@AndroidEntryPoint
class UserHomeActivity : BaseActivity() {

    @Inject
    internal lateinit var profileProvider: ProfileProvider

    @Inject
    internal lateinit var blackListBiz: BlackListBiz

    private lateinit var binding: ActivityHomeBinding
    private var uid: String? = null
    private var name: String? = null
    private var isInBlacklist: Boolean = false
    private var blacklistMenu: MenuItem? = null
    private lateinit var adapter: HomeStatAdapter
    private var profile: Profile? = null
    private var thumbUrl: String? = null

    override val isTranslucent: Boolean
        get() = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        uid = intent.getStringExtra(ARG_UID)
        name = intent.getStringExtra(ARG_USERNAME)
        val thumbImageInfo = intent.getParcelableExtra<ImageInfo>(ARG_IMAGE_INFO)
        thumbUrl = thumbImageInfo?.url
        trackAgent.post(ViewHomeTrackEvent(uid, name))
        leavePageMsg("UserHomeActivity##uid:$uid,name:$name")

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.layoutContent.setPadding(
            binding.layoutContent.paddingLeft,
            binding.layoutContent.paddingTop,
            binding.layoutContent.paddingRight,
            ResourceUtil.getNavigationBarHeight(this)
        )
        val profile = Profile()
        profile.homeUid = uid
        profile.homeUsername = name
        bindProfile(profile)

        initTransition()
        initListener()
        setupImage()
        loadData()

        lifecycleScope.launch {
            mEventBus.getClsFlow<BlackListChangeEvent>()
                .collect { blackListEvent ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        if (blackListEvent.isAdd) {
                            blackListBiz.saveDefaultBlackList(
                                blackListEvent.authorPostId, blackListEvent.authorPostName,
                                blackListEvent.remark
                            )
                        } else {
                            blackListBiz.delDefaultBlackList(
                                blackListEvent.authorPostId,
                                blackListEvent.authorPostName
                            )
                        }
                    }
                }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_home, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        blacklistMenu = menu.findItem(R.id.menu_blacklist)
        refreshBlacklistMenu()
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }

            R.id.menu_blacklist -> {
                if (isInBlacklist) {
                    BlacklistMenuAction.removeBlacklist(this, mEventBus, uid?.toInt() ?: 0, name)
                } else {
                    BlacklistMenuAction.addBlacklist(this, uid?.toInt() ?: 0, name)
                }
                return true
            }

            R.id.menu_refresh_avatar -> {

                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun afterBlackListChange(isAdd: Boolean) {
        showShortToast(if (isAdd) R.string.blacklist_add_success else R.string.blacklist_remove_success)
        refreshBlacklistMenu()
    }

    private fun initTransition() {
        if (!intent.getBooleanExtra(ARG_TRANSITION, false)) {
            window.setSharedElementReturnTransition(null)
            window.setSharedElementReenterTransition(null)
            binding.avatar.transitionName = null

            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                com.github.ykrank.androidtools.R.anim.slide_in_right_quick,
                0
            )
            overrideActivityTransition(
                OVERRIDE_TRANSITION_CLOSE,
                0,
                com.github.ykrank.androidtools.R.anim.slide_out_right_quick
            )
        }
    }

    private fun initListener() {

        binding.appBar.addOnOffsetChangedListener(object : AppBarOffsetChangedListener() {
            override fun onStateChanged(
                appBarLayout: AppBarLayout,
                oldVerticalOffset: Int,
                verticalOffset: Int
            ) {
                val maxScroll = appBarLayout.totalScrollRange
                val oldPercentage = Math.abs(oldVerticalOffset).toFloat() / maxScroll.toFloat()
                val percentage = Math.abs(verticalOffset).toFloat() / maxScroll.toFloat()
                if (oldPercentage < PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR && percentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {
                    //Move up
                    AnimUtils.startAlphaAnimation(
                        binding.toolbarTitle,
                        TITLE_ANIMATIONS_DURATION.toLong(),
                        View.VISIBLE
                    )
                } else if (oldPercentage >= PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR && percentage < PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR) {
                    //Move down
                    AnimUtils.startAlphaAnimation(
                        binding.toolbarTitle,
                        TITLE_ANIMATIONS_DURATION.toLong(),
                        View.INVISIBLE
                    )
                }
            }
        })

        binding.avatar.setOnClickListener { v ->
            val bigAvatarUrl = Api.getAvatarBigUrl(uid)
            GalleryActivity.start(v.context, bigAvatarUrl)
        }

        binding.ivNewPm.setOnClickListener { v ->
            profile?.let {
                NewPmActivity.startNewPmActivityForResultMessage(
                    this,
                    it.homeUid, it.homeUsername
                )
            }
        }

        binding.tvFriends.setOnClickListener { v -> FriendListActivity.start(this, uid, name) }

        binding.tvThreads.setOnClickListener { v -> UserThreadActivity.start(this, uid, name) }

        binding.tvReplies.setOnClickListener { v -> UserReplyActivity.start(this, uid, name) }

        binding.recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.recyclerView.isNestedScrollingEnabled = false
        adapter = HomeStatAdapter(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupImage() {

    }

    private fun loadData() {
        val profileUid = profile?.homeUid ?: return
        lifecycleScope.launch(L.report) {
            try {
                val profile = withContext(Dispatchers.IO) {
                    profileProvider.getProfile(profileUid)
                } ?: return@launch
                renderProfile(profile)
            } catch (e: Exception) {
                L.e(e)
            }
        }
    }

    private fun renderProfile(profile: Profile) {
        if (profile.homeUid.isNullOrEmpty()) {
            profile.homeUid = uid
        }
        if (profile.homeUsername.isNullOrEmpty()) {
            profile.homeUsername = name
        }
        bindProfile(profile)
        adapter.swapDataSet(profile.stats)
    }

    private fun bindProfile(profile: Profile) {
        this.profile = profile
        ViewBindingAdapter.setUserBlurBackground(
            binding.collapsingToolbarLayout,
            null,
            null,
            mDownloadPreferencesManager,
            profile.homeUid
        )
        binding.avatar.visibility = if (mDownloadPreferencesManager.isAvatarsDownload) {
            View.VISIBLE
        } else {
            View.GONE
        }
        ImageViewBindingAdapter.loadAvatar(
            binding.avatar,
            null,
            null,
            null,
            mDownloadPreferencesManager,
            profile.homeUid,
            thumbUrl
        )
        binding.tvName.text = profile.homeUsername
        binding.tvUid.text = getString(R.string.home_label_uid, profile.homeUid)
        binding.tvGroupTitle.text = profile.groupTitle
        binding.tvFriends.text = getString(R.string.home_label_friends, profile.friends)
        binding.tvThreads.text = getString(R.string.home_label_threads, profile.threads)
        binding.tvReplies.text = getString(R.string.home_label_replies, profile.replies)
        binding.toolbarTitle.text = profile.homeUsername
        TextViewBindingAdapter.setHtml(binding.tvSignature, this, profile.signHtml)
        binding.layoutManager.visibility = if (profile.manager != null) View.VISIBLE else View.GONE
        binding.tvManager.text = profile.managerString
        binding.tvOnlineTime.text = getString(R.string.online_time_content, profile.onlineHour)
        LibTextViewBindingAdapter.setSecondTime(binding.tvRegDate, profile.regDate)
        LibTextViewBindingAdapter.setSecondTime(binding.tvLastVisitDate, profile.lastVisitDate)
        LibTextViewBindingAdapter.setSecondTime(binding.tvLastActiveDate, profile.lastActiveDate)
        LibTextViewBindingAdapter.setSecondTime(binding.tvLastPostDate, profile.lastPostDate)
    }

    @MainThread
    private fun refreshBlacklistMenu() {
        if (blacklistMenu == null) {
            return
        }

        lifecycleScope.launch(L.report) {
            val blackList = withContext(Dispatchers.IO) {
                blackListBiz.getMergedBlackList(uid?.toInt() ?: 0, name)
            }
            if (blackList != null) {
                isInBlacklist = true
                blacklistMenu?.setTitle(R.string.menu_blacklist_remove)
            } else {
                isInBlacklist = false
                blacklistMenu?.setTitle(R.string.menu_blacklist_add)
            }
        }
    }

    companion object {

        private const val PERCENTAGE_TO_SHOW_TITLE_AT_TOOLBAR = 0.71f
        private const val TITLE_ANIMATIONS_DURATION = 300

        private const val ARG_UID = "uid"
        private const val ARG_USERNAME = "username"
        private const val ARG_IMAGE_INFO = "image_info"
        private const val ARG_TRANSITION = "transition"

        fun start(
            context: Context,
            uid: String,
            userName: String?
        ) {
            val activity = resolveFragmentActivity(context, uid, userName) ?: return
            start(activity, uid, userName)
        }

        fun start(
            activity: FragmentActivity,
            uid: String,
            userName: String?
        ) {
            val user = EntryPointAccessors.fromApplication(
                activity.applicationContext,
                UserHomeLauncherEntryPoint::class.java
            ).user
            if (LoginPromptDialogFragment.showLoginPromptDialogIfNeeded(
                    activity.supportFragmentManager,
                    user
                )
            ) {
                return
            }

            val intent = Intent(activity, UserHomeActivity::class.java)
            intent.putExtra(ARG_UID, uid)
            intent.putExtra(ARG_USERNAME, userName)
            activity.startActivity(intent)
        }

        fun start(
            context: Context,
            uid: String,
            userName: String?,
            avatarView: View
        ) {
            val activity = resolveFragmentActivity(context, uid, userName) ?: return
            start(activity, uid, userName, avatarView)
        }

        fun start(
            activity: FragmentActivity,
            uid: String,
            userName: String?,
            avatarView: View
        ) {
            //Clear avatar false cache
            AvatarFailUrlsCache.removeFailUserAvatarCache(uid)
            val user = EntryPointAccessors.fromApplication(
                activity.applicationContext,
                UserHomeLauncherEntryPoint::class.java
            ).user
            if (LoginPromptDialogFragment.showLoginPromptDialogIfNeeded(
                    activity.supportFragmentManager,
                    user
                )
            ) {
                return
            }

            val baseContext = ContextUtils.getBaseContext(activity)
            if (baseContext !is Activity) {
                L.leaveMsg("uid:$uid")
                L.leaveMsg("userName:$userName")
                L.report(IllegalStateException("UserHomeActivity start error: context not instance of activity"))
                return
            }
            val imageInfo =
                avatarView.getTag(com.github.ykrank.androidtools.R.id.tag_drawable_info) as ImageInfo?
            val intent = Intent(baseContext, UserHomeActivity::class.java)
            intent.putExtra(ARG_UID, uid)
            intent.putExtra(ARG_USERNAME, userName)
            intent.putExtra(ARG_TRANSITION, true)
            if (imageInfo != null) {
                intent.putExtra(ARG_IMAGE_INFO, imageInfo)
            }
            val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                baseContext, avatarView, baseContext.getString(R.string.transition_avatar)
            )
            ActivityCompat.startActivity(baseContext, intent, options.toBundle())
        }

        private fun resolveFragmentActivity(
            context: Context,
            uid: String,
            userName: String?
        ): FragmentActivity? {
            val activity = ContextUtils.findFragmentActivity(context)
            if (activity != null) {
                return activity
            }
            val baseContext = ContextUtils.getBaseContext(context)
            L.leaveMsg("uid:$uid")
            L.leaveMsg("userName:$userName")
            L.report(
                IllegalStateException(
                    "UserHomeActivity start error: context not instance of FragmentActivity: " +
                        "${context.javaClass.name} -> ${baseContext.javaClass.name}"
                )
            )
            return null
        }
    }
}
