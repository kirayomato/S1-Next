package me.ykrank.s1next.view.event

/**
 * Add image in post edit or new
 */
class PostAddImageEvent(
    val url: String,
    val insertText: String = "[img]$url[/img]",
    val forumAttachmentId: String? = null,
)
