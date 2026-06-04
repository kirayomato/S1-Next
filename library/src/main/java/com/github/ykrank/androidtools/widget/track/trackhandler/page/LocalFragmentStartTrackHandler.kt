package com.github.ykrank.androidtools.widget.track.trackhandler.page

import com.github.ykrank.androidtools.util.ContextUtils
import com.github.ykrank.androidtools.widget.track.TrackAgent
import com.github.ykrank.androidtools.widget.track.event.page.LocalFragmentStartEvent
import com.github.ykrank.androidtools.widget.track.trackhandler.ContextTrackHandlerImp

class LocalFragmentStartTrackHandler(agent: TrackAgent) : ContextTrackHandlerImp<LocalFragmentStartEvent>(agent) {
    override fun trackEvent(event: LocalFragmentStartEvent): Boolean {
        val fragment = event.fragment!!
        agent.onPageStart(fragment.activity!!, ContextUtils.getLocalClassName(fragment))
        return true
    }
}
