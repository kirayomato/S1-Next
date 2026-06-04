package com.github.ykrank.androidtools.widget.track.trackhandler.page

import com.github.ykrank.androidtools.widget.track.TrackAgent
import com.github.ykrank.androidtools.widget.track.event.page.PageStartEvent
import com.github.ykrank.androidtools.widget.track.trackhandler.ContextTrackHandlerImp

class PageStartTrackHandler(agent: TrackAgent) : ContextTrackHandlerImp<PageStartEvent>(agent) {
    override fun trackEvent(event: PageStartEvent): Boolean {
        agent.onPageStart(event.context!!, event.pageName!!)
        return true
    }
}
