package me.ykrank.s1next

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ykrank.androidtools.widget.EventBus
import com.github.ykrank.androidtools.widget.PersistentHttpCookieStore
import com.github.ykrank.androidtools.widget.track.DataTrackAgent
import me.ykrank.s1next.data.Wifi
import me.ykrank.s1next.data.pref.AppDataPreferences
import me.ykrank.s1next.data.pref.AppDataPreferencesImpl
import me.ykrank.s1next.data.pref.AppDataPreferencesManager
import me.ykrank.s1next.data.pref.DataPreferences
import me.ykrank.s1next.data.pref.DataPreferencesImpl
import me.ykrank.s1next.data.pref.DataPreferencesManager
import me.ykrank.s1next.data.pref.DownloadPreferences
import me.ykrank.s1next.data.pref.DownloadPreferencesImpl
import me.ykrank.s1next.data.pref.DownloadPreferencesManager
import me.ykrank.s1next.data.pref.GeneralPreferences
import me.ykrank.s1next.data.pref.GeneralPreferencesImpl
import me.ykrank.s1next.data.pref.GeneralPreferencesManager
import me.ykrank.s1next.data.pref.NetworkPreferences
import me.ykrank.s1next.data.pref.NetworkPreferencesImpl
import me.ykrank.s1next.data.pref.NetworkPreferencesManager
import me.ykrank.s1next.data.pref.ReadPreferences
import me.ykrank.s1next.data.pref.ReadPreferencesImpl
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.data.pref.ThemeManager
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import java.net.CookieManager
import java.net.CookiePolicy

/**
 * Hand-written early graph for objects needed before normal app initialization.
 */
class PreAppGraph(
    app: App,
    private val prefContext: Context,
) : PreAppComponent {
    override val context: Context = app

    override val wifi: Wifi by lazy { Wifi() }

    override val jsonMapper: ObjectMapper by lazy {
        ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
            .configure(JsonParser.Feature.INCLUDE_SOURCE_IN_LOCATION, BuildConfig.DEBUG)
    }

    override val cookieManager: CookieManager by lazy {
        CookieManager(PersistentHttpCookieStore(context), CookiePolicy.ACCEPT_ALL)
    }

    override val cookieJar: CookieJar by lazy {
        JavaNetCookieJar(cookieManager)
    }

    override val eventBus: EventBus by lazy { EventBus() }

    override val dataTrackAgent: DataTrackAgent by lazy { DataTrackAgent() }

    override val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(prefContext)
    }

    private val networkPreferences: NetworkPreferences by lazy {
        NetworkPreferencesImpl(prefContext, sharedPreferences)
    }

    override val networkPreferencesManager: NetworkPreferencesManager by lazy {
        NetworkPreferencesManager(networkPreferences)
    }

    private val generalPreferences: GeneralPreferences by lazy {
        GeneralPreferencesImpl(prefContext, sharedPreferences)
    }

    override val generalPreferencesManager: GeneralPreferencesManager by lazy {
        GeneralPreferencesManager(generalPreferences)
    }

    override val themeManager: ThemeManager by lazy {
        ThemeManager(prefContext, generalPreferences)
    }

    private val downloadPreferences: DownloadPreferences by lazy {
        DownloadPreferencesImpl(prefContext, sharedPreferences)
    }

    override val downloadPreferencesManager: DownloadPreferencesManager by lazy {
        DownloadPreferencesManager(downloadPreferences, wifi)
    }

    private val readPreferences: ReadPreferences by lazy {
        ReadPreferencesImpl(prefContext, sharedPreferences, jsonMapper)
    }

    override val readProgressPreferencesManager: ReadPreferencesManager by lazy {
        ReadPreferencesManager(readPreferences)
    }

    private val dataPreferences: DataPreferences by lazy {
        DataPreferencesImpl(prefContext, sharedPreferences)
    }

    override val dataPreferencesManager: DataPreferencesManager by lazy {
        DataPreferencesManager(dataPreferences)
    }

    private val appDataPreferences: AppDataPreferences by lazy {
        AppDataPreferencesImpl(prefContext, sharedPreferences)
    }

    override val appDataPreferencesManager: AppDataPreferencesManager by lazy {
        AppDataPreferencesManager(appDataPreferences)
    }
}
