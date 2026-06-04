package me.ykrank.s1next.binding

import android.os.Build
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Spinner
import me.ykrank.s1next.R
import me.ykrank.s1next.view.adapter.ArrayAdapterCompat

object SpinnerBindingAdapter {

    @JvmStatic
    fun setForumGroupNameList(
        spinner: Spinner,
        dropDownItemList: List<CharSequence>,
        selectedItemPosition: Int
    ) {
        spinner.adapter = getSpinnerAdapter(spinner, dropDownItemList)
        // invalid position may occurs when user's login status has changed
        if (spinner.adapter.count - 1 < selectedItemPosition) {
            spinner.setSelection(0, false)
        } else {
            spinner.setSelection(selectedItemPosition, false)
        }
    }

    private fun getSpinnerAdapter(
        spinner: Spinner,
        dropDownItemList: List<CharSequence>
    ): BaseAdapter {
        // don't use dropDownItemList#add(int, E), otherwise repeated calls add multiple "全部".
        val list = ArrayList<CharSequence>()
        list.add(spinner.context.getString(R.string.toolbar_spinner_drop_down_all_forums_item_title))
        list.addAll(dropDownItemList)

        val context = spinner.context
        val arrayAdapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ArrayAdapter(context, R.layout.toolbar_spinner_item, list)
        } else {
            ArrayAdapterCompat(context, R.layout.toolbar_spinner_item, list)
        }
        arrayAdapter.setDropDownViewResource(R.layout.toolbar_spinner_dropdown_item)

        return arrayAdapter
    }
}
