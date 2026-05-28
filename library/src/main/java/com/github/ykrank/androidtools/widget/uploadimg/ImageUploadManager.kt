package com.github.ykrank.androidtools.widget.uploadimg

import java.io.File
import java.io.FileDescriptor

interface ImageUploadManager {
    suspend fun uploadImage(imageFile: File): ImageUpload

    suspend fun uploadImage(imageFile: FileDescriptor): ImageUpload

    suspend fun uploadImage(imageFile: FileDescriptor, metadata: ImageUploadMetadata): ImageUpload {
        return uploadImage(imageFile)
    }

    suspend fun delUploadedImage(url: String): ImageDelete
}

data class ImageUploadMetadata(
    val fileName: String? = null,
    val mimeType: String? = null,
    val size: Long? = null,
)
