package me.ykrank.s1next.view.adapter

import android.app.Activity
import android.content.Context
import android.database.Cursor
import androidx.cursoradapter.widget.CursorAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.ykrank.s1next.data.db.biz.BlackWordBiz
import me.ykrank.s1next.data.db.dbmodel.BlackWord
import me.ykrank.s1next.databinding.ItemBlackwordBinding


class BlackWordCursorListViewAdapter(
    activity: Activity,
    private val blackWordBiz: BlackWordBiz
) : CursorAdapter(activity, null, true) {
    private val mLayoutInflater: LayoutInflater = activity.layoutInflater

    override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
        val binding = ItemBlackwordBinding.inflate(mLayoutInflater, parent, false)
        return binding.root.also {
            it.tag = binding
        }
    }

    override fun bindView(view: View, context: Context, cursor: Cursor) {
        val binding = view.tag as ItemBlackwordBinding
        binding.bind(blackWordBiz.fromBlackWordCursor(cursor))
    }

    override fun getItem(position: Int): BlackWord {
        val cursor = super.getItem(position) as Cursor
        return blackWordBiz.fromBlackWordCursor(cursor)
    }

    private fun ItemBlackwordBinding.bind(blackWord: BlackWord) {
        word.text = blackWord.word
        stat.setText(blackWord.statRes)
        time.text = blackWord.time
    }
}
