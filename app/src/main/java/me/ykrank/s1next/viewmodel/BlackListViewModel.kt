package me.ykrank.s1next.viewmodel;

import android.view.View
import android.widget.TextView
import com.github.ykrank.androidtools.ui.internal.CoordinatorLayoutAnchorDelegateBaseImpl
import com.google.android.material.R
import com.google.android.material.snackbar.Snackbar
import me.ykrank.s1next.data.db.dbmodel.BlackList


class BlackListViewModel {
    var blacklist: BlackList? = null

    fun clickSnackbar(): View.OnClickListener {
        return View.OnClickListener { v: View ->
            val snackbar = Snackbar.make(
                v.rootView,
                (v as TextView).text,
                Snackbar.LENGTH_SHORT
            )
            val textView = snackbar.view
                .findViewById<TextView>(R.id.snackbar_text)
            textView.maxLines = CoordinatorLayoutAnchorDelegateBaseImpl.SNACK_BAR_MAX_LINE
            snackbar.show()
        }
    }
}
