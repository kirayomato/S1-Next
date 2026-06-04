package me.ykrank.s1next.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import me.ykrank.s1next.R
import me.ykrank.s1next.view.fragment.FavouriteListFragment

/**
 * An Activity shows the thread lists.
 */
class FavouriteListActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_without_drawer)
        disableDrawerIndicator()

        if (savedInstanceState == null) {
            val fragment = FavouriteListFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment, FavouriteListFragment.TAG)
                .commit()
        }
    }

    companion object {
        @JvmStatic
        fun startFavouriteListActivity(context: Context?) {
            val intent = Intent(context, FavouriteListActivity::class.java)
            context!!.startActivity(intent)
        }
    }
}
