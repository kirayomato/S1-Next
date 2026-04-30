package me.ykrank.s1next.widget.uploadimg.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.widget.uploadimg.ImageUpload

/**
 * Discuz 论坛 swfupload 上传附件响应
 * 
 * 成功示例: {"aid": "12345"}
 * 失败示例: {"error": "没有权限上传"}
 * 错误示例: {"error": {"message": "..."}}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class DiscuzUploadResponse {
    @JsonProperty("aid")
    var aid: String? = null
    
    @JsonProperty("error")
    var error: String? = null

    val success: Boolean get() = !aid.isNullorEmpty()

    fun toCommon(): ImageUpload {
        val model = ImageUpload()
        model.success = success
        model.msg = if (success) null else error
        model.url = if (success) "[attachimg]${aid}[/attachimg]" else null
        model.aid = aid  // 保存附件ID用于删除
        return model
    }

    companion object {
        /**
         * Create a DiscuzUploadResponse from an error message (e.g., when Jackson fails to deserialize)
         */
        fun fromError(errorMsg: String?): DiscuzUploadResponse {
            L.e("❌ DiscuzUploadResponse.fromError: $errorMsg")
            return DiscuzUploadResponse().apply {
                aid = null
                error = errorMsg ?: "上传失败: 服务器返回为空或无效格式"
            }
        }
    }
}

private fun String?.isNullorEmpty(): Boolean = this.isNullOrEmpty()
