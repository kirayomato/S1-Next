package me.ykrank.s1next.view.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentTransaction
import me.ykrank.s1next.R
import me.ykrank.s1next.view.fragment.WebLoginFragment
import me.ykrank.s1next.view.page.login.LoginFragment

class LoginActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_without_drawer_and_scrolling_effect)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, LoginFragment(), LoginFragment.TAG)
                .commit()
        }
    }

    fun loginInWeb() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame_layout, WebLoginFragment.instance, WebLoginFragment.TAG)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .addToBackStack(null)
            .commit()
    }

    companion object {
        @JvmStatic
        fun startLoginActivityForResultMessage(activity: Activity?) {
            val intent = Intent(activity, LoginActivity::class.java)
            activity!!.startActivity(intent)
        }
    }
}
