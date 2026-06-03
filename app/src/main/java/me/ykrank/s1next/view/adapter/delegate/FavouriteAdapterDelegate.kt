package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewHolder
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.R
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
    BaseAdapterDelegate<Favourite, SimpleRecycleViewHolder<ItemFavouriteBinding>>(
        context,
        Favourite::class.java
    ) {

    public override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = DataBindingUtil.inflate<ItemFavouriteBinding>(mLayoutInflater,
                R.layout.item_favourite, parent, false)
        binding.model = FavouriteViewModel(lifecycleOwner, readPreferencesManager, readProgressBiz)
        binding.rxBus = mEventBus
        return SimpleRecycleViewHolder(binding)
    }

    override fun onBindViewHolderData(t: Favourite, position: Int, holder: SimpleRecycleViewHolder<ItemFavouriteBinding>, payloads: List<Any>) {
        val binding = holder.binding
        binding.model?.favourite?.set(t)
    }

}
