package me.ykrank.s1next.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.ykrank.androidtools.util.L
import me.ykrank.s1next.R
import me.ykrank.s1next.view.fragment.UserThreadFragment
import me.ykrank.s1next.widget.track.event.ViewUserThreadTrackEvent

class UserThreadActivity : BaseActivity() {
    private var fragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_without_drawer_and_scrolling_effect)

        val uid = intent.getStringExtra(ARG_UID)
        val name = intent.getStringExtra(ARG_USERNAME)
        trackAgent.post(ViewUserThreadTrackEvent(uid, name))
        L.leaveMsg("UserThreadActivity##uid:$uid,name:$name")
        title = getString(R.string.title_user_threads, name)

        if (savedInstanceState == null) {
            fragment = UserThreadFragment.newInstance(uid!!)
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment!!, UserThreadFragment.TAG)
                .commit()
        }
    }

    companion object {
        private const val ARG_UID = "uid"
        private const val ARG_USERNAME = "username"

        @JvmStatic
        fun start(context: Context?, uid: String?, userName: String?) {
            val intent = Intent(context, UserThreadActivity::class.java)
            intent.putExtra(ARG_UID, uid)
            intent.putExtra(ARG_USERNAME, userName)
            context!!.startActivity(intent)
        }
    }
}
