package me.ykrank.s1next.widget.track.event

import com.github.ykrank.androidtools.widget.track.event.TrackEvent

class ViewImageTrackEvent(url: String?, fromAvatar: Boolean) : TrackEvent() {
    init {
        group = "图片浏览"
        name = if (fromAvatar) {
            "头像"
        } else {
            "帖子中图片"
        }
        addData("url", url)
    }
}
