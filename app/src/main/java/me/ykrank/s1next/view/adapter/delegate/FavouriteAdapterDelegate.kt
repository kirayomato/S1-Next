package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.binding.ViewBindingAdapter
import me.ykrank.s1next.data.api.model.Favourite
import me.ykrank.s1next.data.db.biz.ReadProgressBiz
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.databinding.ItemFavouriteBinding
import me.ykrank.s1next.viewmodel.FavouriteViewModel

class FavouriteAdapterDelegate(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val mEventBus: EventBus,
    private val readPreferencesManager: ReadPreferencesManager,
    private val readProgressBiz: ReadProgressBiz
) :
    BaseAdapterDelegate<Favourite, FavouriteAdapterDelegate.ViewHolder>(
        context,
        Favourite::class.java
    ) {

    public override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemFavouriteBinding.inflate(mLayoutInflater, parent, false)
        return ViewHolder(
            binding,
            FavouriteViewModel(lifecycleOwner, readPreferencesManager, readProgressBiz),
            mEventBus
        )
    }

    override fun onBindViewHolderData(t: Favourite, position: Int, holder: ViewHolder, payloads: List<Any>) {
        holder.bind(t)
    }

    class ViewHolder(
        private val binding: ItemFavouriteBinding,
        private val model: FavouriteViewModel,
        eventBus: EventBus
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            ViewBindingAdapter.setOnViewBind(binding.root, model.onBind())
            binding.root.setOnLongClickListener(model.removeFromFavourites(eventBus))
        }

        fun bind(favourite: Favourite) {
            model.favourite.set(favourite)
            binding.root.text = favourite.title
        }
    }
}
