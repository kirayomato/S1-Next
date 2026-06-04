package com.github.ykrank.androidtools.widget.track.trackhandler.page

import com.github.ykrank.androidtools.util.ContextUtils
import com.github.ykrank.androidtools.widget.track.TrackAgent
import com.github.ykrank.androidtools.widget.track.event.page.FragmentStartEvent
import com.github.ykrank.androidtools.widget.track.trackhandler.ContextTrackHandlerImp

class FragmentStartTrackHandler(agent: TrackAgent) : ContextTrackHandlerImp<FragmentStartEvent>(agent) {
    override fun trackEvent(event: FragmentStartEvent): Boolean {
        val fragment = event.fragment!!
        agent.onPageStart(fragment.context!!, ContextUtils.getLocalClassName(fragment))
        return true
    }
}
