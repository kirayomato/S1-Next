package me.ykrank.s1next.view.adapter

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.data.pref.ThemeManager
import me.ykrank.s1next.view.adapter.delegate.ThreadAdapterDelegate
import me.ykrank.s1next.viewmodel.UserViewModel

class ThreadRecyclerViewAdapter(
    activity: Activity,
    lifecycleOwner: LifecycleOwner,
    forumId: String?,
    userViewModel: UserViewModel,
    themeManager: ThemeManager,
    readPreferencesManager: ReadPreferencesManager
) : BaseRecyclerViewAdapter(
    activity
) {
    init {
        addAdapterDelegate(
            ThreadAdapterDelegate(
                activity,
                lifecycleOwner,
                forumId,
                userViewModel,
                themeManager,
                readPreferencesManager
            )
        )
    }
}
