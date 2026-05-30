package me.ykrank.s1next.view.page.post.render

import android.util.Size
import java.util.concurrent.ConcurrentHashMap

object PostImageSizeCache {
    private val cache = ConcurrentHashMap<String, Size>()

    fun get(url: String): Size? = cache[url]

    fun put(url: String, width: Int, height: Int) {
        if (width > 0 && height > 0) {
            cache[url] = Size(width, height)
        }
    }
}

