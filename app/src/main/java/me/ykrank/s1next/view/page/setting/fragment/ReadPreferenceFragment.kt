package me.ykrank.s1next.view.page.setting.fragment

import android.content.SharedPreferences
import android.os.Bundle
import com.github.ykrank.androidtools.extension.toast
import me.ykrank.s1next.R

/**
 * An Activity includes download settings that allow users
 * to modify download features and behaviors such as cache
 * size and avatars/images download strategy.
 */
class ReadPreferenceFragment : BasePreferenceFragment() {

    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.preference_read)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == getString(R.string.pref_key_thread_padding)) {
            val threadPadding = sharedPreferences?.getString(key, null)?.toIntOrNull()
            if (threadPadding == null || threadPadding <= 0) {
                activity?.toast(R.string.format_error)
            }
        }
    }

    companion object {
        val TAG = ReadPreferenceFragment::class.java.simpleName
    }


}
