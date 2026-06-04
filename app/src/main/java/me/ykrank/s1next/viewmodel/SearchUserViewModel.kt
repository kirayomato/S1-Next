package me.ykrank.s1next.viewmodel

import android.view.View
import me.ykrank.s1next.data.api.model.search.UserSearchResult
import me.ykrank.s1next.view.activity.UserHomeActivity.Companion.start

class SearchUserViewModel {
    var search: UserSearchResult? = null

    fun onClick(v: View, avatarView: View) {
        //个人主页
        search?.uid?.apply {
            start(
                v.context,
                this,
                search?.name,
                avatarView
            )
        }
    }
}
