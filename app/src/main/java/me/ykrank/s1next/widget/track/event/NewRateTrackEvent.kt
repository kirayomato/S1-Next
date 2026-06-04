package me.ykrank.s1next.widget.track.event

import com.github.ykrank.androidtools.widget.track.event.TrackEvent

class NewRateTrackEvent(
    threadId: String?,
    quotePostId: String?,
    score: String?,
    reason: String?,
) : TrackEvent() {
    init {
        group = "新评分"
        addData("ThreadId", threadId)
        addData("QuotePostId", quotePostId)
        addData("Score", score)
        addData("Reason", reason)
    }
}
