package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.binding.LibTextViewBindingAdapter
import com.github.ykrank.androidtools.binding.LibViewBindingAdapter
import me.ykrank.s1next.binding.ImageViewBindingAdapter
import me.ykrank.s1next.data.api.model.Note
import me.ykrank.s1next.databinding.ItemNoteBinding
import me.ykrank.s1next.view.page.post.postlist.PostListGatewayActivity

/**
 * Created by ykrank on 2017/1/5.
 */

class NoteAdapterDelegate(context: Context) : BaseAdapterDelegate<Note, NoteAdapterDelegate.ViewHolder>(context, Note::class.java) {

    override fun onBindViewHolderData(t: Note, position: Int, holder: ViewHolder, payloads: List<Any>) {
        holder.bind(t)
    }

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemNoteBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(binding)
    }

    class ViewHolder(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {
        private var avatarUid: String? = null

        fun bind(note: Note) {
            val tint = if (note.isNew()) {
                binding.root.context.getColor(com.github.ykrank.androidtools.R.color.red_A100)
            } else {
                Int.MIN_VALUE
            }
            LibViewBindingAdapter.setCardBackgroundTint(binding.root, null, tint)
            ImageViewBindingAdapter.loadAvatar(binding.avatar, oldUid = avatarUid, newUid = note.authorId)
            avatarUid = note.authorId
            binding.authorName.text = note.author
            LibTextViewBindingAdapter.setRelativeDateTime(binding.tvName, note.dateline * 1000)
            LibTextViewBindingAdapter.setUnderlineText(binding.tvSummary, note.content)
            binding.tvSummary.setOnClickListener {
                val url = note.url
                if (!url.isNullOrEmpty()) {
                    PostListGatewayActivity.start(it.context, Uri.parse(url))
                }
            }
        }
    }
}
