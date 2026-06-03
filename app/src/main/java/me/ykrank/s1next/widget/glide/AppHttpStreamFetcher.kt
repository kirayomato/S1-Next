package me.ykrank.s1next.widget.glide

import com.bumptech.glide.Priority
import com.bumptech.glide.integration.okhttp3.OkHttpStreamFetcher
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.GlideUrl
import me.ykrank.s1next.data.pref.DownloadPreferencesManager
import okhttp3.Call
import java.io.InputStream

/**
 * Created by ykrank on 2017/3/21.
 */

class AppHttpStreamFetcher(
    client: Call.Factory,
    url: GlideUrl,
    private val mDownloadPreferencesManager: DownloadPreferencesManager,
) : OkHttpStreamFetcher(client, url) {

    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        if (!mDownloadPreferencesManager.isImagesDownload) {
            callback.onDataReady(null)
            return
        }
        super.loadData(priority, callback)
    }
}
