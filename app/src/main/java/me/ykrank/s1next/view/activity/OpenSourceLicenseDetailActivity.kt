package me.ykrank.s1next.view.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.databinding.ActivityOpenSourceLicenseDetailBinding

/**
 * An Activity shows the open source license for corresponding library or file.
 */
class OpenSourceLicenseDetailActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityOpenSourceLicenseDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val libraryOrFileName = intent.getStringExtra(EXTRA_LIBRARY_OR_FILE_NAME)
        title = libraryOrFileName

        TextViewBindingAdapter.loadTextAsset(
            binding.licenseText,
            intent.getStringExtra(EXTRA_LICENSE_FILE_PATH)
        )
    }

    companion object {
        @JvmField
        val TAG: String = OpenSourceLicenseDetailActivity::class.java.name

        private const val EXTRA_LIBRARY_OR_FILE_NAME = "library_or_file_name"
        private const val EXTRA_LICENSE_FILE_PATH = "license_file_path"

        @JvmStatic
        fun startOpenSourceLicenseDetailActivity(
            context: Context,
            libraryOrFileName: String?,
            licenseFilePath: String?
        ) {
            val intent = Intent(context, OpenSourceLicenseDetailActivity::class.java).apply {
                putExtra(EXTRA_LIBRARY_OR_FILE_NAME, libraryOrFileName)
                putExtra(EXTRA_LICENSE_FILE_PATH, licenseFilePath)
            }
            context.startActivity(intent)
        }
    }
}
