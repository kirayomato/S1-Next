package me.ykrank.s1next.view.page.login

import android.os.Bundle
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Single
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import me.ykrank.s1next.data.api.model.wrapper.AccountResultWrapper
import me.ykrank.s1next.data.db.biz.LoginUserBiz
import me.ykrank.s1next.data.db.exmodel.RealLoginUser
import me.ykrank.s1next.view.dialog.ProgressDialogFragment
import me.ykrank.s1next.view.event.LoginEvent
import javax.inject.Inject

/**
 * A [ProgressDialogFragment] posts a request to login to server.
 */
@AndroidEntryPoint
class LoginDialogFragment : BaseLoginDialogFragment<AccountResultWrapper>() {

    @Inject
    internal lateinit var loginUserBiz: LoginUserBiz

    override fun getSourceObservable(): Single<AccountResultWrapper> {
        return mS1Service.login(username, password, questionId, answer).map { resultWrapper ->
            // the authenticity token is not fresh after login
            resultWrapper.data?.apply {
                authenticityToken = null
                mUserValidator.validate(this)
            }
            resultWrapper
        }
    }

    override fun parseData(data: AccountResultWrapper): Result {
        val result = data.result
        return if (result.defaultSuccess) {
            Result(true, result.message)
        } else {
            Result(false, result.message)
        }
    }

    override fun onSuccess(data: AccountResultWrapper, result: Result) {
        super.onSuccess(data, result)

        // 自动登录黑科技
        val username = this.username
        val password = this.password
        if (username != null && password != null) {
            saveLoginUser2Db(data)

            AppLoginDialogFragment.newInstance(username, password, questionId, answer).show(
                parentFragmentManager,
                AppLoginDialogFragment.TAG
            )
        }

        mEventBus.postDefault(LoginEvent())
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun saveLoginUser2Db(data: AccountResultWrapper) {
        GlobalScope.launch(Dispatchers.IO) {
            val time = System.currentTimeMillis()
            val user = RealLoginUser(
                id = null,
                uid = data.data?.uid?.toInt() ?: 0,
                name = data.data?.username,
                password = password,
                questionId = questionId?.toString(),
                answer = answer,
                loginTime = time,
                timestamp = time,
            )
            loginUserBiz.saveUser(user)
        }
    }

    companion object {

        val TAG = LoginDialogFragment::class.java.simpleName


        fun newInstance(
            username: String,
            password: String,
            questionId: Int?,
            answer: String?
        ): LoginDialogFragment {
            val fragment = LoginDialogFragment()
            val bundle = Bundle()
            addBundle(bundle, username, password, questionId, answer)
            fragment.arguments = bundle

            return fragment
        }
    }
}
