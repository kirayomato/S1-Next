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
import me.ykrank.s1next.PreAppComponent
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
    fun providePreAppComponent(@ApplicationContext context: Context): PreAppComponent {
        check(context.applicationContext is App)
        return App.preAppComponent
    }

    @Provides
    @Singleton
    fun provideContext(preAppComponent: PreAppComponent): Context {
        return preAppComponent.context
    }

    @Provides
    @Singleton
    fun provideWifi(preAppComponent: PreAppComponent): Wifi {
        return preAppComponent.wifi
    }

    @Provides
    @Singleton
    fun provideJsonMapper(preAppComponent: PreAppComponent): ObjectMapper {
        return preAppComponent.jsonMapper
    }

    @Provides
    @Singleton
    fun provideCookieManager(preAppComponent: PreAppComponent): CookieManager {
        return preAppComponent.cookieManager
    }

    @Provides
    @Singleton
    fun provideCookieJar(preAppComponent: PreAppComponent): CookieJar {
        return preAppComponent.cookieJar
    }

    @Provides
    @Singleton
    fun provideEventBus(preAppComponent: PreAppComponent): EventBus {
        return preAppComponent.eventBus
    }

    @Provides
    @Singleton
    fun provideDataTrackAgent(preAppComponent: PreAppComponent): DataTrackAgent {
        return preAppComponent.dataTrackAgent
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(preAppComponent: PreAppComponent): SharedPreferences {
        return preAppComponent.sharedPreferences
    }

    @Provides
    @Singleton
    fun provideNetworkPreferencesManager(preAppComponent: PreAppComponent): NetworkPreferencesManager {
        return preAppComponent.networkPreferencesManager
    }

    @Provides
    @Singleton
    fun provideGeneralPreferencesManager(preAppComponent: PreAppComponent): GeneralPreferencesManager {
        return preAppComponent.generalPreferencesManager
    }

    @Provides
    @Singleton
    fun provideThemeManager(preAppComponent: PreAppComponent): ThemeManager {
        return preAppComponent.themeManager
    }

    @Provides
    @Singleton
    fun provideDownloadPreferencesManager(preAppComponent: PreAppComponent): DownloadPreferencesManager {
        return preAppComponent.downloadPreferencesManager
    }

    @Provides
    @Singleton
    fun provideReadPreferencesManager(preAppComponent: PreAppComponent): ReadPreferencesManager {
        return preAppComponent.readProgressPreferencesManager
    }

    @Provides
    @Singleton
    fun provideDataPreferencesManager(preAppComponent: PreAppComponent): DataPreferencesManager {
        return preAppComponent.dataPreferencesManager
    }

    @Provides
    @Singleton
    fun provideAppDataPreferencesManager(preAppComponent: PreAppComponent): AppDataPreferencesManager {
        return preAppComponent.appDataPreferencesManager
    }
}
