package me.ykrank.s1next.view.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ykrank.s1next.App
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.S1Service
import me.ykrank.s1next.data.api.model.WebBlackListInfo
import me.ykrank.s1next.data.api.runApiCatching
import me.ykrank.s1next.data.api.toastError
import me.ykrank.s1next.data.db.biz.BlackListBiz
import me.ykrank.s1next.data.db.dbmodel.BlackList


/**
 * A dialog lets user load website blacklist.
 */
class LoadBlackListFromWebDialogFragment : BaseLoadProgressDialogFragment() {
    private val s1Service: S1Service by lazy { App.appComponent.s1Service }

    private val mUser: User by lazy { App.appComponent.user }

    private val blackListBiz: BlackListBiz by lazy { App.appComponent.blackListBiz }

    private var callBack: (() -> Unit)? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            loadNextPage()
        }
    }

    private suspend fun loadNextPage(page: Int = 1) {
        val info = runApiCatching {
            s1Service.getWebBlackList(mUser.uid, page)
        }.map {
            withContext(Dispatchers.IO) {
                WebBlackListInfo.fromHtml(it)
            }
        }
        info.toastError(activity) {
            withContext(Dispatchers.IO) {
                users.forEach { pair ->
                    blackListBiz.saveBlackList(
                        BlackList(
                            pair.first,
                            pair.second,
                            BlackList.HIDE_POST,
                            BlackList.HIDE_FORUM
                        )
                    )
                }
            }
            binding.max = this.max
            binding.progress = this.page

            if (this.max > this.page) {
                loadNextPage(this.page + 1)
            } else {
                this@LoadBlackListFromWebDialogFragment.dismiss()
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        callBack?.invoke()
        val parentFragment = parentFragment
        if (parentFragment is DialogInterface.OnDismissListener) {
            (parentFragment as DialogInterface.OnDismissListener).onDismiss(dialog)
        }
    }

    companion object {
        val TAG: String = LoadBlackListFromWebDialogFragment::class.java.simpleName

        fun newInstance(callBack: (() -> Unit)? = null): LoadBlackListFromWebDialogFragment {
            val fragment = LoadBlackListFromWebDialogFragment()
            fragment.callBack = callBack
            return fragment
        }
    }
}
