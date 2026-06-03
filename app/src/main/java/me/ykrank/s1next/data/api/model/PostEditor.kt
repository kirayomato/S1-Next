package me.ykrank.s1next.data.api.model

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.util.LooperUtil
import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.model.wrapper.HtmlDataWrapper
import me.ykrank.s1next.util.JsonUtil
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import java.util.*

/**
 * Model for edit post
 * Created by ykrank on 2017/4/12.
 */

class PostEditor {
    /**
     * if no element "#typeid>option", this is null
     */
    var threadTypes: List<ThreadType>? = null
    var typeIndex: Int = 0
    var readPermTypes: List<String>? = null
    var readPermIndex: Int = 0
    var subject: String? = null
    var message: String? = null
    var formAction: String? = null
    var formHash: String? = null
    var postTime: Long? = null
    var noticeAuthor: String? = null
    var noticeTrimStr: String? = null
    var noticeAuthorMsg: String? = null
    var forumAttachmentUploadInfo: ForumAttachmentUploadInfo? = null
    var forumAttachments: List<ForumAttachment> = emptyList()

    data class ForumAttachmentUploadInfo(
        val uploadUrl: String?,
        val fid: Int?,
        val uid: String?,
        val hash: String,
    )

    data class ForumAttachment(
        val aid: String,
        val fileName: String?,
        val imageUrl: String?,
    )

    companion object {

        @Throws
        fun fromHtml(html: String): PostEditor {
            LooperUtil.enforceOnWorkThread()
            val editor = PostEditor()
            try {
                val document = Jsoup.parse(unwrapAjaxHtml(html))
                HtmlDataWrapper.preTreatHtml(document)
                HtmlDataWrapper.preAlertHtml(document)
                HtmlDataWrapper.preAlertAjaxHtml(document)
                editor.formAction = document.selectFirst("form#postform, form[action*=mod=post]")?.attr("action")
                editor.formHash = document.selectFirst("input[name=formhash]")?.attr("value")?.trim()
                    ?.takeIf { it.isNotEmpty() }
                editor.postTime = document.selectFirst("input[name=posttime]")?.attr("value")?.trim()?.toLongOrNull()
                editor.noticeAuthor = document.selectFirst("input[name=noticeauthor]")?.attr("value")?.trim()
                    ?.takeIf { it.isNotEmpty() }
                editor.noticeTrimStr = document.selectFirst("input[name=noticetrimstr]")?.attr("value")
                    ?.takeIf { it.isNotEmpty() }
                editor.noticeAuthorMsg = document.selectFirst("input[name=noticeauthormsg]")?.attr("value")
                    ?.takeIf { it.isNotEmpty() }
                //thread types
                val typeIdElements = document.select("#typeid>option")
                val threadTypes = ArrayList<ThreadType>()
                for (i in typeIdElements.indices) {
                    val element = typeIdElements[i]
                    val typeId = element.attr("value").trim()
                    val typeName = element.text()
                    if ("selected" == element.attr("selected").trim()) {
                        editor.typeIndex = i
                    }
                    threadTypes.add(ThreadType(typeId, typeName))
                }
                editor.threadTypes = threadTypes
                //subject
                editor.subject = document.selectFirst("input#subject, input[name=subject]")?.attr("value")
                //message
                editor.message = document.selectFirst("textarea#e_textarea, textarea[name=message]")?.text()
                //read permission
                val permElements = document.select("#readperm>option")
                val permTypes = hashSetOf<String>()
                var readPerm = ""
                for (i in permElements.indices) {
                    val element = permElements[i]
                    val perm = element.attr("value").trim()
                    if ("selected" == element.attr("selected").trim()) {
                        readPerm = perm
                    }
                    permTypes.add(perm)
                }
                val readPermTypes = permTypes.toList().sortedBy { it.toIntOrNull() }
                editor.readPermIndex = readPermTypes.indexOf(readPerm)
                editor.readPermTypes = readPermTypes
                editor.forumAttachmentUploadInfo = parseForumAttachmentUploadInfo(document)
                editor.forumAttachments = parseForumAttachments(document)
            } catch (e: Exception) {
                L.leaveMsg("Source:$html")
                throw e
            }

            return editor
        }

        fun fromAttachmentListHtml(html: String): List<ForumAttachment> {
            LooperUtil.enforceOnWorkThread()
            val document = Jsoup.parse(unwrapAjaxHtml(html))
            return parseForumAttachments(document)
        }

        private fun unwrapAjaxHtml(html: String): String {
            return Regex("""(?s)<root>\s*<!\[CDATA\[(.*)]]>\s*</root>""")
                .find(html.trim())
                ?.groupValues
                ?.getOrNull(1)
                ?: html
        }

        private fun parseForumAttachmentUploadInfo(document: Document): ForumAttachmentUploadInfo? {
            val contextFid = parseFidFromEditorContext(document)
            readForumAttachmentUploadInfoFromForm(document, contextFid)?.let {
                return it
            }
            document.select("script").forEach { script ->
                val scriptContent = script.data().ifBlank { script.html() }
                val postParams = extractJsObjectProperty(scriptContent, "post_params") ?: return@forEach
                val params = runCatching { jsObjectMapper.readTree(postParams) }.getOrNull() ?: return@forEach
                val hash = params.text("hash")?.takeIf { it.isNotBlank() } ?: return@forEach
                val uid = params.text("uid")
                val uploadUrl = extractJsStringProperty(scriptContent, "upload_url")
                val fid = params.text("fid")?.toIntOrNull()
                    ?: parseFidFromUrl(uploadUrl)
                    ?: contextFid
                return ForumAttachmentUploadInfo(uploadUrl, fid, uid, hash)
            }
            return null
        }

        private fun parseForumAttachments(document: Document): List<ForumAttachment> {
            val attachments = linkedMapOf<String, ForumAttachment>()
            document.select("[id^=attach_]")
                .forEach { element ->
                    val aid = Regex("""^attach_(\d+)$""").find(element.id())?.groupValues?.getOrNull(1)
                        ?: return@forEach
                    val link = document.selectFirst("#attachname$aid, #attachname_$aid")
                    if (link?.attr("isimage") != "1") {
                        return@forEach
                    }
                    val imageUrl = document.selectFirst("#image_$aid")?.attr("src")
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::resolveForumUrl)
                    attachments[aid] = ForumAttachment(
                        aid = aid,
                        fileName = parseAttachmentFileName(link),
                        imageUrl = imageUrl,
                    )
                }
            document.select("input[name=unused[]]").forEach { element ->
                val aid = element.attr("value").trim().takeIf { it.matches(Regex("""\d+""")) } ?: return@forEach
                attachments.putIfAbsent(aid, ForumAttachment(aid, null, null))
            }
            document.select("script").forEach { script ->
                parseUnusedForumAttachments(script.data().ifBlank { script.html() }).forEach { attachment ->
                    attachments.putIfAbsent(attachment.aid, attachment)
                }
            }
            return attachments.values.toList()
        }

        private fun parseUnusedForumAttachments(script: String): List<ForumAttachment> {
            val attachments = linkedMapOf<String, ForumAttachment>()
            val inputRegex = Regex("""<input[^>]*\bname=["']unused\[\]["'][^>]*>|<input[^>]*\bvalue=["']\d+["'][^>]*\bname=["']unused\[\]["'][^>]*>""")
            inputRegex.findAll(script).forEach { match ->
                val input = match.value
                val aid = Regex("""\bvalue=["'](\d+)["']""").find(input)
                    ?.groupValues
                    ?.getOrNull(1)
                    ?: return@forEach
                val fileName = Regex("""<span[^>]*\btitle=["']([^"']*)["']""")
                    .find(script.substring(match.range.last + 1).take(500))
                    ?.groupValues
                    ?.getOrNull(1)
                    ?.takeIf { it.isNotBlank() }
                    ?.lineSequence()
                    ?.firstOrNull()
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                attachments[aid] = ForumAttachment(aid, fileName, null)
            }
            val aidRegex = Regex("""(?:ATTACH|IMG)UNUSEDAID\[(\d+)]""")
            aidRegex.findAll(script).forEach { match ->
                val aid = match.groupValues.getOrNull(1)?.takeIf { it.isNotBlank() } ?: return@forEach
                attachments.putIfAbsent(aid, ForumAttachment(aid, null, null))
            }
            return attachments.values.toList()
        }

        private fun parseAttachmentFileName(link: org.jsoup.nodes.Element?): String? {
            return link?.attr("title")
                ?.lineSequence()
                ?.firstOrNull()
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?: link?.text()?.trim()?.takeIf { it.isNotEmpty() }
        }

        private fun readForumAttachmentUploadInfoFromForm(
            document: Document,
            contextFid: Int?,
        ): ForumAttachmentUploadInfo? {
            val hash = document.selectFirst("input[name=hash]")?.attr("value")?.trim()
                ?.takeIf { it.isNotEmpty() } ?: return null
            val uid = document.selectFirst("input[name=uid]")?.attr("value")?.trim()?.takeIf { it.isNotEmpty() }
            val uploadUrl = document.selectFirst("form[action*=operation=upload]")?.attr("action")
            val fid = document.selectFirst("input[name=fid]")?.attr("value")?.trim()?.toIntOrNull()
                ?: uploadUrl?.let(::parseFidFromUrl)
                ?: contextFid
            return ForumAttachmentUploadInfo(uploadUrl, fid, uid, hash)
        }

        private fun extractJsObjectProperty(script: String, property: String): String? {
            var searchIndex = 0
            while (searchIndex < script.length) {
                val (keyIndex, valueIndex) = findJsPropertyValueStart(script, property, searchIndex) ?: return null
                val objectStart = script.indexOfFirstNonWhitespace(valueIndex)
                if (objectStart >= 0 && script[objectStart] == '{') {
                    return readBalancedBlock(script, objectStart, '{', '}')
                }
                searchIndex = keyIndex + property.length
            }
            return null
        }

        private fun extractJsStringProperty(script: String, property: String): String? {
            var searchIndex = 0
            while (searchIndex < script.length) {
                val (keyIndex, valueIndex) = findJsPropertyValueStart(script, property, searchIndex) ?: return null
                val stringStart = script.indexOfFirstNonWhitespace(valueIndex)
                if (stringStart >= 0 && (script[stringStart] == '\'' || script[stringStart] == '"')) {
                    return readJsString(script, stringStart)
                }
                searchIndex = keyIndex + property.length
            }
            return null
        }

        private fun findJsPropertyValueStart(script: String, property: String, fromIndex: Int): Pair<Int, Int>? {
            var best: Pair<Int, Int>? = null
            listOf("\"$property\"", "'$property'", property).forEach { token ->
                var index = fromIndex
                while (index < script.length) {
                    val keyIndex = script.indexOf(token, index)
                    if (keyIndex < 0) {
                        break
                    }
                    val keyEnd = keyIndex + token.length
                    if (token == property && !script.hasPropertyBoundary(keyIndex, keyEnd)) {
                        index = keyEnd
                        continue
                    }
                    val colonIndex = script.indexOfFirstNonWhitespace(keyEnd)
                    if (colonIndex >= 0 && script[colonIndex] == ':') {
                        val valueIndex = colonIndex + 1
                        if (best == null || keyIndex < best!!.first) {
                            best = keyIndex to valueIndex
                        }
                    }
                    break
                }
            }
            return best
        }

        private fun String.hasPropertyBoundary(start: Int, end: Int): Boolean {
            val before = getOrNull(start - 1)
            val after = getOrNull(end)
            return !before.isJsIdentifierPart() && !after.isJsIdentifierPart()
        }

        private fun Char?.isJsIdentifierPart(): Boolean {
            return this != null && (isLetterOrDigit() || this == '_' || this == '$')
        }

        private fun String.indexOfFirstNonWhitespace(fromIndex: Int): Int {
            for (index in fromIndex until length) {
                if (!this[index].isWhitespace()) {
                    return index
                }
            }
            return -1
        }

        private fun readBalancedBlock(source: String, startIndex: Int, open: Char, close: Char): String? {
            var depth = 0
            var quote: Char? = null
            var escaped = false
            for (index in startIndex until source.length) {
                val char = source[index]
                if (quote != null) {
                    if (escaped) {
                        escaped = false
                    } else if (char == '\\') {
                        escaped = true
                    } else if (char == quote) {
                        quote = null
                    }
                    continue
                }
                when (char) {
                    '\'', '"' -> quote = char
                    open -> depth++
                    close -> {
                        depth--
                        if (depth == 0) {
                            return source.substring(startIndex, index + 1)
                        }
                    }
                }
            }
            return null
        }

        private fun readJsString(source: String, startIndex: Int): String? {
            val quote = source[startIndex]
            val builder = StringBuilder()
            var index = startIndex + 1
            while (index < source.length) {
                val char = source[index]
                if (char == quote) {
                    return normalizeJsString(builder.toString())
                }
                if (char == '\\' && index + 1 < source.length) {
                    val next = source[index + 1]
                    if (next == 'u' && index + 5 < source.length) {
                        val hex = source.substring(index + 2, index + 6)
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            builder.append(code.toChar())
                            index += 6
                            continue
                        }
                    }
                    builder.append(
                        when (next) {
                            'n' -> '\n'
                            'r' -> '\r'
                            't' -> '\t'
                            else -> next
                        }
                    )
                    index += 2
                    continue
                }
                builder.append(char)
                index++
            }
            return null
        }

        private fun normalizeJsString(value: String): String {
            return Parser.unescapeEntities(value.replace("\\/", "/"), true)
        }

        private fun parseFidFromEditorContext(document: Document): Int? {
            document.selectFirst("input[name=fid]")?.attr("value")?.trim()?.toIntOrNull()?.let {
                return it
            }
            document.select("form[action]").forEach { element ->
                parseFidFromUrl(element.attr("action"))?.let {
                    return it
                }
            }
            document.select("script").forEach { script ->
                val scriptContent = script.data().ifBlank { script.html() }
                parseFidFromScript(scriptContent)?.let {
                    return it
                }
            }
            return null
        }

        private fun parseFidFromScript(script: String): Int? {
            val normalized = Parser.unescapeEntities(script, true)
            listOf(
                Regex("""\bfid\s*=\s*parseInt\(\s*['"]?(\d+)['"]?\s*\)"""),
                Regex("""\bfid\s*[:=]\s*['"]?(\d+)['"]?"""),
                Regex("""[?&]fid=(\d+)"""),
            ).forEach { regex ->
                regex.find(normalized)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                    return it
                }
            }
            return null
        }

        private fun parseFidFromUrl(rawUrl: String?): Int? {
            val url = rawUrl?.takeIf { it.isNotBlank() } ?: return null
            val baseUrl = Api.BASE_URL.toHttpUrlOrNull()
            val httpUrl = baseUrl?.resolve(url) ?: url.toHttpUrlOrNull()
            return httpUrl?.queryParameter("fid")?.toIntOrNull()
        }

        private fun resolveForumUrl(url: String): String {
            val baseUrl = Api.BASE_URL.toHttpUrlOrNull()
            val httpUrl = baseUrl?.resolve(url) ?: url.toHttpUrlOrNull()
            if (httpUrl != null && httpUrl.queryParameter("mod") == "image") {
                return httpUrl.newBuilder()
                    .removeAllQueryParameters("nocache")
                    .removeAllQueryParameters("ramdom")
                    .removeAllQueryParameters("random")
                    .build()
                    .toString()
            }
            return httpUrl?.toString() ?: url
        }

        private fun JsonNode.text(field: String): String? {
            return get(field)?.asText()?.trim()?.takeIf { it.isNotEmpty() }
        }

        private val jsObjectMapper: ObjectMapper by lazy {
            JsonUtil.jsonMapper.copy()
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        }
    }
}
