package me.ykrank.s1next.view.page.post.postedit.toolstab

import android.content.Context
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.ui.adapter.LibBaseRecyclerViewAdapter
import me.ykrank.s1next.databinding.ItemPostToolsExtrasBinding
import me.ykrank.s1next.view.adapter.delegate.BaseAdapterDelegate
import me.ykrank.s1next.view.page.post.internal.PostToolsExtra

class PostToolsExtrasAdapter(
    context: Context,
    editTextProvider: () -> EditText,
) : LibBaseRecyclerViewAdapter(context, false) {

    init {
        addAdapterDelegate(PostToolsExtrasAdapterDelegate(context, editTextProvider))
    }

    private class PostToolsExtrasAdapterDelegate(
        context: Context,
        private val editTextProvider: () -> EditText,
    ) : BaseAdapterDelegate<PostToolsExtra, PostToolsExtraViewHolder>(context, null) {

        override fun isForViewType(items: MutableList<Any>, position: Int): Boolean {
            return items[position] is PostToolsExtra
        }

        override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            val binding = ItemPostToolsExtrasBinding.inflate(mLayoutInflater, parent, false)
            return PostToolsExtraViewHolder(binding)
        }

        override fun onBindViewHolderData(
            t: PostToolsExtra,
            position: Int,
            holder: PostToolsExtraViewHolder,
            payloads: List<Any>,
        ) {
            holder.bind(t, editTextProvider)
        }
    }

    private class PostToolsExtraViewHolder(
        private val binding: ItemPostToolsExtrasBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(extra: PostToolsExtra, editTextProvider: () -> EditText) {
            binding.ivIcon.setImageResource(extra.icon)
            binding.tvName.setText(extra.name)
            binding.root.setOnClickListener {
                extra.onClick(editTextProvider())
            }
        }
    }
}
