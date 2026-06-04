package me.ykrank.s1next.binding;

import android.widget.EditText;

public final class EditTextBindingAdapter {

    private EditTextBindingAdapter() {
    }

    public static void setHasProgress(EditText editText, Boolean editable) {
        editText.setEnabled(editable);
    }
}
