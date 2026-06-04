package me.ykrank.s1next.widget.uploadimg

import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.widget.uploadimg.ImageDelete
import com.github.ykrank.androidtools.widget.uploadimg.ImageUpload
import com.github.ykrank.androidtools.widget.uploadimg.ImageUploadManager
import com.github.ykrank.androidtools.widget.uploadimg.ImageUploadMetadata
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.S1Service
import me.ykrank.s1next.data.api.model.PostEditor
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileDescriptor
import java.util.Locale

class ForumAttachmentUploadManager(
    private val target: ForumAttachmentUploadTarget,
    private val user: User,
    private val s1Service: S1Service,
    private val parsedUploadInfoProvider: () -> PostEditor.ForumAttachmentUploadInfo? = { null },
    private val formHashProvider: () -> String? = { null },
) : ImageUploadManager {

    private var uploadInfoCache: ForumAttachmentUploadInfo? = null

    override suspend fun uploadImage(imageFile: File): ImageUpload {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val metadata = ImageUploadMetadata(imageFile.name, null, imageFile.length())
        return upload(requestFile, metadata)
    }

    override suspend fun uploadImage(imageFile: FileDescriptor): ImageUpload {
        return uploadImage(imageFile, ImageUploadMetadata())
    }

    override suspend fun uploadImage(imageFile: FileDescriptor, metadata: ImageUploadMetadata): ImageUpload {
        val requestFile = imageFile.toRequestBody((metadata.mimeType ?: "image/*").toMediaTypeOrNull())
        return upload(requestFile, metadata)
    }

    override suspend fun delUploadedImage(url: String): ImageDelete {
        return runCatching {
            val aid = url.takeIf { it.matches(Regex("""\d+""")) }
                ?: throw IllegalStateException("无效的论坛附件")
            val formHash = formHashProvider()?.takeIf { it.isNotBlank() }
                ?: user.authenticityToken?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("无法获取删除参数")
            val deletedCount = parseDeleteCount(
                s1Service.deleteForumAttachment(
                    formHash = formHash,
                    tid = target.deleteThreadId(),
                    pid = target.deletePostId(),
                    aids = listOf(aid),
                )
            )
            ImageDelete().apply {
                success = deletedCount > 0
                msg = if (success) {
                    "图片已删除"
                } else {
                    "图片删除失败"
                }
            }
        }.getOrElse { error ->
            L.report(error)
            ImageDelete().apply {
                success = false
                msg = error.message ?: "图片删除失败"
            }
        }
    }

    private suspend fun upload(image: RequestBody, metadata: ImageUploadMetadata): ImageUpload {
        return runCatching {
            val info = loadUploadInfo()
            val uid = info.uid ?: user.uid ?: throw IllegalStateException("请先登录后再上传图片")
            val mimeType = metadata.mimeType ?: "image/jpeg"
            val fileName = metadata.fileName?.takeIf { it.isNotBlank() } ?: defaultFileName(mimeType)
            val fileType = fileName.substringAfterLast('.', "")
                .takeIf { it.isNotBlank() }
                ?: mimeType.substringAfter('/', "jpg")
            val uploadId = "WU_FILE_0"
            val file = MultipartBody.Part.createFormData("Filedata", fileName, image)
            val aid = parseUploadAid(
                response = s1Service.uploadForumAttachment(
                        uploadUrl = info.uploadUrl,
                        origin = origin(),
                        referer = referer(info),
                        uid = uid.toPlainBody(),
                        hash = info.hash.toPlainBody(),
                        id = uploadId.toPlainBody(),
                        type = mimeType.toPlainBody(),
                        size = (metadata.size ?: 0L).toString().toPlainBody(),
                        fileType = fileType.lowercase(Locale.US).toPlainBody(),
                        file = file,
                    )
            )
            val imageUrl = loadUploadedImageUrl(aid, info.fid, uploadId)
                ?: throw IllegalStateException("无法获取论坛图片地址")
            ImageUpload().apply {
                success = true
                msg = "success"
                url = imageUrl
                deleteUrl = aid.toString()
                insertText = "[attachimg]$aid[/attachimg]"
            }
        }.getOrElse { error ->
            L.report(error)
            ImageUpload().apply {
                success = false
                msg = error.message ?: "图片上传准备失败"
            }
        }
    }

    private fun loadUploadInfo(): ForumAttachmentUploadInfo {
        uploadInfoCache?.let {
            return it
        }
        return parsedUploadInfoProvider()?.toUploadInfo(target.fallbackFid())?.also {
            uploadInfoCache = it
        } ?: throw IllegalStateException("无法获取论坛附件上传参数")
    }

    private fun PostEditor.ForumAttachmentUploadInfo.toUploadInfo(fallbackFid: Int?): ForumAttachmentUploadInfo {
        val resolvedFid = this.fid
            ?: parseFidFromUploadUrl(uploadUrl)
            ?: fallbackFid
            ?: throw IllegalStateException("无法获取论坛附件上传版块")
        val resolvedUploadUrl = uploadUrl?.takeIf { it.isNotBlank() } ?: defaultUploadUrl(resolvedFid)
        return ForumAttachmentUploadInfo(resolvedUploadUrl, resolvedFid, uid, hash)
    }

    private fun ForumAttachmentUploadTarget.fallbackFid(): Int? {
        return when (this) {
            is ForumAttachmentUploadTarget.NewThread -> fid
            is ForumAttachmentUploadTarget.Reply -> fid
            is ForumAttachmentUploadTarget.EditPost -> fid
        }
    }

    private fun parseUploadAid(response: String): Long {
        val result = response.trim()
        val parts = result.split("|")
        val numericResult = result.toLongOrNull()
        return numericResult?.takeIf { it > 0 }
            ?: parts.getOrNull(2)
                ?.takeIf { parts.firstOrNull() == "DISCUZUPLOAD" && parts.getOrNull(1) == "0" }
                ?.toLongOrNull()
                ?.takeIf { it > 0 }
            ?: throw IllegalStateException(uploadErrorMessage(parts, response))
    }

    private fun parseDeleteCount(response: String): Int {
        val text = Regex("""(?s)<!\[CDATA\[(.*)]]>""")
            .find(response)
            ?.groupValues
            ?.getOrNull(1)
            ?: response
        return text.trim().toIntOrNull() ?: 0
    }

    private fun ForumAttachmentUploadTarget.deleteThreadId(): String {
        return when (this) {
            is ForumAttachmentUploadTarget.NewThread -> "0"
            is ForumAttachmentUploadTarget.Reply -> tid
            is ForumAttachmentUploadTarget.EditPost -> tid.toString()
        }
    }

    private fun ForumAttachmentUploadTarget.deletePostId(): Int {
        return when (this) {
            is ForumAttachmentUploadTarget.EditPost -> pid
            else -> 0
        }
    }

    private suspend fun loadUploadedImageUrl(aid: Long, fid: Int, uploadId: String): String? {
        val html = s1Service.getForumAttachmentList(aid.toString(), fid, uploadId)
        return PostEditor.fromAttachmentListHtml(html)
            .firstOrNull { it.aid == aid.toString() }
            ?.imageUrl
    }

    private fun uploadErrorMessage(parts: List<String>, response: String): String {
        val rawReason = parts.drop(1).firstOrNull { it.isNotBlank() && it != "0" }
        return when (rawReason) {
            "ban" -> "附件类型被禁止"
            "perday" -> "今日附件上传额度不足"
            "-1" -> "图片上传失败"
            null -> "图片上传失败：${response.abbreviateForToast()}"
            else -> "图片上传失败：$rawReason"
        }
    }

    private fun defaultFileName(mimeType: String): String {
        val suffix = when (mimeType.lowercase(Locale.US)) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            else -> "jpg"
        }
        return "image.$suffix"
    }

    private fun defaultUploadUrl(fid: Int): String {
        return "${Api.BASE_URL}misc.php?mod=swfupload&action=swfupload&operation=upload&fid=$fid"
    }

    private fun parseFidFromUploadUrl(uploadUrl: String?): Int? {
        val url = uploadUrl?.takeIf { it.isNotBlank() } ?: return null
        val baseUrl = Api.BASE_URL.toHttpUrlOrNull()
        val httpUrl = baseUrl?.resolve(url) ?: url.toHttpUrlOrNull()
        return httpUrl?.queryParameter("fid")?.toIntOrNull()
    }

    private fun referer(info: ForumAttachmentUploadInfo): String {
        return when (target) {
            is ForumAttachmentUploadTarget.NewThread ->
                "${Api.BASE_URL}forum.php?mod=post&action=newthread&fid=${target.fid}&inajax=yes"

            is ForumAttachmentUploadTarget.Reply ->
                "${Api.BASE_URL}forum.php?mod=post&action=reply&fid=${info.fid}&tid=${target.tid}&inajax=yes"

            is ForumAttachmentUploadTarget.EditPost ->
                "${Api.BASE_URL}forum.php?mod=post&action=edit&fid=${target.fid}&tid=${target.tid}&pid=${target.pid}&inajax=yes"
        }
    }

    private fun origin(): String {
        val url = Api.BASE_URL.toHttpUrlOrNull() ?: return Api.BASE_URL.trimEnd('/')
        val defaultPort = if (url.scheme == "https") 443 else 80
        val port = if (url.port == defaultPort) "" else ":${url.port}"
        return "${url.scheme}://${url.host}$port"
    }

    private fun String.toPlainBody(): RequestBody {
        return toRequestBody("text/plain".toMediaTypeOrNull())
    }

    private fun String.abbreviateForToast(): String {
        return replace("\\s+".toRegex(), " ").trim().take(80)
    }
}

private data class ForumAttachmentUploadInfo(
    val uploadUrl: String,
    val fid: Int,
    val uid: String?,
    val hash: String,
)
