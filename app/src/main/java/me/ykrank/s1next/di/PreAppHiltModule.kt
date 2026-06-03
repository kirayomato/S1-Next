package me.ykrank.s1next.di

import android.content.Context
import android.content.SharedPreferences
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ykrank.androidtools.widget.EventBus
import com.github.ykrank.androidtools.widget.track.DataTrackAgent
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.ykrank.s1next.App
import me.ykrank.s1next.PreAppGraph
import me.ykrank.s1next.data.Wifi
import me.ykrank.s1next.data.pref.AppDataPreferencesManager
import me.ykrank.s1next.data.pref.DataPreferencesManager
import me.ykrank.s1next.data.pref.DownloadPreferencesManager
import me.ykrank.s1next.data.pref.GeneralPreferencesManager
import me.ykrank.s1next.data.pref.NetworkPreferencesManager
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.data.pref.ThemeManager
import okhttp3.CookieJar
import java.net.CookieManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PreAppHiltModule {
    @Provides
    @Singleton
    fun providePreAppGraph(@ApplicationContext context: Context): PreAppGraph {
        check(context.applicationContext is App)
        return App.preAppGraph
    }

    @Provides
    @Singleton
    fun provideContext(preAppGraph: PreAppGraph): Context {
        return preAppGraph.context
    }

    @Provides
    @Singleton
    fun provideWifi(preAppGraph: PreAppGraph): Wifi {
        return preAppGraph.wifi
    }

    @Provides
    @Singleton
    fun provideJsonMapper(preAppGraph: PreAppGraph): ObjectMapper {
        return preAppGraph.jsonMapper
    }

    @Provides
    @Singleton
    fun provideCookieManager(preAppGraph: PreAppGraph): CookieManager {
        return preAppGraph.cookieManager
    }

    @Provides
    @Singleton
    fun provideCookieJar(preAppGraph: PreAppGraph): CookieJar {
        return preAppGraph.cookieJar
    }

    @Provides
    @Singleton
    fun provideEventBus(preAppGraph: PreAppGraph): EventBus {
        return preAppGraph.eventBus
    }

    @Provides
    @Singleton
    fun provideDataTrackAgent(preAppGraph: PreAppGraph): DataTrackAgent {
        return preAppGraph.dataTrackAgent
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(preAppGraph: PreAppGraph): SharedPreferences {
        return preAppGraph.sharedPreferences
    }

    @Provides
    @Singleton
    fun provideNetworkPreferencesManager(preAppGraph: PreAppGraph): NetworkPreferencesManager {
        return preAppGraph.networkPreferencesManager
    }

    @Provides
    @Singleton
    fun provideGeneralPreferencesManager(preAppGraph: PreAppGraph): GeneralPreferencesManager {
        return preAppGraph.generalPreferencesManager
    }

    @Provides
    @Singleton
    fun provideThemeManager(preAppGraph: PreAppGraph): ThemeManager {
        return preAppGraph.themeManager
    }

    @Provides
    @Singleton
    fun provideDownloadPreferencesManager(preAppGraph: PreAppGraph): DownloadPreferencesManager {
        return preAppGraph.downloadPreferencesManager
    }

    @Provides
    @Singleton
    fun provideReadPreferencesManager(preAppGraph: PreAppGraph): ReadPreferencesManager {
        return preAppGraph.readProgressPreferencesManager
    }

    @Provides
    @Singleton
    fun provideDataPreferencesManager(preAppGraph: PreAppGraph): DataPreferencesManager {
        return preAppGraph.dataPreferencesManager
    }

    @Provides
    @Singleton
    fun provideAppDataPreferencesManager(preAppGraph: PreAppGraph): AppDataPreferencesManager {
        return preAppGraph.appDataPreferencesManager
    }
}
