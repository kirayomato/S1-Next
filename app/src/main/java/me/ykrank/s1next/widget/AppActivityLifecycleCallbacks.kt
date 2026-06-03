package me.ykrank.s1next.widget

import android.app.Activity
import com.github.ykrank.androidtools.widget.net.WifiActivityLifecycleCallbacks
import me.ykrank.s1next.data.Wifi
import me.ykrank.s1next.widget.hostcheck.NoticeCheckTask

class AppActivityLifecycleCallbacks(
    private val noticeCheckTask: NoticeCheckTask,
    private val wifi: Wifi
) :
    WifiActivityLifecycleCallbacks() {

    override val wifiStateChangedCallback: ((Boolean) -> Unit)?
        get() = { wifi.isWifiEnabled = it }


    override fun onActivityResumed(activity: Activity) {
        super.onActivityResumed(activity)
        noticeCheckTask.inspectCheckNoticeTask()
    }
}
