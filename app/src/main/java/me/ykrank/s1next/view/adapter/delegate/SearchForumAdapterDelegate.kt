package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import me.ykrank.s1next.data.api.model.search.ForumSearchResult
import me.ykrank.s1next.databinding.ItemSearchForumBinding
import me.ykrank.s1next.widget.span.SearchMovementMethod

class SearchForumAdapterDelegate(context: Context) :
    BaseAdapterDelegate<ForumSearchResult, SearchForumAdapterDelegate.ViewHolder>(
        context,
        ForumSearchResult::class.java
    ) {

    public override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemSearchForumBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolderData(
        t: ForumSearchResult,
        position: Int,
        holder: ViewHolder,
        payloads: List<Any>
    ) {
        holder.bind(t)
    }

    class ViewHolder(private val binding: ItemSearchForumBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.content.movementMethod = SearchMovementMethod.instance
        }

        fun bind(result: ForumSearchResult) {
            binding.content.text = result.htmlContent
        }
    }
}
