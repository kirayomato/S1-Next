package me.ykrank.s1next.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SearchView
import com.github.ykrank.androidtools.util.L
import me.ykrank.s1next.R
import me.ykrank.s1next.view.fragment.HistoryListFragment
import me.ykrank.s1next.view.fragment.HistoryListFragment.Companion.newInstance
import me.ykrank.s1next.view.page.setting.SettingsActivity
import me.ykrank.s1next.widget.track.event.ViewHistoryTrackEvent

/**
 * Activity show post view history list
 */
class HistoryActivity : BaseActivity() {
    private val mode: String
        get() = intent.getStringExtra(ARG_MODE) ?: MODE_HISTORY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_without_drawer_and_scrolling_effect)
        trackAgent.post(ViewHistoryTrackEvent())
        L.leaveMsg("HistoryActivity")
        setTitle(
            if (mode == MODE_POST_BACKUP) {
                R.string.title_post_backups
            } else {
                R.string.title_history
            }
        )
        if (savedInstanceState == null) {
            val fragment: Fragment = newInstance(mode)
            supportFragmentManager.beginTransaction().add(
                R.id.frame_layout, fragment,
                HistoryListFragment.TAG
            ).commit()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_history, menu)
        menu.findItem(R.id.menu_backup_restore).isVisible = mode == MODE_POST_BACKUP
        val searchItem = menu.findItem(R.id.menu_search)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                updateSearchQuery(query.orEmpty())
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                updateSearchQuery(newText.orEmpty())
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_backup_restore -> {
                SettingsActivity.startBackupSettingsActivity(this)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateSearchQuery(query: String) {
        (supportFragmentManager.findFragmentByTag(HistoryListFragment.TAG) as? HistoryListFragment)
            ?.updateQuery(query)
    }

    companion object {
        const val MODE_HISTORY = "history"
        const val MODE_POST_BACKUP = "post_backup"
        private const val ARG_MODE = "mode"

        fun start(context: Context) {
            start(context, MODE_HISTORY)
        }

        fun startPostBackups(context: Context) {
            start(context, MODE_POST_BACKUP)
        }

        private fun start(context: Context, mode: String) {
            val intent = Intent(context, HistoryActivity::class.java)
                .putExtra(ARG_MODE, mode)
            context.startActivity(intent)
        }
    }
}
