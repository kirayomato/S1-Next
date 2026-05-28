package com.github.ykrank.androidtools.widget.uploadimg

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.annotation.MainThread
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.databinding.FragmentUploadedImageBinding
import com.github.ykrank.androidtools.extension.toast
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.widget.imagepicker.LibImagePickerFragment
import com.github.ykrank.androidtools.widget.imagepicker.LocalMedia
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

open class LibImageUploadFragment : LibImagePickerFragment() {

    private lateinit var binding: FragmentUploadedImageBinding
    private lateinit var adapter: ImageUploadAdapter

    protected lateinit var imageUploadManager: ImageUploadManager

    val images = arrayListOf<ModelImageUpload>()
    private val modelAdd = ModelImageUploadAdd()

    //Call onCreateView
    open val imageClickListener: ((View, ModelImageUpload) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentUploadedImageBinding.inflate(inflater, container, false)

        adapter = ImageUploadAdapter(this, imageClickListener)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = GridLayoutManager(
            context,
            imageGridSpanCount(),
            RecyclerView.VERTICAL,
            false
        )

        if (savedInstanceState != null) {
            images.clear()
            val list = savedInstanceState.getParcelableArrayList<ModelImageUpload>(Extras_Upload_Images)
            if (list != null) {
                images.addAll(list)
            }
            images.forEach {
                //初始化下载状态
                if (it.state != ModelImageUpload.STATE_DONE) {
                    it.state = ModelImageUpload.STATE_INIT
                }
            }
        } else {
            images.clear()
        }
        refreshDataSet()

        imageUploadManager = provideImageUploadManager()
        createUploadOptionsView(inflater, binding.uploadOptionsContainer)?.let {
            binding.uploadOptionsContainer.visibility = View.VISIBLE
            binding.uploadOptionsContainer.addView(it)
        }

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelableArrayList(Extras_Upload_Images, images)
    }

    private fun refreshDataSet() {
        val dataset = mutableListOf<Any>()
        dataset.addAll(images)
        dataset.add(modelAdd)
        adapter.refreshDataSet(dataset, true)
    }

    protected fun addUploadedImages(uploadedImages: List<ModelImageUpload>) {
        if (uploadedImages.isEmpty()) {
            return
        }
        val existingKeys = images.mapNotNull { it.remoteId ?: it.url ?: it.localUri?.toString() }.toHashSet()
        uploadedImages.forEach { image ->
            val key = image.remoteId ?: image.url ?: image.localUri?.toString()
            if (key != null && existingKeys.add(key)) {
                images.add(image)
            }
        }
        refreshDataSet()
    }

    override fun afterPickImage(medias: List<LocalMedia>) {
        medias.map { ModelImageUpload(it) }
            .apply {
                //仅添加不同路径的图片
                val pathSet = hashSetOf<String?>()
                images.forEach { it.localUri?.apply { pathSet.add(this.path) } }
                this.forEach {
                    val path = it.localUri?.path
                    if (path != null && !pathSet.contains(path)) {
                        images.add(it)
                        pathSet.add(path)
                    }
                }

                refreshDataSet()
                uploadPickedImage()
            }
    }

    open fun provideImageUploadManager(): ImageUploadManager {
        return SmmsImageUploadManager()
    }

    open fun createUploadOptionsView(inflater: LayoutInflater, container: ViewGroup): View? {
        return null
    }

    protected open fun imageGridSpanCount(): Int {
        val displayMetrics = resources.displayMetrics
        val widthDp = displayMetrics.widthPixels / displayMetrics.density
        return (widthDp / 88f).toInt().coerceIn(3, 5)
    }

    private fun uploadPickedImage() {
        val appContext = requireContext().applicationContext
        images.filter { it.state == ModelImageUpload.STATE_INIT }
            .forEach { model ->
                val uploadManager = imageUploadManager
                model.state = ModelImageUpload.STATE_UPLOADING
                notifyModelChanged(model)
                lifecycleScope.launch {
                    val upload = runCatching {
                        withContext(uploadDispatcher) {
                            uploadImage(appContext, uploadManager, model)
                        }
                    }.getOrElse { error ->
                        if (error is CancellationException) {
                            throw error
                        }
                        L.report(error)
                        ImageUpload().apply {
                            success = false
                            msg = error.message
                        }
                    }
                    handleUploadResult(model, upload)
                }
            }
    }

    private suspend fun uploadImage(
        context: Context,
        uploadManager: ImageUploadManager,
        model: ModelImageUpload,
    ): ImageUpload? {
        val uri = model.localUri ?: return null
        return context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { assetFileDescriptor ->
            val metadata = buildUploadMetadata(context, uri, assetFileDescriptor)
            uploadManager.uploadImage(assetFileDescriptor.fileDescriptor, metadata)
        }
    }

    private fun handleUploadResult(model: ModelImageUpload, upload: ImageUpload?) {
        L.d(upload.toString())
        if (upload?.success == true) {
            model.state = ModelImageUpload.STATE_DONE
            model.url = upload.url
            model.deleteUrl = upload.deleteUrl
            model.insertText = upload.insertText
        } else {
            model.state = ModelImageUpload.STATE_ERROR
            context?.toast(upload?.msg)
            L.report(ImageUploadError("Upload image error: $model, $upload"))
        }
        adapter.dataSet.indexOf(model).also { index ->
            if (index >= 0) {
                adapter.notifyItemChanged(index)
            } else {
                // If image removed from list, remove it from server.
                delPickedImage(model)
            }
        }
    }

    private fun notifyModelChanged(model: ModelImageUpload) {
        adapter.dataSet.indexOf(model).also { index ->
            if (index >= 0) {
                adapter.notifyItemChanged(index)
            }
        }
    }

    private fun buildUploadMetadata(
        context: Context,
        uri: Uri,
        assetFileDescriptor: AssetFileDescriptor?,
    ): ImageUploadMetadata {
        val resolver = context.contentResolver
        val queried = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) {
                        return@use null
                    }
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val name = if (nameIndex >= 0) cursor.getString(nameIndex) else null
                    val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else null
                    name to size
                }
        }.getOrNull()
        val fileName = queried?.first ?: uri.lastPathSegment?.substringAfterLast('/')
        val size = assetFileDescriptor?.length?.takeIf { it >= 0 } ?: queried?.second
        val mimeType = resolver.getType(uri) ?: fileName?.substringAfterLast('.', "")
            ?.takeIf { it.isNotEmpty() }
            ?.let { MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.lowercase()) }
        return ImageUploadMetadata(fileName, mimeType, size)
    }

    @MainThread
    fun delPickedImage(model: ModelImageUpload?) {
        if (model != null) {
            val deleteUrl = model.deleteUrl
            if (deleteUrl == null) {
                removeUploadedImage(model)
            } else {
                lifecycleScope.launch {
                    val delete = runCatching {
                        withContext(uploadDispatcher) {
                            imageUploadManager.delUploadedImage(deleteUrl)
                        }
                    }.getOrElse { error ->
                        if (error is CancellationException) {
                            throw error
                        }
                        L.report(error)
                        null
                    }
                    removeUploadedImage(model)
                    delete?.let {
                        context?.toast(it.msg)
                        if (!it.success) {
                            L.report(ImageUploadError("Delete image error: $model, $it"))
                        }
                    }
                }
            }
        }
    }

    @MainThread
    protected fun removeUploadedImage(model: ModelImageUpload) {
        images.remove(model)
        adapter.dataSet.indexOf(model).also {
            if (it >= 0) {
                adapter.removeItem(it)
                adapter.notifyItemRemoved(it)
            }
        }
    }

    companion object {
        val TAG = LibImageUploadFragment::class.java.simpleName

        val Extras_Upload_Images = "extras_upload_images"

        val uploadExecutor = ThreadPoolExecutor(1, 3, 1, TimeUnit.SECONDS, LinkedBlockingDeque(32))
        val uploadDispatcher = uploadExecutor.asCoroutineDispatcher()
    }
}
