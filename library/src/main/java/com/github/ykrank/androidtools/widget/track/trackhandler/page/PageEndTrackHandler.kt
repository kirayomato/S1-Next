package com.github.ykrank.androidtools.widget.track.trackhandler.page

import com.github.ykrank.androidtools.widget.track.TrackAgent
import com.github.ykrank.androidtools.widget.track.event.page.PageEndEvent
import com.github.ykrank.androidtools.widget.track.trackhandler.ContextTrackHandlerImp

class PageEndTrackHandler(agent: TrackAgent) : ContextTrackHandlerImp<PageEndEvent>(agent) {
    override fun trackEvent(event: PageEndEvent): Boolean {
        agent.onPageEnd(event.context!!, event.pageName!!)
        return true
    }
}
