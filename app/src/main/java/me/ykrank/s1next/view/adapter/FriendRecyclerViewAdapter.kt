package me.ykrank.s1next.view.adapter

import android.app.Activity
import me.ykrank.s1next.view.adapter.delegate.FriendAdapterDelegate

class FriendRecyclerViewAdapter(activity: Activity?) : BaseRecyclerViewAdapter(activity!!) {
    init {
        addAdapterDelegate(FriendAdapterDelegate(activity!!))
    }
}
