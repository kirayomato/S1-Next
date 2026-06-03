package me.ykrank.s1next.widget.glide

import com.github.ykrank.androidtools.widget.track.DataTrackAgent
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.ykrank.s1next.data.pref.DownloadPreferencesManager
import me.ykrank.s1next.widget.net.Image
import okhttp3.OkHttpClient

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GlideDependenciesEntryPoint {
    @get:Image
    val imageOkHttpClient: OkHttpClient
    val downloadPreferencesManager: DownloadPreferencesManager
    val avatarFailUrlsCache: AvatarFailUrlsCache
    val dataTrackAgent: DataTrackAgent
}
