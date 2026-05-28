package me.ykrank.s1next.widget.uploadimg

import com.github.ykrank.androidtools.widget.uploadimg.ImageDelete
import com.github.ykrank.androidtools.widget.uploadimg.ImageUpload
import com.github.ykrank.androidtools.widget.uploadimg.ImageUploadManager
import me.ykrank.s1next.widget.uploadimg.model.RIPImageUpload
import okhttp3.ExperimentalOkHttpApi
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.File
import java.io.FileDescriptor
import java.util.concurrent.TimeUnit

//赞美坛友R.I.P提供的图库服务
class RIPImageUploadManager(_okHttpClient: OkHttpClient? = null) : ImageUploadManager {

    private val okHttpClient: OkHttpClient by lazy {
        _okHttpClient ?: OkHttpClient.Builder()
                .connectTimeout(17, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build()
    }

    private val uploadApiService: RIPImageUploadApiService by lazy {
        Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl("https://p.sda1.dev/api/v1/")
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .build()
                .create(RIPImageUploadApiService::class.java)
    }

    /**
     * Force upload to sm.ms
     */
    override suspend fun uploadImage(imageFile: File): ImageUpload {
        val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
        return uploadApiService.postRIPImage(imageFile.name, requestFile).toCommon()
    }

    @OptIn(ExperimentalOkHttpApi::class)
    override suspend fun uploadImage(imageFile: FileDescriptor): ImageUpload {
        val requestFile = imageFile.toRequestBody("image/*".toMediaTypeOrNull())
        return uploadApiService.postRIPImage("image.jpg", requestFile).toCommon()
    }

    override suspend fun delUploadedImage(url: String): ImageDelete {
        return ImageDelete().apply { success = true }
    }
}

interface RIPImageUploadApiService {
    @POST("https://p.sda1.dev/api/v1/upload_external_noform")
    suspend fun postRIPImage(@Query("filename") fileName: String, @Body image: RequestBody?): RIPImageUpload
}
