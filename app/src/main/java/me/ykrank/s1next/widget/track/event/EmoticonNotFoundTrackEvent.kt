package me.ykrank.s1next.widget.track.event

import com.github.ykrank.androidtools.widget.track.event.TrackEvent

class EmoticonNotFoundTrackEvent(uri: String?) : TrackEvent() {
    init {
        group = "未知表情"
        name = uri
    }
}
