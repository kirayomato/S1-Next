package com.github.ykrank.androidtools.ui.vm

import android.text.Editable
import android.text.InputFilter
import android.text.TextUtils
import android.text.TextWatcher
import android.widget.SeekBar
import com.github.ykrank.androidtools.widget.RangeInputFilter


class PageJumpViewModel(private val seekBarMax: Int, seekBarProgress: Int) {

    var seekBarProgress: Int = 0
        private set

    // current page is zero-based
    val seekBarProgressText: CharSequence
        get() = (seekBarProgress + 1).toString()

    val onSeekBarChangeListener: SeekBar.OnSeekBarChangeListener
        get() = object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (progress != this@PageJumpViewModel.seekBarProgress) {
                    seekBarProgress = progress
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        }

    // SeekBar max is zero-based
    val filters: Array<InputFilter>
        get() = arrayOf(RangeInputFilter(1, seekBarMax + 1))

    val textWatcher: TextWatcher
        get() = object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable) {
                val value = s.toString()
                if (!TextUtils.isEmpty(s)) {
                    val progress = Integer.parseInt(value) - 1
                    if (progress != this@PageJumpViewModel.seekBarProgress) {
                        seekBarProgress = progress
                    }
                }
            }
        }

    init {
        this.seekBarProgress = seekBarProgress
    }

    fun getSeekBarMax(): Int {
        return seekBarMax
    }
}
