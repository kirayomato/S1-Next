package me.ykrank.s1next.data.cache.biz

import android.database.Cursor
import androidx.annotation.WorkerThread
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.util.ZipUtils
import me.ykrank.s1next.App
import me.ykrank.s1next.data.cache.CacheConstants
import me.ykrank.s1next.data.cache.CacheDatabase
import me.ykrank.s1next.data.cache.CacheDatabaseManager
import me.ykrank.s1next.data.cache.dao.CacheDao
import me.ykrank.s1next.data.cache.dbmodel.Cache
import me.ykrank.s1next.data.cache.exmodel.CacheGroupModel
import me.ykrank.s1next.data.db.dbmodel.History

/**
 * Created by ykrank on 7/17/24
 * 
 */
class CacheBiz(private val manager: CacheDatabaseManager, private val objectMapper: ObjectMapper) {

    private val cacheDao: CacheDao
        get() = session.cache()

    private val session: CacheDatabase
        get() = manager.getOrBuildDb()

    val count
        @WorkerThread
        get() = cacheDao.getCount()

    val ordinaryCount
        @WorkerThread
        get() = cacheDao.getCountExcludeGroup()

    val size
        @WorkerThread
        get() = App.get().getDatabasePath(CacheDatabase.DB_NAME).length()

    /**
     * 1. 如果传入blob，则将blob压缩为zip。
     * 2. 否则将传入的decodeZipString压缩为zip
     */
    @WorkerThread
    private fun saveZip(
        cache: Cache,
        maxSize: Int = DEFAULT_MAX_SIZE
    ) {
        val content = cache.blob ?: cache.decodeZipString?.toByteArray(Charsets.UTF_8)
        if (content != null) {
            val start = System.currentTimeMillis()
            val gzipBlob = ZipUtils.compressByGzip(content)
            val gzipTime = System.currentTimeMillis() - start
            L.i(
                TAG,
                "saveTextZip: ${cache.group} $${cache.key} s${content.size}->${gzipBlob.size} t${gzipTime}, max:${maxSize}"
            )
            cache.blob = gzipBlob
        }
        preserveBackupGroup(cache)
        cacheDao.insert(cache)
        if (ordinaryCount > maxSize) {
            cacheDao.deleteNotTopRecords(maxSize)
        }
    }

    @WorkerThread
    private fun preserveBackupGroup(cache: Cache) {
        if (cache.group == CacheConstants.GROUP_POST_BACKUP) {
            return
        }
        val oldCache = cacheDao.getByKey(cache.key)
        if (oldCache?.group == CacheConstants.GROUP_POST_BACKUP) {
            cache.id = oldCache.id
            cache.group = oldCache.group
            cache.group1 = oldCache.group1
            cache.group2 = oldCache.group2
            cache.group3 = oldCache.group3
            if (cache.title.isNullOrBlank()) {
                cache.title = oldCache.title
            }
        }
    }

    /**
     * 注意content必须是不可修改的，避免异步问题
     */
    fun <T> saveZipAsync(
        key: String,
        uid: Int?,
        content: T,
        title: String? = null,
        maxSize: Int = DEFAULT_MAX_SIZE,
        groups: List<String>,
    ) {
        manager.runAsync {
            saveZip(
                Cache(
                    key,
                    uid = uid,
                    title = title,
                    groups = groups,
                    decodeZipString = if (content is String) {
                        content
                    } else {
                        objectMapper.writeValueAsString(content)
                    }
                ), maxSize
            )
        }
    }

    /**
     * 查询缓存，并解码zip blob
     */
    @WorkerThread
    fun getTextZipByKey(key: String): Cache? {
        return cacheDao.getByKey(key)?.apply {
            this.decodeZipString = this.blob?.let {
                ZipUtils.decompressGzipToString(it)
            }
        }
    }

    fun getTextZipNewest(
        groups: List<String>,
    ): Cache? {
        val group = CacheGroupModel(groups)
        return cacheDao.getNewestByGroup(group.group, group.group1, group.group2, group.group3)
            ?.apply {
            this.decodeZipString = this.blob?.let {
                ZipUtils.decompressGzipToString(it)
            }
        }
    }

    @WorkerThread
    fun savePostBackup(
        key: String,
        uid: Int?,
        content: Any,
        title: String?,
        threadId: String,
        page: Int,
    ) {
        saveZip(
            Cache(
                key,
                uid = uid,
                title = title,
                groups = listOf(
                    CacheConstants.GROUP_POST_BACKUP,
                    threadId,
                    page.toString(),
                ),
                decodeZipString = if (content is String) {
                    content
                } else {
                    objectMapper.writeValueAsString(content)
                }
            ),
            maxSize = Int.MAX_VALUE
        )
    }

    @WorkerThread
    fun getPostBackupThreadsCursor(query: String?): Cursor {
        return cacheDao.loadPostBackupThreadsCursor(
            query = query?.trim()?.takeIf { it.isNotEmpty() }
        )
    }

    fun historyFromPostBackupCursor(cursor: Cursor): History {
        return History(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
            threadId = cursor.getInt(cursor.getColumnIndexOrThrow("ThreadId")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("Title")),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("Timestamp")),
        )
    }

    companion object {
        const val TAG = "CacheBiz"
        const val DEFAULT_MAX_SIZE = 1000
    }
}
