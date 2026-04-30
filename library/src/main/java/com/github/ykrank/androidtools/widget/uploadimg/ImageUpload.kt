package com.github.ykrank.androidtools.widget.uploadimg

class ImageUpload {
    var success: Boolean = false
    var msg: String? = null
    var url: String? = null
    var deleteUrl: String? = null
    var aid: String? = null  // 附件ID，用于Discuz论坛删除

    override fun toString(): String {
        return "ImageUpload(success=$success, msg=$msg, url=$url, deleteUrl=$deleteUrl, aid=$aid)"
    }
}