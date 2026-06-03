package me.ykrank.s1next.view.activity

import com.github.ykrank.androidtools.widget.EventBus
import com.github.ykrank.androidtools.widget.track.DataTrackAgent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.ykrank.s1next.data.pref.DataPreferencesManager
import me.ykrank.s1next.data.pref.DownloadPreferencesManager
import me.ykrank.s1next.data.pref.GeneralPreferencesManager
import me.ykrank.s1next.data.pref.ThemeManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BaseActivityDependenciesEntryPoint {
    val eventBus: EventBus
    val generalPreferencesManager: GeneralPreferencesManager
    val downloadPreferencesManager: DownloadPreferencesManager
    val dataPreferencesManager: DataPreferencesManager
    val themeManager: ThemeManager
    val dataTrackAgent: DataTrackAgent
}
