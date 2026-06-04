package me.ykrank.s1next.view.adapter

import android.app.Activity
import me.ykrank.s1next.view.adapter.delegate.HomeThreadAdapterDelegate

class HomeThreadRecyclerViewAdapter(activity: Activity?) : BaseRecyclerViewAdapter(activity!!) {
    init {
        addAdapterDelegate(HomeThreadAdapterDelegate(activity!!))
    }
}
