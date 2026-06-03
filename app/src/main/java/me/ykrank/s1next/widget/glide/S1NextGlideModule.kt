package me.ykrank.s1next.widget.glide

import android.content.Context
import android.os.Build
import android.util.Log
import com.bumptech.glide.Glide
import com.bumptech.glide.GlideBuilder
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.EntryPointAccessors
import java.io.InputStream

/**
 * Lazily configures Glide.
 */
@GlideModule
class S1NextGlideModule : AppGlideModule() {
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }

    override fun applyOptions(context: Context, builder: GlideBuilder) {
        val dependencies = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GlideDependenciesEntryPoint::class.java
        )

        // set max size of the disk cache for images
        builder.setDiskCache(
            InternalCacheDiskCacheFactory(
                context, dependencies.downloadPreferencesManager.totalImageCacheSize
            )
        )
        builder.setLogLevel(Log.ERROR)
        var requestOptions = RequestOptions()

        //Change default RGB_565 to ARGB_8888, show image with transparent
        requestOptions = requestOptions.format(DecodeFormat.PREFER_ARGB_8888)
        requestOptions = requestOptions.diskCacheStrategy(DiskCacheStrategy.DATA)

        //shared element transition crash in version O, fix in O MR1
        //https://muyangmin.github.io/glide-docs-cn/doc/hardwarebitmaps.html
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            requestOptions = requestOptions.disallowHardwareConfig()
        }
        builder.setDefaultRequestOptions(requestOptions)

//        val bitmapPoolSizeBytes = 1024 * 1024 * 0L // 0mb
//        val memoryCacheSizeBytes = 1024 * 1024 * 0L // 0mb
//        builder.setMemoryCache(LruResourceCache(memoryCacheSizeBytes))
//        builder.setBitmapPool(LruBitmapPool(bitmapPoolSizeBytes))

        //兼容了华为机型上，Register too many Broadcast Receivers 的问题
        if (NoConnectivityMonitorFactory.needDisableNetCheck()) {
            builder.setConnectivityMonitorFactory(NoConnectivityMonitorFactory())
        }
    }

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        super.registerComponents(context, glide, registry)
        val dependencies = EntryPointAccessors.fromApplication(
            context.applicationContext,
            GlideDependenciesEntryPoint::class.java
        )
        registry.replace(
            GlideUrl::class.java, InputStream::class.java, AppHttpUrlLoader.Factory(
                dependencies.imageOkHttpClient,
                dependencies.downloadPreferencesManager,
                dependencies.avatarFailUrlsCache,
            )
        )
    }
}
