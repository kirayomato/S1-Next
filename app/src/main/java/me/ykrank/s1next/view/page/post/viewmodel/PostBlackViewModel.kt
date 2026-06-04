package me.ykrank.s1next.view.page.post.viewmodel

import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.LifecycleOwner
import com.github.ykrank.androidtools.util.ContextUtils
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.model.Post
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.api.model.Vote
import me.ykrank.s1next.view.activity.UserHomeActivity
import me.ykrank.s1next.view.internal.BlacklistMenuAction

class PostBlackViewModel(val lifecycleOwner: LifecycleOwner, private val eventBus: EventBus) {

    var post: Post? = null
    var thread: Thread? = null
    var vote: Vote? = null
    var pageNum: Int = 0

    val floor: CharSequence?
        get() {
            val p = post ?: return null
            return "#${p.number}"
        }

    fun onAvatarClick(v: View) {
        post?.let {
            val authorId = it.authorId
            val authorName = it.authorName
            if (authorId != null && authorName != null) {
                //个人主页
                UserHomeActivity.start(
                    v.context,
                    authorId,
                    authorName,
                    v
                )
            }
        }
    }

    fun onAvatarLongClick(v: View): Boolean {
        return showBlackListMenu(v)
    }

    fun onFloorClick(v: View) {
        showBlackListMenu(v)
    }

    fun showBlackListMenu(v: View): Boolean {
        //长按显示抹布菜单
        val popup = PopupMenu(v.context, v)
        val postData = post
        popup.setOnMenuItemClickListener { menuitem: MenuItem ->
            when (menuitem.itemId) {
                R.id.menu_popup_blacklist -> {
                    val authorId = postData?.authorId
                    if (!authorId.isNullOrBlank()) {
                        val authorIdInt = authorId!!.toInt()
                        val authorName = postData.authorName
                        if (authorName != null) {
                            if (menuitem.title == v.context.getString(R.string.menu_blacklist_remove)) {
                                BlacklistMenuAction.removeBlacklist(lifecycleOwner, eventBus, authorIdInt, authorName)
                            } else {
                                val activity = ContextUtils.findFragmentActivity(v.context)
                                if (activity != null) {
                                    BlacklistMenuAction.addBlacklist(
                                        activity,
                                        authorIdInt,
                                        authorName
                                    )
                                } else {
                                    L.report(IllegalStateException("抹布时头像Context不为FragmentActivity${v.context}"))
                                }
                            }
                        }
                    }

                    return@setOnMenuItemClickListener true
                }

                else -> return@setOnMenuItemClickListener false
            }
        }
        popup.inflate(R.menu.popup_blacklist)
        if (postData?.hide == Post.HIDE_USER) {
            popup.menu.findItem(R.id.menu_popup_blacklist).setTitle(R.string.menu_blacklist_remove)
        }
        popup.show()
        return true
    }


}
