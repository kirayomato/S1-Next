package me.ykrank.s1next.widget.track.event

import com.github.ykrank.androidtools.widget.track.event.TrackEvent

class ViewUserReplyTrackEvent(id: String?, name: String?) : TrackEvent() {
    init {
        group = "浏览个人回复列表"
        addData("id", id)
        addData("name", name)
    }
}
