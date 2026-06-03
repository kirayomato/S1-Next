package me.ykrank.s1next.view.page.setting.blacklist

import android.app.Activity
import android.content.Context
import android.database.Cursor
import androidx.cursoradapter.widget.CursorAdapter
import androidx.databinding.DataBindingUtil
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import me.ykrank.s1next.R
import me.ykrank.s1next.data.db.biz.BlackListBiz
import me.ykrank.s1next.data.db.dbmodel.BlackList
import me.ykrank.s1next.databinding.ItemBlacklistBinding
import me.ykrank.s1next.viewmodel.BlackListViewModel


class BlackListCursorListViewAdapter(
    activity: Activity,
    private val blackListBiz: BlackListBiz
) : CursorAdapter(activity, null, true) {
    private val mLayoutInflater: LayoutInflater = activity.layoutInflater

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val itemBlacklistBinding = DataBindingUtil.inflate<ItemBlacklistBinding>(
            mLayoutInflater,
            R.layout.item_blacklist, parent, false
        )
        itemBlacklistBinding.blackListViewModel = BlackListViewModel()
        return itemBlacklistBinding.root
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val binding = DataBindingUtil.findBinding<ItemBlacklistBinding>(view)
        binding?.blackListViewModel?.blacklist?.set(blackListBiz.fromBlackListCursor(cursor))
    }

    override fun getItem(position: Int): BlackList {
        val cursor = super.getItem(position) as Cursor
        return blackListBiz.fromBlackListCursor(cursor)
    }
}
