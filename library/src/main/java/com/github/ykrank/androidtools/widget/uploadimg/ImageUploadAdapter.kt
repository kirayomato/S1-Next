package com.github.ykrank.androidtools.widget.uploadimg

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.ykrank.androidtools.binding.LibImageViewBindingAdapter
import com.github.ykrank.androidtools.databinding.ItemUploadedImageAddBinding
import com.github.ykrank.androidtools.databinding.ItemUploadedImageBinding
import com.github.ykrank.androidtools.ui.adapter.LibBaseRecyclerViewAdapter
import com.github.ykrank.androidtools.ui.adapter.delegate.LibBaseAdapterDelegate

class ImageUploadAdapter(fragment: LibImageUploadFragment, imageClickListener: ((View, ModelImageUpload) -> Unit)? = null) : LibBaseRecyclerViewAdapter(fragment.requireContext()) {

    init {
        val context = fragment.requireContext()
        addAdapterDelegate(UploadedImageAdapterDelegate(context, fragment::delPickedImage, imageClickListener))
        addAdapterDelegate(UploadedImageAddAdapterDelegate(context, fragment::startPickImage))
    }

    private class UploadedImageAdapterDelegate(
        context: android.content.Context,
        private val onDeleteImage: (ModelImageUpload?) -> Unit,
        private val imageClickListener: ((View, ModelImageUpload) -> Unit)?,
    ) : LibBaseAdapterDelegate<ModelImageUpload, UploadedImageViewHolder>(
        context,
        ModelImageUpload::class.java,
    ) {

        override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            val binding = ItemUploadedImageBinding.inflate(mLayoutInflater, parent, false)
            return UploadedImageViewHolder(binding, onDeleteImage, imageClickListener)
        }

        override fun onBindViewHolderData(
            t: ModelImageUpload,
            position: Int,
            holder: UploadedImageViewHolder,
            payloads: List<Any>,
        ) {
            holder.bind(t)
        }
    }

    private class UploadedImageAddAdapterDelegate(
        context: android.content.Context,
        private val onAddImage: () -> Unit,
    ) : LibBaseAdapterDelegate<ModelImageUploadAdd, UploadedImageAddViewHolder>(
        context,
        ModelImageUploadAdd::class.java,
    ) {

        override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
            val binding = ItemUploadedImageAddBinding.inflate(mLayoutInflater, parent, false)
            return UploadedImageAddViewHolder(binding, onAddImage)
        }

        override fun onBindViewHolderData(
            t: ModelImageUploadAdd,
            position: Int,
            holder: UploadedImageAddViewHolder,
            payloads: List<Any>,
        ) = Unit
    }

    private class UploadedImageViewHolder(
        private val binding: ItemUploadedImageBinding,
        private val onDeleteImage: (ModelImageUpload?) -> Unit,
        imageClickListener: ((View, ModelImageUpload) -> Unit)?,
    ) : RecyclerView.ViewHolder(binding.root) {
        private var model: ModelImageUpload? = null

        init {
            binding.ivClose.setOnClickListener {
                onDeleteImage(model)
            }
            if (imageClickListener != null) {
                binding.image.setOnClickListener { view ->
                    model?.let { imageClickListener(view, it) }
                }
            }
        }

        fun bind(model: ModelImageUpload) {
            this.model = model
            val remoteUrl = if (model.state == ModelImageUpload.STATE_DONE) model.url else null
            LibImageViewBindingAdapter.loadImageNetLocal(binding.image, remoteUrl, model.localUri)
            binding.progress.visibility = if (model.state == ModelImageUpload.STATE_DONE) {
                View.GONE
            } else {
                View.VISIBLE
            }
        }
    }

    private class UploadedImageAddViewHolder(
        binding: ItemUploadedImageAddBinding,
        onAddImage: () -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.ivCorners.setOnClickListener {
                onAddImage()
            }
        }
    }
}
