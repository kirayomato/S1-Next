package me.ykrank.s1next.viewmodel

import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LifecycleOwner
import com.github.ykrank.androidtools.util.ContextUtils
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.R
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.app.model.AppPost
import me.ykrank.s1next.data.api.app.model.AppThread
import me.ykrank.s1next.view.activity.UserHomeActivity
import me.ykrank.s1next.view.activity.WebViewActivity
import me.ykrank.s1next.view.event.EditAppPostEvent
import me.ykrank.s1next.view.event.QuoteEvent
import me.ykrank.s1next.view.event.RateEvent
import me.ykrank.s1next.view.internal.BlacklistMenuAction

class AppPostViewModel(
    val lifecycleOwner: LifecycleOwner,
    private val eventBus: EventBus,
    private val user: User
) {

    var post: AppPost? = null
    var thread: AppThread? = null

    val floor: CharSequence?
        get() {
            val p = post ?: return null
            return "#${p.position}"
        }

    fun onAvatarClick(v: View) {
        post?.let {
            //个人主页
            UserHomeActivity.start(
                v.context,
                "" + it.authorId,
                it.author,
                v
            )
        }
    }

    fun onLongClick(v: View): Boolean {
        //长按显示抹布菜单
        val popup = PopupMenu(v.context, v)
        val postData = post
        popup.setOnMenuItemClickListener { menuitem: MenuItem ->
            when (menuitem.itemId) {
                R.id.menu_popup_blacklist -> {
                    if (menuitem.title == v.context.getString(R.string.menu_blacklist_remove)) {
                        BlacklistMenuAction.removeBlacklist(
                            lifecycleOwner, eventBus, postData?.authorId ?: 0, postData?.author
                        )
                    } else {
                        val activity = ContextUtils.findFragmentActivity(v.context)
                        if (activity != null) {
                            BlacklistMenuAction.addBlacklist(
                                activity,
                                postData?.authorId ?: 0, postData?.author
                            )
                        } else {
                            L.report(IllegalStateException("抹布时头像Context不为FragmentActivity${v.context}"))
                        }
                    }
                    return@setOnMenuItemClickListener true
                }

                else -> return@setOnMenuItemClickListener false
            }
        }
        popup.inflate(R.menu.popup_blacklist)
        if (postData?.hide == true) {
            popup.menu.findItem(R.id.menu_popup_blacklist).setTitle(R.string.menu_blacklist_remove)
        }
        popup.show()
        return true
    }

    //click floor textView, show popup menu
    fun showFloorActionMenu(v: View) {
        val popup = PopupMenu(v.context, v)
        popup.setOnMenuItemClickListener { menuitem: MenuItem ->
            when (menuitem.itemId) {
                R.id.menu_popup_reply -> {
                    onReplyClick(v)
                    return@setOnMenuItemClickListener true
                }

                R.id.menu_popup_rate -> {
                    onRateClick(v)
                    return@setOnMenuItemClickListener true
                }

                R.id.menu_popup_edit -> {
                    onEditClick(v)
                    return@setOnMenuItemClickListener true
                }

                else -> return@setOnMenuItemClickListener false
            }
        }
        popup.inflate(R.menu.popup_post_floor)

        val editPostMenuItem = popup.menu.findItem(R.id.menu_popup_edit)
        editPostMenuItem.isVisible = user.isLogged && user.uid == post?.authorId?.toString()
        popup.show()
    }

    fun onReplyClick(v: View) {
        post?.let {
            eventBus.postDefault(QuoteEvent(it.pid.toString(), it.position.toString()))
        }
    }

    fun onRateClick(v: View) {
        post?.let {
            eventBus.postDefault(RateEvent(it.tid.toString(), it.pid.toString()))
        }
    }

    fun onEditClick(v: View) {
        val p = post
        val t = thread
        if (p != null && t != null) {
            eventBus.postDefault(EditAppPostEvent(p, t))
        }
    }

    fun onTradeHtmlClick(v: View) {
        post?.let {
            val url = String.format(
                "%sforum.php?mod=viewthread&do=tradeinfo&tid=%s&pid=%s",
                Api.BASE_URL,
                it.tid,
                it.pid + 1
            )
            WebViewActivity.start(v.context, url, true, true)
        }
    }
}
