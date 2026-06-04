package com.github.ykrank.androidtools.binding;

import android.content.Context;
import android.graphics.Paint;
import android.text.format.DateUtils;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class LibTextViewBindingAdapter {
    private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    private LibTextViewBindingAdapter() {
    }

    public static void setUnderlineText(TextView textView, String text) {
        textView.setPaintFlags(textView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        textView.getPaint().setAntiAlias(true);
        textView.setText(text);
    }

    public static void setRelativeDateTime(TextView textView, long datetime) {
        textView.setText(getRelativeDateTime(textView.getContext(), datetime));
    }

    public static CharSequence getRelativeDateTime(Context context, long datetime) {
        return DateUtils.getRelativeDateTimeString(context, datetime,
                DateUtils.MINUTE_IN_MILLIS, DateUtils.DAY_IN_MILLIS, 0);
    }

    public static void setSecondTime(TextView textView, Long datetimeSecond) {
        if (datetimeSecond == null){
            textView.setText("-");
        } else {
            textView.setText(df.format(new Date(datetimeSecond * 1000)));
        }
    }
}
