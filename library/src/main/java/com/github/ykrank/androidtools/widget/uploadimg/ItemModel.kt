package com.github.ykrank.androidtools.widget.uploadimg

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import com.github.ykrank.androidtools.ui.adapter.StableIdModel
import com.github.ykrank.androidtools.ui.adapter.model.DiffSameItem
import com.github.ykrank.androidtools.widget.imagepicker.LocalMedia

internal class ModelImageUploadAdd : StableIdModel {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

class ModelImageUpload(val media: LocalMedia?, val remoteId: String? = null) : DiffSameItem, Parcelable, StableIdModel {

    var url: String? = null
    var deleteUrl: String? = null
    var insertText: String? = null

    var state: Int = STATE_INIT

    val localUri: Uri?
        get() = if (media == null) {
            null
        } else if (!media.isCompressed) {
            media.uri
        } else {
            media.compressPath
        }

    constructor(parcel: Parcel) : this(
        parcel.readParcelable<LocalMedia>(LocalMedia::class.java.classLoader),
        parcel.readString()
    ) {
        url = parcel.readString()
        deleteUrl = parcel.readString()
        insertText = parcel.readString()
        state = parcel.readInt()
    }

    override val stableId: Long
        get() = (remoteId ?: localUri ?: url).hashCode().toLong()

    override fun isSameItem(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelImageUpload

        if (remoteId != null || other.remoteId != null) {
            return remoteId == other.remoteId
        }
        if (media?.uri != other.media?.uri) return false

        return true
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(media, flags)
        parcel.writeString(remoteId)
        parcel.writeString(url)
        parcel.writeString(deleteUrl)
        parcel.writeString(insertText)
        parcel.writeInt(state)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelImageUpload

        if (media != other.media) return false
        if (remoteId != other.remoteId) return false
        if (url != other.url) return false
        if (deleteUrl != other.deleteUrl) return false
        if (insertText != other.insertText) return false
        if (state != other.state) return false

        return true
    }

    override fun hashCode(): Int {
        var result = media.hashCode()
        result = 31 * result + (remoteId?.hashCode() ?: 0)
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (deleteUrl?.hashCode() ?: 0)
        result = 31 * result + (insertText?.hashCode() ?: 0)
        result = 31 * result + state
        return result
    }

    override fun toString(): String {
        return "ModelImageUpload(media=$media, remoteId=$remoteId, url=$url, deleteUrl=$deleteUrl, insertText=$insertText, state=$state)"
    }

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<ModelImageUpload> {
            override fun createFromParcel(parcel: Parcel): ModelImageUpload {
                return ModelImageUpload(parcel)
            }

            override fun newArray(size: Int): Array<ModelImageUpload?> {
                return arrayOfNulls(size)
            }
        }

        const val STATE_INIT = 0
        const val STATE_UPLOADING = 1
        const val STATE_DONE = 2
        const val STATE_ERROR = 3
    }
}
