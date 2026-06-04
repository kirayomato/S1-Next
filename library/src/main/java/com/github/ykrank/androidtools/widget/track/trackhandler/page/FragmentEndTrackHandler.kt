package com.github.ykrank.androidtools.widget.track.trackhandler.page

import com.github.ykrank.androidtools.util.ContextUtils
import com.github.ykrank.androidtools.widget.track.TrackAgent
import com.github.ykrank.androidtools.widget.track.event.page.FragmentEndEvent
import com.github.ykrank.androidtools.widget.track.trackhandler.ContextTrackHandlerImp

class FragmentEndTrackHandler(agent: TrackAgent) : ContextTrackHandlerImp<FragmentEndEvent>(agent) {
    override fun trackEvent(event: FragmentEndEvent): Boolean {
        val fragment = event.fragment!!
        agent.onPageEnd(fragment.context!!, ContextUtils.getLocalClassName(fragment))
        return true
    }
}
