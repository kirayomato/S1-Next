package me.ykrank.s1next.widget.track.event

import com.github.ykrank.androidtools.widget.track.event.TrackEvent

class NewReplyTrackEvent(threadId: String?, quotePostId: String?) : TrackEvent() {
    init {
        group = "新回复"
        addData("ThreadId", threadId)
        addData("QuotePostId", quotePostId)
    }
}
