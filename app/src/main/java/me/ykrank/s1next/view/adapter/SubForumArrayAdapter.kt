package me.ykrank.s1next.view.adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.annotation.LayoutRes
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.data.api.model.Forum
import me.ykrank.s1next.data.pref.ThemeManager

class SubForumArrayAdapter(
    activity: Activity,
    @LayoutRes resource: Int,
    objects: List<Forum>,
    themeManager: ThemeManager
) :
    ArrayAdapter<Forum?>(activity, resource, objects) {

    private val mLayoutInflater: LayoutInflater

    @LayoutRes
    private val mResource: Int
    private val mGentleAccentColor: Int

    init {
        mLayoutInflater = activity.layoutInflater
        mResource = resource
        mGentleAccentColor = themeManager.gentleAccentColor
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val viewHolder: ViewHolder
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(mResource, parent, false)
            viewHolder = ViewHolder(convertView as TextView)
            convertView.setTag(viewHolder)
        } else {
            viewHolder = convertView.tag as ViewHolder
        }
        getItem(position)?.apply {
            TextViewBindingAdapter.setForum(
                viewHolder.textView,
                this,
                mGentleAccentColor
            )
        }
        return convertView
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getItemId(position: Int): Long {
        return getItem(position)?.id?.toLong() ?: 0
    }

    private class ViewHolder(val textView: TextView) {
    }
}
