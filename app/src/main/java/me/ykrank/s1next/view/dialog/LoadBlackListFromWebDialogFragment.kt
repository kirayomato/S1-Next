package me.ykrank.s1next.view.dialog

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.S1Service
import me.ykrank.s1next.data.api.model.WebBlackListInfo
import me.ykrank.s1next.data.api.runApiCatching
import me.ykrank.s1next.data.api.toastError
import me.ykrank.s1next.data.db.biz.BlackListBiz
import me.ykrank.s1next.data.db.dbmodel.BlackList
import javax.inject.Inject


/**
 * A dialog lets user load website blacklist.
 */
@AndroidEntryPoint
class LoadBlackListFromWebDialogFragment : BaseLoadProgressDialogFragment() {
    @Inject
    internal lateinit var s1Service: S1Service

    @Inject
    internal lateinit var mUser: User

    @Inject
    internal lateinit var blackListBiz: BlackListBiz

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
            progressMax = this.max
            progressValue = this.page

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
