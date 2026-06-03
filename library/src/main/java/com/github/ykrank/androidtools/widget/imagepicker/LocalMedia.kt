package com.github.ykrank.androidtools.widget.imagepicker

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Created by ykrank on 5/27/24
 * 
 */
@Parcelize
data class LocalMedia(
    val uri: Uri,
    val isCompressed: Boolean = false,
    val compressPath: Uri? = null
) : Parcelable
