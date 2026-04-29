package me.ykrank.s1next.data.api.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 论坛图片上传配置，从发帖/回复页面的HTML中解析
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class ForumUploadConfig {
    @JsonProperty("upload_url")
    var uploadUrl: String? = null

    @JsonProperty("hash")
    var hash: String? = null

    @JsonProperty("uid")
    var uid: String? = null

    @JsonProperty("fid")
    var fid: Int = 0

    @JsonProperty("file_size_limit")
    var fileSizeLimit: String? = null

    @JsonProperty("file_types")
    var fileTypes: String? = null

    /**
     * 从HTML中解析上传配置
     */
    companion object {
        fun fromHtml(html: String?): ForumUploadConfig? {
            if (html.isNullOrEmpty()) return null
            
            val config = ForumUploadConfig()
            
            // 解析 upload_url
            // 格式类似: upload_url: "https://stage1st.com/2b/misc.php?mod=swfupload&action=swfupload&operation=upload&fid=151"
            val uploadUrlPattern = """upload_url\s*:\s*["'](.*?)["']""".toRegex()
            val uploadUrlMatch = uploadUrlPattern.find(html)
            config.uploadUrl = uploadUrlMatch?.groupValues?.getOrNull(1)
            
            // 解析 post_params 中的 hash
            // 格式类似: post_params: {"uid" : "535990", "hash":"a70a922fef3aa0f2bd7efa3c4d429be4", "type":"image"}
            val hashPattern = """["']hash["']\s*:\s*["'](.*?)["']""".toRegex()
            val hashMatch = hashPattern.find(html)
            config.hash = hashMatch?.groupValues?.getOrNull(1)
            
            // 解析 uid
            val uidPattern = """["']uid["']\s*:\s*["'](.*?)["']""".toRegex()
            val uidMatch = uidPattern.find(html)
            config.uid = uidMatch?.groupValues?.getOrNull(1)
            
            // 从 upload_url 中解析 fid
            config.uploadUrl?.let { url ->
                val fidPattern = """fid=(\d+)""".toRegex()
                val fidMatch = fidPattern.find(url)
                config.fid = fidMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            }
            
            // 如果没找到必要参数，返回 null
            if (config.hash.isNullOrEmpty()) {
                return null
            }
            
            return config
        }
    }
}
