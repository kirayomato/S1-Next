package me.ykrank.s1next.view.adapter

import android.app.Activity
import me.ykrank.s1next.view.adapter.delegate.HomeReplyItemAdapterDelegate
import me.ykrank.s1next.view.adapter.delegate.HomeReplyTitleAdapterDelegate

class HomeReplyRecyclerViewAdapter(activity: Activity?) : BaseRecyclerViewAdapter(activity!!) {
    init {
        addAdapterDelegate(HomeReplyTitleAdapterDelegate(activity!!))
        addAdapterDelegate(HomeReplyItemAdapterDelegate(activity))
    }
}
