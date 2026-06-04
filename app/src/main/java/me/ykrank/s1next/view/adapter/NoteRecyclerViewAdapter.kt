package me.ykrank.s1next.view.adapter

import android.app.Activity
import me.ykrank.s1next.view.adapter.delegate.NoteAdapterDelegate

class NoteRecyclerViewAdapter(activity: Activity?) : BaseRecyclerViewAdapter(activity!!) {
    init {
        addAdapterDelegate(NoteAdapterDelegate(activity!!))
    }
}
