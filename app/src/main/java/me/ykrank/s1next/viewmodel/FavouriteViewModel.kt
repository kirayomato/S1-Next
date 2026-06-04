package me.ykrank.s1next.viewmodel

import android.view.MenuItem
import android.view.View
import android.view.View.OnLongClickListener
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LifecycleOwner
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.model.Favourite
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.db.biz.ReadProgressBiz
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.view.event.FavoriteRemoveEvent
import me.ykrank.s1next.view.page.post.postlist.PostListActivity.Companion.bindClickStartForView

class FavouriteViewModel(
    private val lifecycleOwner: LifecycleOwner,
    private val readPreferencesManager: ReadPreferencesManager,
    private val readProgressBiz: ReadProgressBiz
) {
    var favourite: Favourite? = null

    fun onBind(): Function1<View, Any> {
        return { v: View ->
            bindClickStartForView(
                v, lifecycleOwner, readPreferencesManager, readProgressBiz
            ) {
                favourite?.let {
                    val thread = Thread()
                    thread.id = it.id
                    thread.title = it.title
                    thread
                }
            }
        }
    }

    fun removeFromFavourites(eventBus: EventBus): OnLongClickListener {
        return OnLongClickListener { v: View ->
            val popup = PopupMenu(v.context, v)
            popup.setOnMenuItemClickListener { menuitem: MenuItem ->
                if (menuitem.itemId == R.id.menu_popup_remove_favourite) {
                    favourite?.let {
                        eventBus.postDefault(FavoriteRemoveEvent(it.favId))
                    }
                    return@setOnMenuItemClickListener true
                }
                false
            }
            popup.inflate(R.menu.popup_favorites)
            popup.show()
            true
        }
    }
}
