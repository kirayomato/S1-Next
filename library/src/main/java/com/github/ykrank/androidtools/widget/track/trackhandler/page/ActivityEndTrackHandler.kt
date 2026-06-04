package com.github.ykrank.androidtools.widget.track.trackhandler.page

import com.github.ykrank.androidtools.widget.track.TrackAgent
import com.github.ykrank.androidtools.widget.track.event.page.ActivityEndEvent
import com.github.ykrank.androidtools.widget.track.trackhandler.ContextTrackHandlerImp

class ActivityEndTrackHandler(agent: TrackAgent) : ContextTrackHandlerImp<ActivityEndEvent>(agent) {
    override fun trackEvent(event: ActivityEndEvent): Boolean {
        agent.onPause(event.activity!!)
        return true
    }
}
