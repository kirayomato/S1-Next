package me.ykrank.s1next.widget.track.event

import com.github.ykrank.androidtools.widget.track.event.TrackEvent

class ViewUserFriendsTrackEvent(id: String?, name: String?) : TrackEvent() {
    init {
        group = "浏览个人好友列表"
        addData("id", id)
        addData("name", name)
    }
}
