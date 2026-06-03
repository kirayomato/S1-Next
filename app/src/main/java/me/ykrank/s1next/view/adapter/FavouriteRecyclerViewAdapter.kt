package me.ykrank.s1next.view.adapter

import android.app.Activity
import androidx.lifecycle.LifecycleOwner
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.data.db.biz.ReadProgressBiz
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.view.adapter.delegate.FavouriteAdapterDelegate

class FavouriteRecyclerViewAdapter(
    activity: Activity,
    private val lifecycleOwner: LifecycleOwner,
    private val eventBus: EventBus,
    private val readPreferencesManager: ReadPreferencesManager,
    private val readProgressBiz: ReadProgressBiz,
) : BaseRecyclerViewAdapter(
    activity
) {
    init {
        addAdapterDelegate(
            FavouriteAdapterDelegate(
                activity,
                lifecycleOwner,
                eventBus,
                readPreferencesManager,
                readProgressBiz
            )
        )
    }
}
