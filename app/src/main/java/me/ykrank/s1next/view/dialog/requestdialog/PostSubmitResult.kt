package me.ykrank.s1next.view.dialog.requestdialog

import me.ykrank.s1next.data.api.model.AjaxResult
import me.ykrank.s1next.data.api.model.wrapper.AccountResultWrapper

data class PostSubmitResult(
    val success: Boolean,
    val message: String?,
) {
    companion object {
        fun fromAccountResult(wrapper: AccountResultWrapper): PostSubmitResult {
            val result = wrapper.result
            return PostSubmitResult(result.defaultSuccess, result.message)
        }

        fun fromAjaxHtml(html: String?): PostSubmitResult {
            val result = AjaxResult.fromAjaxString(html)
            return PostSubmitResult(result.success, result.msg)
        }
    }
}
