package me.ykrank.s1next.view.page.login

import dagger.hilt.android.AndroidEntryPoint


/**
 * A Fragment offers login via username and password.
 */
@AndroidEntryPoint
class LoginFragment : BaseLoginFragment() {
    override fun showLoginDialog(
        username: String,
        password: String,
        questionId: Int?,
        answer: String?
    ) {
        LoginDialogFragment.newInstance(username, password, questionId, answer).show(
            parentFragmentManager,
            AppLoginDialogFragment.TAG
        )
    }

    companion object {
        @JvmField
        val TAG = LoginFragment::class.java.getName()
    }
}
