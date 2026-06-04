package me.ykrank.s1next.view.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.ykrank.s1next.R
import me.ykrank.s1next.view.event.PmGroupClickEvent
import me.ykrank.s1next.view.fragment.PmFragment
import me.ykrank.s1next.view.fragment.PmGroupsFragment
import me.ykrank.s1next.view.internal.RequestCode

class PmActivity : BaseActivity() {
    private var fragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_without_drawer_and_scrolling_effect)

        if (savedInstanceState == null) {
            fragment = PmGroupsFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment!!, PmGroupsFragment.TAG)
                .commit()
        }

        lifecycleScope.launch {
            mEventBus.getClsFlow<PmGroupClickEvent>().collect { event ->
                val pmGroup = event.pmGroup
                val newFragment = PmFragment.newInstance(pmGroup.toUid.orEmpty(), pmGroup.toUsername.orEmpty())
                fragment = newFragment
                replaceFragmentWithBackStack(newFragment, PmFragment.TAG)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RequestCode.REQUEST_CODE_MESSAGE_IF_SUCCESS && resultCode == Activity.RESULT_OK) {
            val pmFragment = supportFragmentManager.findFragmentByTag(PmFragment.TAG) as? PmFragment
            pmFragment?.startSwipeRefresh()
        }
    }

    companion object {
        @JvmStatic
        fun startPmActivity(context: Context) {
            context.startActivity(Intent(context, PmActivity::class.java))
        }
    }
}
