package me.ykrank.s1next.view.adapter

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import me.ykrank.s1next.data.User
import me.ykrank.s1next.view.adapter.delegate.PmLeftAdapterDelegate
import me.ykrank.s1next.view.adapter.delegate.PmRightAdapterDelegate

class PmRecyclerViewAdapter(activity: Activity, lifecycleOwner: LifecycleOwner, user: User) :
    BaseRecyclerViewAdapter(activity) {
    init {
        addAdapterDelegate(PmLeftAdapterDelegate(activity, lifecycleOwner, user))
        addAdapterDelegate(PmRightAdapterDelegate(activity, lifecycleOwner, user))
    }
}
