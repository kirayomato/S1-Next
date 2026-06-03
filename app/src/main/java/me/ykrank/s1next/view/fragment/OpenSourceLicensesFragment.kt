package me.ykrank.s1next.view.fragment

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragment
import androidx.preference.PreferenceScreen
import me.ykrank.s1next.R
import me.ykrank.s1next.view.activity.OpenSourceLicenseDetailActivity

class OpenSourceLicensesFragment : PreferenceFragment() {

    override fun onCreatePreferences(bundle: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preference_open_souce_licenses)

        val preferenceScreen = preferenceScreen
        setupLibrariesPreference(preferenceScreen)
        setupFilesPreference(preferenceScreen)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        OpenSourceLicenseDetailActivity.startOpenSourceLicenseDetailActivity(
            preference.context,
            preference.title.toString(),
            preference.peekExtras()?.getString(EXTRAS_LIBRARY_OR_FILE_OPEN_SOURCE_LICENSE_FILE_PATH)
        )
        return true
    }

    /**
     * Adds libraries to its PreferenceCategory programmatically.
     */
    private fun setupLibrariesPreference(preferenceScreen: PreferenceScreen) {
        val preferenceCategory = requireNotNull(
            preferenceScreen.findPreference<PreferenceCategory>(getString(R.string.pref_key_libraries))
        )
        val context = preferenceCategory.context
        getLibrariesInfos().forEach { libraryInfos ->
            val preference = Preference(context).apply {
                title = libraryInfos[0]
                isPersistent = false
                extras.putString(
                    EXTRAS_LIBRARY_OR_FILE_OPEN_SOURCE_LICENSE_FILE_PATH,
                    ASSET_PATH_OPEN_SOURCE_LICENSES_LIBRARY + libraryInfos[1]
                )
            }
            preferenceCategory.addPreference(preference)
        }
    }

    /**
     * Adds files to its PreferenceCategory programmatically.
     */
    private fun setupFilesPreference(preferenceScreen: PreferenceScreen) {
        val preferenceCategory = requireNotNull(
            preferenceScreen.findPreference<PreferenceCategory>(getString(R.string.pref_key_files))
        )
        val context = preferenceCategory.context
        getFilesInfo().forEach { fileInfos ->
            val preference = Preference(context).apply {
                title = fileInfos[0]
                isPersistent = false
                extras.putString(
                    EXTRAS_LIBRARY_OR_FILE_OPEN_SOURCE_LICENSE_FILE_PATH,
                    ASSET_PATH_OPEN_SOURCE_LICENSES_FILE + fileInfos[1]
                )
            }
            preferenceCategory.addPreference(preference)
        }
    }

    /**
     * Gets each library's name and its license's name.
     */
    private fun getLibrariesInfos(): Array<Array<String>> {
        return arrayOf(
            arrayOf("AdapterDelegates", "ADAPTER_DELEGATES"),
            arrayOf("Android Support Library", "ANDROID_SUPPORT"),
            arrayOf("Apache Commons Lang", "APACHE_LICENSE_2.0"),
            arrayOf("android-apt", "UNLICENSE"),
            arrayOf("Bugsnag Android", "BUGSNAG_ANDROID"),
            arrayOf("Dagger 2", "DAGGER_2"),
            arrayOf("Data Binding", "ANDROID_SUPPORT"),
            arrayOf("FindBugs-jsr305", "APACHE_LICENSE_2.0"),
            arrayOf("Glide", "GLIDE"),
            arrayOf("Gradle Retrolambda Plugin", "GRADLE_RETROLAMBDA_PLUGIN"),
            arrayOf("Gradle Versions Plugin", "APACHE_LICENSE_2.0"),
            arrayOf("Guava", "APACHE_LICENSE_2.0"),
            arrayOf("jackson-databind", "APACHE_LICENSE_2.0"),
            arrayOf("JSR-250 Common Annotations for the JavaTM Platform", "CDDL_1.0"),
            arrayOf("LeakCanary", "LEAKCANARY"),
            arrayOf("OkHttp", "APACHE_LICENSE_2.0"),
            arrayOf("Retrofit", "RETROFIT"),
            arrayOf("Retrolambda", "APACHE_LICENSE_2.0"),
            arrayOf("RxAndroid", "RX_ANDROID"),
            arrayOf("RxJava", "RX_JAVA"),
            arrayOf("ActiveAndroid", "ACTIVE_ANDROID")
        )
    }

    /**
     * Gets each file's name and its license's name.
     */
    private fun getFilesInfo(): Array<Array<String>> {
        return arrayOf(
            arrayOf("CookieStoreImpl.java", "COOKIE_STORE_IMPL"),
            arrayOf("TagFragmentStatePagerAdapter.java", "FRAGMENT_STATE_PAGER_ADAPTER")
        )
    }

    companion object {
        private const val EXTRAS_LIBRARY_OR_FILE_OPEN_SOURCE_LICENSE_FILE_PATH =
            "libraries_or_files_open_source_license_file_path"

        private const val ASSET_PATH_OPEN_SOURCE_LICENSES_LIBRARY = "text/license/library/"
        private const val ASSET_PATH_OPEN_SOURCE_LICENSES_FILE = "text/license/file/"
    }
}
