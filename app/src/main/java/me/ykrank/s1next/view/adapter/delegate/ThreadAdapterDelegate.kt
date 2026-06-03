package me.ykrank.s1next.view.adapter.delegate

import android.content.Context
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import me.ykrank.s1next.binding.TextViewBindingAdapter
import me.ykrank.s1next.binding.ViewBindingAdapter
import me.ykrank.s1next.data.api.model.Thread
import me.ykrank.s1next.data.db.biz.ReadProgressBiz
import me.ykrank.s1next.data.pref.ReadPreferencesManager
import me.ykrank.s1next.data.pref.ThemeManager
import me.ykrank.s1next.databinding.ItemThreadBinding
import me.ykrank.s1next.viewmodel.ThreadViewModel
import me.ykrank.s1next.viewmodel.UserViewModel

class ThreadAdapterDelegate(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val forumId: String?,
    private val mUserViewModel: UserViewModel,
    private val mThemeManager: ThemeManager,
    private val mReadPreferencesManager: ReadPreferencesManager,
    private val readProgressBiz: ReadProgressBiz
) :
        BaseAdapterDelegate<Thread, ThreadAdapterDelegate.ViewHolder>(context, Thread::class.java) {

    public override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val binding = ItemThreadBinding.inflate(mLayoutInflater, parent, false)
        val model = ThreadViewModel(lifecycleOwner, mReadPreferencesManager, readProgressBiz)

        val threadPadding = mReadPreferencesManager.threadPadding
        if (threadPadding != null && threadPadding > 0) {
            val paddingPx = threadPadding * parent.context.resources.displayMetrics.scaledDensity.toInt()
            binding.tvThread.setPadding(binding.tvThread.paddingLeft, paddingPx, binding.tvThread.paddingLeft, paddingPx)
        }

        return ViewHolder(binding, model, mUserViewModel, mThemeManager, forumId)
    }

    override fun onBindViewHolderData(t: Thread, position: Int, holder: ViewHolder, payloads: List<Any>) {
        holder.bind(t)
    }

    class ViewHolder(
        private val binding: ItemThreadBinding,
        private val model: ThreadViewModel,
        private val userViewModel: UserViewModel,
        private val themeManager: ThemeManager,
        private val forumId: String?
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            ViewBindingAdapter.setOnViewBind(binding.tvThread, model.onBind())
            binding.tvThread.setOnLongClickListener(model.goToThisThreadLastPage())
        }

        fun bind(thread: Thread) {
            model.thread.set(thread)
            TextViewBindingAdapter.setThread(
                binding.tvThread,
                themeManager,
                forumId.orEmpty(),
                thread,
                userViewModel.user
            )
        }
    }

}
