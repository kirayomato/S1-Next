package me.ykrank.s1next.widget.uploadimg

import com.fasterxml.jackson.core.JsonProcessingException
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.widget.uploadimg.ImageDelete
import com.github.ykrank.androidtools.widget.uploadimg.ImageUpload
import com.github.ykrank.androidtools.widget.uploadimg.ImageUploadManager
import io.reactivex.Single
import java.io.File
import java.io.FileDescriptor
import java.util.concurrent.TimeUnit
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.Api
import me.ykrank.s1next.data.api.S1Service
import me.ykrank.s1next.data.api.model.ForumUploadConfig
import me.ykrank.s1next.widget.uploadimg.model.DiscuzUploadResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Url

class ForumImageUploadManager(
    _okHttpClient: OkHttpClient? = null,
    private val user: User? = null,
    private var fid: Int = 0,
    private val s1Service: S1Service? = null,
) : ImageUploadManager {

    private val okHttpClient: OkHttpClient by lazy {
        val builder = _okHttpClient?.newBuilder() ?: OkHttpClient.Builder()
        builder
            .connectTimeout(17, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                L.d("🌐 Upload Request: ${request.url}")

                val requestBody = request.body
                if (requestBody is MultipartBody) {
                    L.d("📤 Upload Parts:")
                    requestBody.parts.forEachIndexed { index, part ->
                        val disposition = part.headers?.get("Content-Disposition")
                        val size = part.body.contentLength()
                        L.d("   Part $index: $disposition (${size}bytes)")
                    }
                }

                val response = chain.proceed(request)

                val body = response.peekBody(1024 * 1024)
                val bodyStr = body.string()

                L.d("📥 Response Code: ${response.code}")
                L.d("📥 Response Body: $bodyStr")

                response
            }
            .build()
    }

    private val uploadApiService: DiscuzUploadApiService by lazy {
        Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(Api.BASE_URL)
            .addConverterFactory(JacksonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(DiscuzUploadApiService::class.java)
    }

    fun setFid(fid: Int) {
        this.fid = fid
        L.d("📝 Set fid: $fid")
    }

    override fun uploadImage(imageFile: File): Single<ImageUpload> {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("Filedata", imageFile.name, requestFile)
        return uploadWithHash(filePart, imageFile.name, imageFile.length())
    }

    @OptIn(okhttp3.ExperimentalOkHttpApi::class)
    override fun uploadImage(imageFile: FileDescriptor): Single<ImageUpload> {
        val requestFile = imageFile.toRequestBody("image/*".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("Filedata", "image.jpg", requestFile)
        return uploadWithHash(filePart, "image.jpg", requestFile.contentLength())
    }

    private fun uploadWithHash(
        filePart: MultipartBody.Part,
        filename: String,
        fileSize: Long,
    ): Single<ImageUpload> {
        val uid = user?.uid
        if (uid.isNullOrEmpty()) {
            L.e("❌ 用户未登录，uid为空")
            return Single.just(ImageUpload().apply {
                success = false
                msg = "未登录，请先登录"
            })
        }

        val uploadConfig = user?.forumUploadConfig
        val hash = uploadConfig?.hash

        if (hash.isNullOrEmpty()) {
            L.w("⚠️ hash为空，尝试更新上传配置")
            val currentFid = 151
            return s1Service?.getNewThreadInfo(currentFid)
                ?.map { html -> ForumUploadConfig.fromHtml(html) }
                ?.doOnSuccess { config ->
                    if (config != null) {
                        user.forumUploadConfig = config
                        L.d("✅ 已更新上传配置: hash=${config.hash}, fid=${config.fid}")
                    }
                }
                ?.flatMap { config ->
                    if (config.hash.isNullOrEmpty()) {
                        L.e("❌ 更新后 hash 仍为空")
                        Single.just(ImageUpload().apply {
                            success = false
                            msg = "未找到上传参数，请先打开发帖或回复页面"
                        })
                    } else {
                        uploadWithHashInternal(filePart, filename, fileSize, config!!)
                    }
                }
                ?: run {
                    L.e("❌ 未找到上传 hash 且 S1Service 为空")
                    Single.just(ImageUpload().apply {
                        success = false
                        msg = "未找到上传参数，请先打开发帖或回复页面"
                    })
                }
        }

        return uploadWithHashInternal(filePart, filename, fileSize, uploadConfig!!)
    }

    private fun uploadWithHashInternal(
        filePart: MultipartBody.Part,
        filename: String,
        fileSize: Long,
        uploadConfig: ForumUploadConfig,
    ): Single<ImageUpload> {
        val hash = uploadConfig.hash!!

        val uploadFid = if (uploadConfig.fid != 0) uploadConfig.fid else fid

        val ext = filename.substringAfterLast('.', "").let {
            if (it.isNotEmpty()) it else "jpg"
        }

        L.d("🔑 Using hash: '$hash' (uid: ${user?.uid}, fid: $uploadFid)")

        val uidPart = user!!.uid!!.toRequestBody("text/plain".toMediaTypeOrNull())
        val hashPart = hash.toRequestBody("text/plain".toMediaTypeOrNull())
        val typePart = "image".toRequestBody("text/plain".toMediaTypeOrNull())
        val idPart = "WU_FILE_0".toRequestBody("text/plain".toMediaTypeOrNull())
        val sizePart = fileSize.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val filetypePart = ext.toRequestBody("text/plain".toMediaTypeOrNull())

        val uploadUrl = uploadConfig.uploadUrl ?: "misc.php?mod=swfupload&action=swfupload&operation=upload&fid=${uploadFid ?: 0}"

        return uploadApiService.uploadImage(
            url = uploadUrl,
            uid = uidPart,
            hash = hashPart,
            type = typePart,
            id = idPart,
            size = sizePart,
            filetype = filetypePart,
            file = filePart,
        ).map { responseBody ->
            val responseStr = responseBody.string()
            L.d("📥 原始响应: $responseStr")
            
            // 尝试解析响应
            val uploadResponse = try {
                // 先尝试作为纯数字解析（服务器可能直接返回附件ID）
                val aid = responseStr.trim().toLongOrNull()
                if (aid != null && aid > 0) {
                    // 纯数字，认为是附件ID
                    DiscuzUploadResponse().apply {
                        this.aid = aid.toString()
                    }
                } else {
                    // 尝试作为 JSON 解析
                    val objectMapper = com.fasterxml.jackson.databind.ObjectMapper()
                    objectMapper.readValue(responseStr, DiscuzUploadResponse::class.java)
                }
            } catch (e: Exception) {
                L.e("❌ 解析响应失败: $responseStr", e)
                DiscuzUploadResponse.fromError("解析响应失败: ${e.message}")
            }
            
            if (uploadResponse.aid.isNullOrEmpty() && uploadResponse.error.isNullOrEmpty()) {
                throw Exception("服务器返回数据格式错误: $responseStr")
            }
            if (uploadResponse.success) {
                L.d("✅ 上传成功: aid=${uploadResponse.aid}")
            } else {
                L.e("❌ 上传失败: error=${uploadResponse.error}")
            }
            uploadResponse.toCommon()
        }.onErrorReturn { throwable ->
            L.report(throwable)
            val errorMsg = when {
                throwable is retrofit2.HttpException -> {
                    "上传失败: HTTP ${throwable.code()} - ${throwable.message()}"
                }
                throwable is java.io.IOException -> {
                    "上传失败: 网络连接错误 - ${throwable.message ?: "请检查网络"}"
                }
                throwable is JsonProcessingException -> {
                    "上传失败: 数据解析错误 - ${throwable.message ?: "服务器返回格式异常"}"
                }
                throwable.message?.contains("No content to map", ignoreCase = true) == true -> {
                    "上传失败: 服务器返回为空"
                }
                throwable.message?.contains("end-of-input", ignoreCase = true) == true -> {
                    "上传失败: 服务器返回不完整"
                }
                else -> "上传失败: ${throwable.message ?: "未知错误"}"
            }
            ImageUpload().apply {
                success = false
                msg = errorMsg
            }
        }
    }

    override fun delUploadedImage(url: String): Single<ImageDelete> {
        return Single.just(ImageDelete().apply { success = true })
    }
}

interface DiscuzUploadApiService {
    @Multipart
    @POST
    fun uploadImage(
        @Url url: String,
        @Part("uid") uid: okhttp3.RequestBody?,
        @Part("hash") hash: okhttp3.RequestBody?,
        @Part("type") type: okhttp3.RequestBody?,
        @Part("id") id: okhttp3.RequestBody?,
        @Part("size") size: okhttp3.RequestBody?,
        @Part("filetype") filetype: okhttp3.RequestBody?,
        @Part file: MultipartBody.Part,
    ): Single<ResponseBody>
}
