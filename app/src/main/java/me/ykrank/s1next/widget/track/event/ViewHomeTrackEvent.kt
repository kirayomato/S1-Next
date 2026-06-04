package me.ykrank.s1next.widget.track.event

import com.github.ykrank.androidtools.widget.track.event.TrackEvent

class ViewHomeTrackEvent(id: String?, name: String?) : TrackEvent() {
    init {
        group = "浏览个人主页"
        addData("id", id)
        addData("name", name)
    }
}
