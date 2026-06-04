package me.ykrank.s1next.view.adapter

import android.app.Activity
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.data.User
import me.ykrank.s1next.view.adapter.delegate.PmGroupsAdapterDelegate

class PmGroupsRecyclerViewAdapter(
    activity: Activity?,
    eventBus: EventBus,
    user: User
) : BaseRecyclerViewAdapter(activity!!) {
    init {
        addAdapterDelegate(PmGroupsAdapterDelegate(activity!!, eventBus, user))
    }
}
