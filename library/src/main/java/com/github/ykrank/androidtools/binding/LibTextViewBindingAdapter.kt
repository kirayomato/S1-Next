package com.github.ykrank.androidtools.binding

import android.content.Context
import android.graphics.Paint
import android.text.format.DateUtils
import android.widget.TextView
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LibTextViewBindingAdapter {
    private val df: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    @JvmStatic
    fun setUnderlineText(textView: TextView, text: String?) {
        textView.paintFlags = textView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        textView.paint.isAntiAlias = true
        textView.text = text
    }

    @JvmStatic
    fun setRelativeDateTime(textView: TextView, datetime: Long) {
        textView.text = getRelativeDateTime(textView.context, datetime)
    }

    @JvmStatic
    fun getRelativeDateTime(context: Context, datetime: Long): CharSequence {
        return DateUtils.getRelativeDateTimeString(
            context,
            datetime,
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.DAY_IN_MILLIS,
            0
        )
    }

    @JvmStatic
    fun setSecondTime(textView: TextView, datetimeSecond: Long?) {
        if (datetimeSecond == null) {
            textView.text = "-"
        } else {
            textView.text = df.format(Date(datetimeSecond * 1000))
        }
    }
}
