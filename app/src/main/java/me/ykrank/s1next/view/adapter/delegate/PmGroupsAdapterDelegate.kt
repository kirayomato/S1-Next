package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import androidx.databinding.DataBindingUtil
import android.view.ViewGroup
import com.github.ykrank.androidtools.ui.adapter.simple.SimpleRecycleViewHolder
import com.github.ykrank.androidtools.widget.EventBus
import me.ykrank.s1next.R
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.model.PmGroup
import me.ykrank.s1next.databinding.ItemPmGroupBinding
import me.ykrank.s1next.viewmodel.PmGroupViewModel

class PmGroupsAdapterDelegate(
    context: Context,
    private val mEventBus: EventBus,
    private val mUser: User
) : BaseAdapterDelegate<PmGroup, SimpleRecycleViewHolder<ItemPmGroupBinding>>(context, PmGroup::class.java) {

    public override fun onCreateViewHolder(parent: ViewGroup): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        val binding = DataBindingUtil.inflate<ItemPmGroupBinding>(mLayoutInflater,
                R.layout.item_pm_group, parent, false)
        binding.model = PmGroupViewModel()
        binding.rxBus = mEventBus
        binding.user = mUser
        return SimpleRecycleViewHolder<ItemPmGroupBinding>(binding)
    }

    override fun onBindViewHolderData(t: PmGroup, position: Int, holder: SimpleRecycleViewHolder<ItemPmGroupBinding>, payloads: List<Any>) {
        val binding = holder.binding
        binding.model?.pmGroup?.set(t)
    }

}
