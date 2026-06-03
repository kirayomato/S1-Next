package com.github.ykrank.androidtools.ui.dialog

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.SeekBar
import androidx.annotation.CallSuper
import androidx.appcompat.app.AlertDialog
import com.github.ykrank.androidtools.R
import com.github.ykrank.androidtools.databinding.DialogPageJumpBinding
import com.github.ykrank.androidtools.ui.vm.PageJumpViewModel
import com.github.ykrank.androidtools.util.ViewUtil

/**
 * A dialog shows a [SeekBar] and input field for jumping to a page.
 */
class PageJumpDialogFragment : LibBaseDialogFragment() {
    private lateinit var pageJumpViewModel: PageJumpViewModel

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogPageJumpBinding.inflate(requireActivity().layoutInflater, null, false)

        val seekBarProgress = savedInstanceState?.getInt(STATE_SEEK_BAR_PROGRESS)
            ?: requireArguments().getInt(ARG_CURRENT_PAGE)

        pageJumpViewModel = PageJumpViewModel(
            requireArguments().getInt(ARG_TOTAL_PAGES) - 1,
            seekBarProgress
        )
        bindPageJumpControls(binding)

        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle(R.string.menu_page_jump)
            .setView(binding.root)
            .setPositiveButton(R.string.dialog_button_text_jump) { _, _ ->
                if (!TextUtils.isEmpty(binding.value.text)) {
                    (parentFragment as OnPageJumpedListener).onPageJumped(
                        pageJumpViewModel.seekBarProgress
                    )
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        ViewUtil.consumeRunnableWhenImeActionPerformed(binding.value) {
            alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        }
        return alertDialog
    }

    private fun bindPageJumpControls(binding: DialogPageJumpBinding) {
        binding.seekBar.max = pageJumpViewModel.getSeekBarMax()
        binding.seekBar.progress = pageJumpViewModel.seekBarProgress
        binding.value.filters = pageJumpViewModel.filters
        binding.value.setText(pageJumpViewModel.seekBarProgressText)
        binding.value.setSelection(binding.value.text.length)

        val seekBarChangeListener = pageJumpViewModel.onSeekBarChangeListener
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                seekBarChangeListener.onProgressChanged(seekBar, progress, fromUser)
                val progressText = pageJumpViewModel.seekBarProgressText
                if (!TextUtils.equals(binding.value.text, progressText)) {
                    binding.value.setText(progressText)
                    binding.value.setSelection(binding.value.text.length)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                seekBarChangeListener.onStartTrackingTouch(seekBar)
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                seekBarChangeListener.onStopTrackingTouch(seekBar)
            }
        })

        val textWatcher = pageJumpViewModel.textWatcher
        binding.value.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                textWatcher.beforeTextChanged(s, start, count, after)
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                textWatcher.onTextChanged(s, start, before, count)
            }

            override fun afterTextChanged(s: Editable) {
                textWatcher.afterTextChanged(s)
                val progress = pageJumpViewModel.seekBarProgress
                if (binding.seekBar.progress != progress) {
                    binding.seekBar.progress = progress
                }
            }
        })
    }

    @CallSuper
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_SEEK_BAR_PROGRESS, pageJumpViewModel.seekBarProgress)
    }

    /**
     * Callback interface for responding to page jump.
     */
    interface OnPageJumpedListener {
        fun onPageJumped(position: Int)
    }

    companion object {
        @JvmField
        val TAG: String = PageJumpDialogFragment::class.java.name

        private const val ARG_TOTAL_PAGES = "total_pages"
        private const val ARG_CURRENT_PAGE = "current_page"
        private const val STATE_SEEK_BAR_PROGRESS = "seek_bar_progress"

        @JvmStatic
        fun newInstance(totalPages: Int, currentPage: Int): PageJumpDialogFragment {
            return PageJumpDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_TOTAL_PAGES, totalPages)
                    putInt(ARG_CURRENT_PAGE, currentPage)
                }
            }
        }
    }
}
