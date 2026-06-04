package me.ykrank.s1next.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.ykrank.androidtools.util.L
import me.ykrank.s1next.R
import me.ykrank.s1next.view.fragment.FriendListFragment
import me.ykrank.s1next.widget.track.event.ViewUserFriendsTrackEvent

class FriendListActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_without_drawer_and_scrolling_effect)

        val uid = intent.getStringExtra(ARG_UID)
        val name = intent.getStringExtra(ARG_USERNAME)
        trackAgent.post(ViewUserFriendsTrackEvent(uid, name))
        L.leaveMsg("FriendListActivity##uid:$uid,name:$name")

        if (savedInstanceState == null) {
            val fragment: Fragment = FriendListFragment.newInstance(uid!!)
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment, FriendListFragment.TAG)
                .commit()
        }
    }

    companion object {
        private const val ARG_UID = "uid"
        private const val ARG_USERNAME = "username"

        @JvmStatic
        fun start(context: Context?, uid: String?, userName: String?) {
            val intent = Intent(context, FriendListActivity::class.java)
            intent.putExtra(ARG_UID, uid)
            intent.putExtra(ARG_USERNAME, userName)
            context!!.startActivity(intent)
        }
    }
}
