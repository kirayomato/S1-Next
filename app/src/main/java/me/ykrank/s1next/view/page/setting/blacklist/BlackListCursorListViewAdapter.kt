package me.ykrank.s1next.view.page.setting.blacklist

import android.app.Activity
import android.content.Context
import android.database.Cursor
import androidx.cursoradapter.widget.CursorAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import me.ykrank.s1next.data.db.biz.BlackListBiz
import me.ykrank.s1next.data.db.dbmodel.BlackList
import me.ykrank.s1next.databinding.ItemBlacklistBinding


class BlackListCursorListViewAdapter(
    activity: Activity,
    private val blackListBiz: BlackListBiz
) : CursorAdapter(activity, null, true) {
    private val mLayoutInflater: LayoutInflater = activity.layoutInflater

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val binding = ItemBlacklistBinding.inflate(mLayoutInflater, parent, false)
        return binding.root.also {
            it.tag = binding
        }
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val binding = view.tag as ItemBlacklistBinding
        binding.bind(blackListBiz.fromBlackListCursor(cursor))
    }

    override fun getItem(position: Int): BlackList {
        val cursor = super.getItem(position) as Cursor
        return blackListBiz.fromBlackListCursor(cursor)
    }

    private fun ItemBlacklistBinding.bind(blackList: BlackList) {
        authorId.text = blackList.authorId.toString()
        authorName.text = blackList.author
        forum.setText(blackList.forumRes)
        post.setText(blackList.postRes)
        time.text = blackList.time
    }
}
