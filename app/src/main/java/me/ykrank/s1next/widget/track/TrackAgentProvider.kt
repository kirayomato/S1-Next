package me.ykrank.s1next.widget.track

import android.content.Context
import com.github.ykrank.androidtools.widget.track.DataTrackAgent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import me.ykrank.s1next.App

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TrackAgentEntryPoint {
    val dataTrackAgent: DataTrackAgent
}

object TrackAgentProvider {
    fun get(context: Context = App.get()): DataTrackAgent {
        return EntryPointAccessors.fromApplication(
            context.applicationContext,
            TrackAgentEntryPoint::class.java
        ).dataTrackAgent
    }
}
