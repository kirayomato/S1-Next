package me.ykrank.s1next.view.page.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import me.ykrank.s1next.R
import me.ykrank.s1next.view.activity.BaseActivity
import me.ykrank.s1next.view.page.login.AppLoginFragment

class AppLoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_without_drawer_and_scrolling_effect)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, AppLoginFragment(), AppLoginFragment.TAG)
                .commit()
        }
    }

    companion object {
        @JvmStatic
        fun startLoginActivityForResultMessage(activity: Activity?) {
            val intent = Intent(activity, AppLoginActivity::class.java)
            activity!!.startActivity(intent)
        }
    }
}
