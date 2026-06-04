package me.ykrank.s1next.view.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.core.app.ActivityCompat
import androidx.core.app.NavUtils
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.lifecycleScope
import com.github.ykrank.androidtools.extension.await
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.util.RxJavaUtil
import com.google.common.base.Optional
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Single
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import me.ykrank.s1next.R
import me.ykrank.s1next.binding.SpinnerBindingAdapter
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.databinding.ToolbarSpinnerBinding
import me.ykrank.s1next.view.event.LoginEvent
import me.ykrank.s1next.view.fragment.ForumFragment
import me.ykrank.s1next.view.internal.ToolbarDropDownInterface
import me.ykrank.s1next.view.page.post.postlist.PostListActivity
import javax.inject.Inject

/**
 * An Activity shows the forum groups.
 *
 *
 * This Activity has Spinner in Toolbar to switch between different forum groups.
 */
@AndroidEntryPoint
class ForumActivity : BaseActivity(), ToolbarDropDownInterface.Callback, AdapterView.OnItemSelectedListener {

    @Inject
    internal lateinit var mReadPrefManager: ReadPreferencesManager

    private var mToolbarSpinnerBinding: ToolbarSpinnerBinding? = null

    /**
     * Stores selected Spinner position.
     */
    private var mSelectedPosition = 0

    private lateinit var onItemSelectedListener: ToolbarDropDownInterface.OnItemSelectedListener

    private lateinit var fragment: ForumFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_base)

        val fragmentManager = supportFragmentManager
        if (savedInstanceState == null) {
            restoreFromInterrupt()

            fragment = ForumFragment()
            fragmentManager.beginTransaction().add(R.id.frame_layout, fragment, ForumFragment.TAG)
                .commit()
        } else {
            mSelectedPosition = savedInstanceState.getInt(STATE_SPINNER_SELECTED_POSITION)
            fragment = fragmentManager.findFragmentByTag(ForumFragment.TAG) as ForumFragment
        }

        onItemSelectedListener = fragment
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        fragment.startSwipeRefresh()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt(STATE_SPINNER_SELECTED_POSITION, mSelectedPosition)
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        mSelectedPosition = position
        onItemSelectedListener.onToolbarDropDownItemSelected(mSelectedPosition)
    }

    override fun onNothingSelected(parent: AdapterView<*>) {}

    override fun setupToolbarDropDown(dropDownItemList: List<CharSequence>) {
        val binding: ToolbarSpinnerBinding
        if (mToolbarSpinnerBinding == null) {
            setTitle("")

            // add Spinner to Toolbar
            binding = ToolbarSpinnerBinding.inflate(
                layoutInflater,
                toolbar.get(),
                true
            )
            binding.spinner.onItemSelectedListener = this
            // let spinner's parent to handle clicking event in order
            // to increase spinner's clicking area.
            binding.spinnerContainer.setOnClickListener { v -> binding.spinner.performClick() }

            mToolbarSpinnerBinding = binding
        } else {
            binding = mToolbarSpinnerBinding as ToolbarSpinnerBinding
        }

        SpinnerBindingAdapter.setForumGroupNameList(binding.spinner, dropDownItemList, mSelectedPosition)

    }

    private fun restoreFromInterrupt() {
        lifecycleScope.launch {
            try {
                val readProgress = Single.just(0)
                    .map { Optional.fromNullable(mReadPrefManager.lastReadProgress) }
                    .compose(RxJavaUtil.iOSingleTransformer())
                    .await()
                if (readProgress.isPresent()) {
                    PostListActivity.start(this@ForumActivity, readProgress.get())
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) return@launch
                L.report(throwable)
            } finally {
                mReadPrefManager.saveLastReadProgress(null)
            }
        }
    }

    companion object {

        /**
         * The serialization (saved instance state) Bundle key representing
         * the position of the selected spinner item.
         */
        private val STATE_SPINNER_SELECTED_POSITION = "spinner_selected_position"

        fun start(activity: Activity) {
            val intent = Intent(activity, ForumActivity::class.java)
            // if this activity is not part of this app's task
            if (NavUtils.shouldUpRecreateTask(activity, intent)) {
                // finish all our Activities in that app
                ActivityCompat.finishAffinity(activity)
                // create a new task when navigating up with
                // a synthesized back stack
                TaskStackBuilder.create(activity)
                    .addNextIntentWithParentStack(intent)
                    .startActivities()
            } else {
                // back to ForumActivity (main Activity)
                NavUtils.navigateUpTo(activity, intent)
            }
        }
    }
}
