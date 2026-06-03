package me.ykrank.s1next.data.db.biz

import android.database.Cursor
import androidx.annotation.WorkerThread
import com.github.ykrank.androidtools.util.L
import me.ykrank.s1next.data.db.AppDatabase
import me.ykrank.s1next.data.db.AppDatabaseManager
import me.ykrank.s1next.data.db.dao.HistoryDao
import me.ykrank.s1next.data.db.dbmodel.History

class HistoryBiz(private val manager: AppDatabaseManager) {

    private val historyDao: HistoryDao
        get() = session.history()

    private val session: AppDatabase
        get() = manager.getOrBuildDb()

    /**
     * limit [.MAX_SIZE] order by timestamp desc
     */
    @WorkerThread
    fun getHistoryListCursor(query: String? = null): Cursor {
        val normalizedQuery = query?.trim()?.takeIf { it.isNotEmpty() }
        return if (normalizedQuery == null) {
            historyDao.loadCursor(MAX_SIZE)
        } else {
            historyDao.searchCursor(MAX_SIZE, normalizedQuery)
        }
    }

    fun fromCursor(cursor: Cursor): History {
        return History(
            id = cursor.getLong(cursor.getColumnIndexOrThrow("_id")),
            threadId = cursor.getInt(cursor.getColumnIndexOrThrow("ThreadId")),
            title = cursor.getString(cursor.getColumnIndexOrThrow("Title")),
            timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("Timestamp")),
        )
    }

    /**
     * add new history
     */
    @WorkerThread
    fun addNewHistory(history: History) {
        val oldHistory: History? = historyDao.getByThreadId(history.threadId)
        if (oldHistory != null) {
            //have same threadId history
            history.id = oldHistory.id
            historyDao.update(history)
        } else {
            historyDao.insert(history)
            val delCount = historyDao.deleteNotTopRecords()
            L.i(TAG, "del not top records: $delCount")
        }
    }

    companion object {
        private const val TAG = "HistoryBiz"

        /**
         * max history count
         */
        const val MAX_SIZE = 300

        val instance: HistoryBiz
            get() = bizDependencies().historyBiz
    }

}
