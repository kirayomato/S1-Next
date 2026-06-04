package me.ykrank.s1next.view.adapter

import android.app.Activity
import me.ykrank.s1next.view.adapter.delegate.DarkRoomAdapterDelegate

class DarkRoomRecyclerViewAdapter(activity: Activity?) : BaseRecyclerViewAdapter(activity!!) {
    init {
        addAdapterDelegate(DarkRoomAdapterDelegate(activity!!))
    }
}
