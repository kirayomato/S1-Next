package me.ykrank.s1next.view.adapter

import android.app.Activity
import me.ykrank.s1next.view.adapter.delegate.SearchForumAdapterDelegate
import me.ykrank.s1next.view.adapter.delegate.SearchUserAdapterDelegate

class SearchRecyclerViewAdapter(activity: Activity?) : BaseRecyclerViewAdapter(activity!!, false) {
    init {
        addAdapterDelegate(SearchForumAdapterDelegate(activity!!))
        addAdapterDelegate(SearchUserAdapterDelegate(activity))
    }
}
