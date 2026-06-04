package com.github.ykrank.androidtools.widget.track.trackhandler.page

import com.github.ykrank.androidtools.widget.track.TrackAgent
import com.github.ykrank.androidtools.widget.track.event.page.ActivityStartEvent
import com.github.ykrank.androidtools.widget.track.trackhandler.ContextTrackHandlerImp

class ActivityStartTrackHandler(agent: TrackAgent) : ContextTrackHandlerImp<ActivityStartEvent>(agent) {
    override fun trackEvent(event: ActivityStartEvent): Boolean {
        agent.onResume(event.activity!!)
        return true
    }
}
