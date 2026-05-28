package me.ykrank.s1next.widget.uploadimg

import me.ykrank.s1next.data.api.model.PostEditor

const val FORUM_ATTACHMENT_REMOTE_PREFIX = "forum_attachment_"

sealed class ForumAttachmentUploadTarget {
    data class NewThread(val fid: Int) : ForumAttachmentUploadTarget()
    data class Reply(val tid: String, val fid: Int? = null, val page: Int = 1) : ForumAttachmentUploadTarget()
    data class EditPost(val fid: Int, val tid: Int, val pid: Int) : ForumAttachmentUploadTarget()
}

interface ForumAttachmentUploadTargetProvider {
    val forumAttachmentUploadTarget: ForumAttachmentUploadTarget?
    val forumAttachmentUploadInfo: PostEditor.ForumAttachmentUploadInfo?
        get() = null
    val forumAttachments: List<PostEditor.ForumAttachment>
        get() = emptyList()
}
