package me.ykrank.s1next.widget.uploadimg

import me.ykrank.s1next.data.api.Api
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object ForumAttachmentPostSubmitHelper {
    private val attachImgRegex = Regex("""(?i)\[attachimg](\d+)\[/attachimg]""")
    private val forumImageRegex = Regex("""(?is)\[img](.*?forum\.php\?.*?(?:[?&]|&amp;)aid=(\d+).*?)\[/img]""")

    fun normalizeMessage(message: String?): String? {
        if (message.isNullOrEmpty()) {
            return message
        }
        return forumImageRegex.replace(message) { match ->
            "[attachimg]${match.groupValues[2]}[/attachimg]"
        }
    }

    fun collectForumAttachmentIds(message: String?): Set<String> {
        if (message.isNullOrEmpty()) {
            return emptySet()
        }
        val ids = linkedSetOf<String>()
        attachImgRegex.findAll(message).forEach { match ->
            ids.add(match.groupValues[1])
        }
        forumImageRegex.findAll(message).forEach { match ->
            ids.add(match.groupValues[2])
        }
        return ids
    }

    fun hasForumAttachments(message: String?): Boolean {
        return collectForumAttachmentIds(message).isNotEmpty()
    }

    fun appendAttachNewFields(fields: MutableMap<String, String>, attachmentIds: Set<String>) {
        attachmentIds.forEach { aid ->
            fields["attachnew[$aid][description]"] = ""
            fields["attachnew[$aid][readperm]"] = ""
        }
    }

    fun webSubmitUrl(formAction: String?, fallbackPath: String): String {
        val rawUrl = formAction
            ?.takeIf { it.isNotBlank() }
            ?: fallbackPath
        val decodedUrl = rawUrl.replace("&amp;", "&")
        val baseUrl = Api.BASE_URL.toHttpUrlOrNull()
        val httpUrl = baseUrl?.resolve(decodedUrl) ?: decodedUrl.toHttpUrlOrNull()
        if (httpUrl != null) {
            val builder = httpUrl.newBuilder()
            if (httpUrl.queryParameter("inajax").isNullOrEmpty()) {
                builder.addQueryParameter("inajax", "1")
            }
            return builder.build().toString()
        }
        return if (decodedUrl.contains("inajax=")) {
            decodedUrl
        } else {
            decodedUrl + if (decodedUrl.contains("?")) "&inajax=1" else "?inajax=1"
        }
    }
}
