package me.ykrank.s1next.view.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import dagger.hilt.android.AndroidEntryPoint
import me.ykrank.s1next.R
import me.ykrank.s1next.data.db.AppDatabaseManager
import me.ykrank.s1next.databinding.DialogVersionInfoBinding
import me.ykrank.s1next.util.AppDeviceUtil
import javax.inject.Inject

/**
 * A dialog shows version info.
 */
@AndroidEntryPoint
class VersionInfoDialogFragment : BaseDialogFragment() {
    @Inject
    internal lateinit var appDatabaseManager: AppDatabaseManager

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogVersionInfoBinding.inflate(requireActivity().layoutInflater, null, false)

        binding.version.text = getString(
            R.string.version,
            AppDeviceUtil.getVersionName(),
            appDatabaseManager.version.toString()
        )
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    companion object {
        val TAG: String = VersionInfoDialogFragment::class.java.simpleName
    }
}
