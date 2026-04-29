package me.ykrank.s1next.data.pref

import android.content.Context
import android.content.SharedPreferences
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ykrank.androidtools.data.BasePreferences
import com.github.ykrank.androidtools.data.PreferenceDelegates
import me.ykrank.s1next.R
import me.ykrank.s1next.data.api.model.ForumUploadConfig

/**
 * A helper class retrieving the app api preferences from [SharedPreferences].
 */
class AppDataPreferencesImpl(context: Context, sharedPreferences: SharedPreferences,
                             private val objectMapper: ObjectMapper)
    : BasePreferences(context, sharedPreferences), AppDataPreferences {

    override var appToken: String? by PreferenceDelegates.string(
            mContext.getString(R.string.pref_key_app_token), "")

    override fun getForumUploadConfigJson(uid: String?): String? {
        if (uid.isNullOrEmpty()) return null
        val key = mContext.getString(R.string.pref_key_forum_upload_config) + "_" + uid
        return preferences.getString(key, null)
    }

    override fun setForumUploadConfigJson(uid: String?, json: String?) {
        if (uid.isNullOrEmpty()) return
        val key = mContext.getString(R.string.pref_key_forum_upload_config) + "_" + uid
        val editor = preferences.edit()
        if (json.isNullOrEmpty()) {
            editor.remove(key)
        } else {
            editor.putString(key, json)
        }
        editor.apply()
    }
}

interface AppDataPreferences {
    var appToken: String?
    fun getForumUploadConfigJson(uid: String?): String?
    fun setForumUploadConfigJson(uid: String?, json: String?)
}

class AppDataPreferencesManager(private val mPreferencesProvider: AppDataPreferences) : AppDataPreferences by mPreferencesProvider