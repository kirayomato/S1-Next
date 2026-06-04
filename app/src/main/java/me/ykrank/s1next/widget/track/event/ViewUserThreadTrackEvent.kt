package me.ykrank.s1next.widget.track.event

import com.github.ykrank.androidtools.widget.track.event.TrackEvent

class ViewUserThreadTrackEvent(id: String?, name: String?) : TrackEvent() {
    init {
        group = "浏览个人发帖列表"
        addData("id", id)
        addData("name", name)
    }
}
