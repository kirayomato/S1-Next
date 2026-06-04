package me.ykrank.s1next.view.adapter

import android.app.Activity
import me.ykrank.s1next.data.pref.ThemeManager
import me.ykrank.s1next.view.adapter.delegate.ForumAdapterDelegate

class ForumRecyclerViewAdapter(
    activity: Activity?,
    themeManager: ThemeManager
) : BaseRecyclerViewAdapter(activity!!) {
    init {
        addAdapterDelegate(ForumAdapterDelegate(activity!!, themeManager))
    }
}
