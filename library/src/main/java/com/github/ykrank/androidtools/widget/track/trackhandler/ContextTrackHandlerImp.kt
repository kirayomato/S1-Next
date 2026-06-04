package com.github.ykrank.androidtools.widget.track.trackhandler

import android.os.Handler
import androidx.annotation.UiThread
import com.github.ykrank.androidtools.widget.track.TrackAgent

abstract class ContextTrackHandlerImp<T> protected constructor(
    protected val agent: TrackAgent
) : TrackHandler<T> {

    final override fun track(handler: Handler, eventType: T) {
        trackEvent(eventType)
    }

    /**
     * event handle action. run on UI thread.
     *
     * @param event Event
     * @return does action success
     */
    @UiThread
    abstract fun trackEvent(event: T): Boolean
}
