package me.ykrank.s1next.widget.track.event

import com.github.ykrank.androidtools.widget.track.event.TrackEvent

class SearchTrackEvent(query: String?) : TrackEvent() {
    init {
        group = "搜索"
        addData("query", query)
    }
}
