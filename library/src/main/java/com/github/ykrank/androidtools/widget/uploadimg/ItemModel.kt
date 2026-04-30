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

class ModelImageUpload(val media: LocalMedia?) : DiffSameItem, Parcelable, StableIdModel {

    var url: String? = null
    var deleteUrl: String? = null

    var aid: String? = null  // 附件ID
    var tid: Int? = null     // 主题ID
    var pid: Int? = null     // 帖子ID

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
        parcel.readParcelable<LocalMedia>(LocalMedia::class.java.classLoader)
    ) {
        url = parcel.readString()
        deleteUrl = parcel.readString()
        state = parcel.readInt()
        aid = parcel.readString()
        tid = parcel.readValue(Int::class.java.classLoader) as? Int
        pid = parcel.readValue(Int::class.java.classLoader) as? Int
    }

    override val stableId: Long
        get() = localUri.hashCode().toLong()

    override fun isSameItem(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelImageUpload

        if (media?.uri != other.media?.uri) return false

        return true
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(media, flags)
        parcel.writeString(url)
        parcel.writeString(deleteUrl)
        parcel.writeInt(state)
        parcel.writeString(aid)
        parcel.writeValue(tid)
        parcel.writeValue(pid)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModelImageUpload

        if (media != other.media) return false
        if (url != other.url) return false
        if (deleteUrl != other.deleteUrl) return false
        if (state != other.state) return false
        if (aid != other.aid) return false
        if (tid != other.tid) return false
        if (pid != other.pid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = media.hashCode()
        result = 31 * result + (url?.hashCode() ?: 0)
        result = 31 * result + (deleteUrl?.hashCode() ?: 0)
        result = 31 * result + state
        result = 31 * result + (aid?.hashCode() ?: 0)
        result = 31 * result + (tid?.hashCode() ?: 0)
        result = 31 * result + (pid?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ModelImageUpload(media=$media, url=$url, deleteUrl=$deleteUrl, state=$state, aid=$aid, tid=$tid, pid=$pid)"
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