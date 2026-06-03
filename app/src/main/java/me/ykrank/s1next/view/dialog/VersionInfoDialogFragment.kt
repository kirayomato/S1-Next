package me.ykrank.s1next.view.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import me.ykrank.s1next.R
import me.ykrank.s1next.data.db.AppDatabaseManager
import me.ykrank.s1next.databinding.DialogVersionInfoBinding
import javax.inject.Inject

/**
 * A dialog shows version info.
 */
@AndroidEntryPoint
class VersionInfoDialogFragment : BaseDialogFragment() {
    @Inject
    internal lateinit var appDatabaseManager: AppDatabaseManager

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DataBindingUtil.inflate<DialogVersionInfoBinding>(
            requireActivity().layoutInflater,
            R.layout.dialog_version_info, null, false
        )

        binding.dbVersion = appDatabaseManager.version.toString()
        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()
    }

    companion object {
        val TAG: String = VersionInfoDialogFragment::class.java.simpleName
    }
}
