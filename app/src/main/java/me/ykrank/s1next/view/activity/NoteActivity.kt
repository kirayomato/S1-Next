package me.ykrank.s1next.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import me.ykrank.s1next.R
import me.ykrank.s1next.view.fragment.NoteFragment

class NoteActivity : BaseActivity() {
    private var fragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_without_drawer)

        if (savedInstanceState == null) {
            fragment = NoteFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment!!, NoteFragment.TAG)
                .commit()
        }
    }

    companion object {
        @JvmStatic
        fun start(context: Context?) {
            val intent = Intent(context, NoteActivity::class.java)
            context!!.startActivity(intent)
        }
    }
}
