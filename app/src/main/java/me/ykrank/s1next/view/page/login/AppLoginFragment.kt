package me.ykrank.s1next.view.page.login

import dagger.hilt.android.AndroidEntryPoint


/**
 * A Fragment offers login via username and password.
 */
@AndroidEntryPoint
class AppLoginFragment : BaseLoginFragment() {
    override fun showLoginDialog(
        username: String,
        password: String,
        questionId: Int?,
        answer: String?
    ) {
        AppLoginDialogFragment.newInstance(username, password, questionId, answer).show(
            parentFragmentManager,
            AppLoginDialogFragment.TAG
        )
    }

    companion object {
        @JvmField
        val TAG = AppLoginFragment::class.java.getName()
    }
}
