package com.github.ykrank.androidtools.widget.track.trackhandler.page

import com.github.ykrank.androidtools.util.ContextUtils
import com.github.ykrank.androidtools.widget.track.TrackAgent
import com.github.ykrank.androidtools.widget.track.event.page.LocalFragmentEndEvent
import com.github.ykrank.androidtools.widget.track.trackhandler.ContextTrackHandlerImp

class LocalFragmentEndTrackHandler(agent: TrackAgent) : ContextTrackHandlerImp<LocalFragmentEndEvent>(agent) {
    override fun trackEvent(event: LocalFragmentEndEvent): Boolean {
        val fragment = event.fragment!!
        agent.onPageEnd(fragment.activity!!, ContextUtils.getLocalClassName(fragment))
        return true
    }
}
