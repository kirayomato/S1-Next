package com.github.ykrank.androidtools.widget.track.talkingdata

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

import com.github.ykrank.androidtools.data.TrackUser
import com.github.ykrank.androidtools.widget.track.TrackAgent
import com.tendcloud.tenddata.TalkingDataSDK

/**
 * Created by ykrank on 2016/12/28.
 * Agent for talking data proxy
 */

class TalkingDataAgent : TrackAgent {

    override fun init(context: Context) {
        val appContext = context.applicationContext
        TalkingDataSDK.setVerboseLogDisable()
        TalkingDataSDK.setReportUncaughtExceptions(false)
        TalkingDataSDK.initSDK(
            appContext,
            appContext.getMetaData(TD_APP_ID),
            appContext.getMetaData(TD_CHANNEL_ID),
            ""
        )
        TalkingDataSDK.startA(appContext)
    }

    override fun setUser(user: TrackUser) {
        TalkingDataSDK.setGlobalKV("UserName", user.name)
        TalkingDataSDK.setGlobalKV("Uid", user.uid)
        TalkingDataSDK.setGlobalKV("Permission", user.permission.toString())
        user.extras?.forEach { TalkingDataSDK.setGlobalKV(it.key, it.value) }
    }

    override fun onResume(activity: Activity) {
        TalkingDataSDK.onPageBegin(activity, activity.localClassName)
    }

    override fun onPause(activity: Activity) {
        TalkingDataSDK.onPageEnd(activity, activity.localClassName)
    }

    override fun onPageStart(context: Context, string: String) {
        TalkingDataSDK.onPageBegin(context, string)
    }

    override fun onPageEnd(context: Context, string: String) {
        TalkingDataSDK.onPageEnd(context, string)
    }

    override fun onEvent(context: Context, name: String, label: String, data: Map<String, String?>) {
        val params = LinkedHashMap<String, Any>()
        data.forEach { (key, value) ->
            if (value != null) {
                params[key] = value
            }
        }
        if (label.isNotEmpty() && !params.containsKey(EVENT_LABEL_KEY)) {
            params[EVENT_LABEL_KEY] = label
        }
        TalkingDataSDK.onEvent(context, name, params)
    }

    private fun Context.getMetaData(name: String): String {
        val appInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getApplicationInfo(
                    packageName,
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            }
        }.getOrNull()
        return appInfo?.metaData?.getString(name).orEmpty()
    }

    companion object {
        private const val TD_APP_ID = "TD_APP_ID"
        private const val TD_CHANNEL_ID = "TD_CHANNEL_ID"
        private const val EVENT_LABEL_KEY = "Label"
    }
}
