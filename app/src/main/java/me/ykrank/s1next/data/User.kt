package me.ykrank.s1next.data

import android.text.TextUtils
import com.fasterxml.jackson.core.JsonProcessingException
import com.github.ykrank.androidtools.data.TrackUser
import com.github.ykrank.androidtools.util.L
import me.ykrank.s1next.App
import me.ykrank.s1next.data.api.model.ForumUploadConfig
import me.ykrank.s1next.data.pref.AppDataPreferencesManager

open class User(private val appDataPref: AppDataPreferencesManager) : TrackUser {

    @Volatile
    override var uid: String? = null

    @Volatile
    override var name: String? = null

    @Volatile
    override var permission: Int = 0

    override val extras: Map<String, String> = hashMapOf()

    @Volatile
    var authenticityToken: String? = null

    var forumUploadConfig: ForumUploadConfig?
        get() {
            val json = appDataPref.getForumUploadConfigJson(uid)
            if (json.isNullOrEmpty()) return null
            return try {
                App.preAppComponent.jsonMapper.readValue(json, ForumUploadConfig::class.java)
            } catch (e: JsonProcessingException) {
                L.report(e)
                null
            }
        }
        set(value) {
            try {
                val json = if (value == null) null else App.preAppComponent.jsonMapper.writeValueAsString(value)
                appDataPref.setForumUploadConfigJson(uid, json)
            } catch (e: JsonProcessingException) {
                L.report(e)
            }
        }

    var appSecureToken: String?
        get() = appDataPref.appToken
        set(value) {
            appDataPref.appToken = value
        }

    @Volatile
    open var isLogged: Boolean = false

    @Volatile
    open var isSigned: Boolean = false

    val isAppLogged: Boolean
        get() = !TextUtils.isEmpty(appSecureToken)

    val key: String
        get() {
            if (!TextUtils.isEmpty(uid)) {
                return uid!!
            }
            return "anonymous"
        }

    val isDebugUser: Boolean
        get() = uid == "223963"
}
