package com.github.ykrank.androidtools.widget.track.event

class ThemeChangeTrackEvent(fromAvatar: Boolean) : TrackEvent() {
    init {
        group = "切换主题"
        name = if (fromAvatar) {
            "点击头像"
        } else {
            "设置中切换"
        }
    }
}
