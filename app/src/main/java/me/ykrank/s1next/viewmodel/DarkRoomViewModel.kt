package me.ykrank.s1next.viewmodel

import android.view.View

import me.ykrank.s1next.data.api.model.darkroom.DarkRoom
import me.ykrank.s1next.view.activity.UserHomeActivity


class DarkRoomViewModel {

    var darkRoom: DarkRoom? = null

    fun onAvatarClick(v: View) {
        val uid = darkRoom?.uid
        val name = darkRoom?.username

        if (uid != null) {
            UserHomeActivity.start(v.context, uid, name, v)
        }
    }
}
