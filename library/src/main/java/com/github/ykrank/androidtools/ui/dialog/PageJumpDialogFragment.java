package com.github.ykrank.androidtools.ui.dialog;

import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.SeekBar;

import com.github.ykrank.androidtools.R;
import com.github.ykrank.androidtools.databinding.DialogPageJumpBinding;
import com.github.ykrank.androidtools.ui.vm.PageJumpViewModel;
import com.github.ykrank.androidtools.util.ViewUtil;


/**
 * A dialog shows {@link SeekBar} and {@link EditText} to
 * display the page number you want to go to.
 * <p>
 * Host class should implement {@link OnPageJumpedListener}
 * in order to to handle the page jump event.
 */
public final class PageJumpDialogFragment extends LibBaseDialogFragment {

    public static final String TAG = PageJumpDialogFragment.class.getName();

    private static final String ARG_TOTAL_PAGES = "total_pages";
    private static final String ARG_CURRENT_PAGE = "current_page";

    /**
     * The serialization (saved instance state) Bundle key representing
     * the SeekBar's progress.
     */
    private static final String STATE_SEEK_BAR_PROGRESS = "seek_bar_progress";

    private PageJumpViewModel mPageJumpViewModel;

    public static PageJumpDialogFragment newInstance(int totalPages, int currentPage) {
        PageJumpDialogFragment fragment = new PageJumpDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_TOTAL_PAGES, totalPages);
        bundle.putInt(ARG_CURRENT_PAGE, currentPage);
        fragment.setArguments(bundle);

        return fragment;
    }

    @NonNull
    @Override
    public final Dialog onCreateDialog(Bundle savedInstanceState) {
        DialogPageJumpBinding binding = DialogPageJumpBinding.inflate(getActivity().getLayoutInflater(),
                null, false);

        int seekBarProgress;
        if (savedInstanceState == null) {
            seekBarProgress = getArguments().getInt(ARG_CURRENT_PAGE);
        } else {
            seekBarProgress = savedInstanceState.getInt(STATE_SEEK_BAR_PROGRESS);
        }

        // SeekBar max is zero-based
        mPageJumpViewModel = new PageJumpViewModel(getArguments().getInt(ARG_TOTAL_PAGES) - 1,
                seekBarProgress);
        bindPageJumpControls(binding);

        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setTitle(R.string.menu_page_jump)
                .setView(binding.getRoot())
                .setPositiveButton(R.string.dialog_button_text_jump, (dialog, which) -> {
                    if (!TextUtils.isEmpty(binding.value.getText())) {
                        ((OnPageJumpedListener) getParentFragment()).onPageJumped(
                                mPageJumpViewModel.getSeekBarProgress());
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        ViewUtil.consumeRunnableWhenImeActionPerformed(binding.value, () ->
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick());
        return alertDialog;
    }

    private void bindPageJumpControls(DialogPageJumpBinding binding) {
        binding.seekBar.setMax(mPageJumpViewModel.getSeekBarMax());
        binding.seekBar.setProgress(mPageJumpViewModel.getSeekBarProgress());
        binding.value.setFilters(mPageJumpViewModel.getFilters());
        binding.value.setText(mPageJumpViewModel.getSeekBarProgressText());
        binding.value.setSelection(binding.value.getText().length());

        SeekBar.OnSeekBarChangeListener seekBarChangeListener = mPageJumpViewModel.getOnSeekBarChangeListener();
        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarChangeListener.onProgressChanged(seekBar, progress, fromUser);
                CharSequence progressText = mPageJumpViewModel.getSeekBarProgressText();
                if (!TextUtils.equals(binding.value.getText(), progressText)) {
                    binding.value.setText(progressText);
                    binding.value.setSelection(binding.value.getText().length());
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                seekBarChangeListener.onStartTrackingTouch(seekBar);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                seekBarChangeListener.onStopTrackingTouch(seekBar);
            }
        });

        TextWatcher textWatcher = mPageJumpViewModel.getTextWatcher();
        binding.value.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                textWatcher.beforeTextChanged(s, start, count, after);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textWatcher.onTextChanged(s, start, before, count);
            }

            @Override
            public void afterTextChanged(Editable s) {
                textWatcher.afterTextChanged(s);
                int progress = mPageJumpViewModel.getSeekBarProgress();
                if (binding.seekBar.getProgress() != progress) {
                    binding.seekBar.setProgress(progress);
                }
            }
        });
    }

    @Override
    @CallSuper
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(STATE_SEEK_BAR_PROGRESS, mPageJumpViewModel.getSeekBarProgress());
    }

    /**
     * Callback interface for responding to page jump.
     */
    public interface OnPageJumpedListener {

        /**
         * This method will be invoked when a page is selected.
         *
         * @param position Position index of the new selected page.
         */
        void onPageJumped(int position);
    }
}
