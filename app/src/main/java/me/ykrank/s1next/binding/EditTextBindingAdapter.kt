package me.ykrank.s1next.binding

import android.widget.EditText

object EditTextBindingAdapter {
    @JvmStatic
    fun setHasProgress(editText: EditText, editable: Boolean) {
        editText.isEnabled = editable
    }
}
