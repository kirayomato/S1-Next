package me.ykrank.s1next.widget.track.event

import com.github.ykrank.androidtools.widget.track.event.TrackEvent

class BlackListTrackEvent(isAdd: Boolean, authorId: String?, authorName: String?) : TrackEvent() {
    init {
        group = "黑名单"
        name = if (isAdd) {
            "添加黑名单"
        } else {
            "删除黑名单"
        }
        addData("id", authorId)
        addData("name", authorName)
    }
}
