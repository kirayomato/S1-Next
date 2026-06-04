package me.ykrank.s1next.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import me.ykrank.s1next.R
import me.ykrank.s1next.view.fragment.OpenSourceLicensesFragment

/**
 * An Activity shows the libraries and files we use in our app.
 */
class OpenSourceLicensesActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_without_drawer)

        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                .add(R.id.frame_layout, OpenSourceLicensesFragment())
                .commit()
        }
    }

    companion object {
        @JvmField
        val TAG: String = OpenSourceLicensesActivity::class.java.name

        @JvmStatic
        fun startOpenSourceLicensesActivity(context: Context?) {
            val intent = Intent(context, OpenSourceLicensesActivity::class.java)
            context!!.startActivity(intent)
        }
    }
}
