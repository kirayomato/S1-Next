package me.ykrank.s1next.data.db

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.annotation.WorkerThread
import com.github.ykrank.androidtools.util.L
import com.github.ykrank.androidtools.widget.BackupDelegate
import me.ykrank.s1next.data.db.dbmodel.BlackList
import me.ykrank.s1next.data.db.dbmodel.BlackWord
import me.ykrank.s1next.data.db.dbmodel.DbThread
import me.ykrank.s1next.data.db.dbmodel.History
import me.ykrank.s1next.data.db.dbmodel.LoginUser
import me.ykrank.s1next.data.db.dbmodel.ReadProgress
import me.ykrank.s1next.data.db.dbmodel.UserProfile
import java.io.File
import java.io.IOException

class AppDatabaseBackupMerger(
    private val context: Context,
    private val databaseManager: AppDatabaseManager
) {

    @WorkerThread
    fun merge(srcUri: Uri): Int {
        val tempFile = runCatching { copyToTempFile(srcUri) }.getOrElse {
            L.e(TAG, it)
            return if (it is IOException) {
                BackupDelegate.IO_EXCEPTION
            } else {
                BackupDelegate.UNKNOWN_EXCEPTION
            }
        }
        return try {
            SQLiteDatabase.openDatabase(
                tempFile.absolutePath,
                null,
                SQLiteDatabase.OPEN_READONLY
            ).use { sourceDb ->
                val targetDb = databaseManager.getOrBuildDb()
                targetDb.runInTransaction {
                    mergeBlackList(sourceDb, targetDb)
                    mergeBlackWord(sourceDb, targetDb)
                    mergeThreads(sourceDb, targetDb)
                    mergeHistories(sourceDb, targetDb)
                    mergeReadProgress(sourceDb, targetDb)
                    mergeLoginUsers(sourceDb, targetDb)
                    mergeUserProfiles(sourceDb, targetDb)
                }
            }
            BackupDelegate.SUCCESS
        } catch (e: Exception) {
            L.e(TAG, e)
            BackupDelegate.UNKNOWN_EXCEPTION
        } finally {
            tempFile.delete()
        }
    }

    @WorkerThread
    private fun copyToTempFile(srcUri: Uri): File {
        val tempFile = File.createTempFile("s1next_restore_", ".db", context.cacheDir)
        context.contentResolver.openInputStream(srcUri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Can not open backup file")
        return tempFile
    }

    private fun mergeBlackList(sourceDb: SQLiteDatabase, targetDb: AppDatabase) {
        queryTable(
            sourceDb,
            "BlackList",
            arrayOf("_id", "AuthorId", "Author", "Post", "Forum", "Remark", "Timestamp", "Upload")
        ) { cursor ->
            val item = BlackList(
                id = null,
                authorId = cursor.getInt("AuthorId"),
                author = cursor.getStringOrNull("Author"),
                post = cursor.getInt("Post"),
                forum = cursor.getInt("Forum"),
                remark = cursor.getStringOrNull("Remark"),
                timestamp = cursor.getLong("Timestamp"),
                upload = cursor.getInt("Upload") != 0
            )
            val oldItem = item.author?.let {
                targetDb.blacklist().getByAuthorAndId(item.authorId, it).firstOrNull()
            }
            if (oldItem == null) {
                targetDb.blacklist().insert(listOf(item))
            } else if (item.timestamp >= oldItem.timestamp) {
                item.id = oldItem.id
                targetDb.blacklist().update(listOf(item))
            }
        }
    }

    private fun mergeBlackWord(sourceDb: SQLiteDatabase, targetDb: AppDatabase) {
        queryTable(
            sourceDb,
            "BlackWord",
            arrayOf("_id", "Word", "Stat", "Timestamp", "Upload")
        ) { cursor ->
            val item = BlackWord(
                id = null,
                word = cursor.getStringOrNull("Word"),
                stat = cursor.getInt("Stat"),
                timestamp = cursor.getLong("Timestamp"),
                upload = cursor.getInt("Upload") != 0
            )
            val word = item.word
            val oldItem = if (word.isNullOrEmpty()) null else targetDb.blackWord().getByWord(word)
            if (oldItem == null) {
                targetDb.blackWord().insert(item)
            } else if (item.timestamp >= oldItem.timestamp) {
                item.id = oldItem.id
                targetDb.blackWord().update(item)
            }
        }
    }

    private fun mergeThreads(sourceDb: SQLiteDatabase, targetDb: AppDatabase) {
        queryTable(
            sourceDb,
            "DbThread",
            arrayOf("_id", "ThreadId", "LastCountWhenView", "Timestamp")
        ) { cursor ->
            val item = DbThread(
                id = null,
                threadId = cursor.getInt("ThreadId"),
                lastCountWhenView = cursor.getInt("LastCountWhenView"),
                timestamp = cursor.getLong("Timestamp")
            )
            val oldItem = targetDb.thread().getByThreadId(item.threadId)
            if (oldItem == null) {
                targetDb.thread().insert(item)
            } else if (item.timestamp >= oldItem.timestamp) {
                item.id = oldItem.id
                targetDb.thread().update(item)
            }
        }
    }

    private fun mergeHistories(sourceDb: SQLiteDatabase, targetDb: AppDatabase) {
        queryTable(
            sourceDb,
            "History",
            arrayOf("_id", "ThreadId", "Title", "Timestamp")
        ) { cursor ->
            val item = History(
                id = null,
                threadId = cursor.getInt("ThreadId"),
                title = cursor.getStringOrNull("Title"),
                timestamp = cursor.getLong("Timestamp")
            )
            val oldItem = targetDb.history().getByThreadId(item.threadId)
            if (oldItem == null) {
                targetDb.history().insert(item)
            } else if (item.timestamp >= oldItem.timestamp) {
                item.id = oldItem.id
                targetDb.history().update(item)
            }
        }
    }

    private fun mergeReadProgress(sourceDb: SQLiteDatabase, targetDb: AppDatabase) {
        queryTable(
            sourceDb,
            "ReadProgress",
            arrayOf("_id", "ThreadId", "Page", "Position", "Offset", "Timestamp")
        ) { cursor ->
            val item = ReadProgress(
                id = null,
                threadId = cursor.getInt("ThreadId"),
                page = cursor.getInt("Page"),
                position = cursor.getInt("Position"),
                offset = cursor.getInt("Offset"),
                timestamp = cursor.getLong("Timestamp")
            )
            val oldItem = targetDb.readProgress().getByThreadId(item.threadId)
            if (oldItem == null) {
                targetDb.readProgress().insert(item)
            } else if (item.timestamp >= oldItem.timestamp) {
                item.id = oldItem.id
                targetDb.readProgress().update(item)
            }
        }
    }

    private fun mergeLoginUsers(sourceDb: SQLiteDatabase, targetDb: AppDatabase) {
        queryTable(
            sourceDb,
            "LoginUser",
            arrayOf(
                "_id",
                "Uid",
                "Name",
                "EncryptPassword",
                "QuestionId",
                "EncryptAnswer",
                "LoginTime",
                "Timestamp"
            )
        ) { cursor ->
            val item = LoginUser(
                id = null,
                uid = cursor.getInt("Uid"),
                name = cursor.getStringOrNull("Name"),
                encryptPassword = cursor.getStringOrNull("EncryptPassword"),
                questionId = cursor.getStringOrNull("QuestionId"),
                encryptAnswer = cursor.getStringOrNull("EncryptAnswer"),
                loginTime = cursor.getLong("LoginTime"),
                timestamp = cursor.getLong("Timestamp")
            )
            val oldItem = targetDb.loginUser().getByUid(item.uid)
            if (oldItem == null) {
                targetDb.loginUser().insert(item)
            } else if (item.timestamp >= oldItem.timestamp) {
                item.id = oldItem.id
                targetDb.loginUser().update(item)
            }
        }
    }

    private fun mergeUserProfiles(sourceDb: SQLiteDatabase, targetDb: AppDatabase) {
        queryTable(
            sourceDb,
            "UserProfile",
            arrayOf(
                "Uid",
                "Username",
                "HomeUsername",
                "SignHtml",
                "Friends",
                "Replies",
                "Threads",
                "GroupTitle",
                "OnlineHour",
                "RegDate",
                "LastVisitDate",
                "LastActiveDate",
                "LastPostDate",
                "Goose",
                "StatsJson",
                "ManagerJson",
                "UpdatedAt",
                "LastRequestAt"
            )
        ) { cursor ->
            val uid = cursor.getStringOrNull("Uid") ?: return@queryTable
            val item = UserProfile(
                uid = uid,
                username = cursor.getStringOrNull("Username"),
                homeUsername = cursor.getStringOrNull("HomeUsername"),
                signHtml = cursor.getStringOrNull("SignHtml"),
                friends = cursor.getIntOrNull("Friends"),
                replies = cursor.getIntOrNull("Replies"),
                threads = cursor.getIntOrNull("Threads"),
                groupTitle = cursor.getStringOrNull("GroupTitle"),
                onlineHour = cursor.getIntOrNull("OnlineHour"),
                regDate = cursor.getLongOrNull("RegDate"),
                lastVisitDate = cursor.getLongOrNull("LastVisitDate"),
                lastActiveDate = cursor.getLongOrNull("LastActiveDate"),
                lastPostDate = cursor.getLongOrNull("LastPostDate"),
                goose = cursor.getIntOrNull("Goose"),
                statsJson = cursor.getStringOrNull("StatsJson"),
                managerJson = cursor.getStringOrNull("ManagerJson"),
                updatedAt = cursor.getLong("UpdatedAt"),
                lastRequestAt = cursor.getLong("LastRequestAt"),
            )
            val oldItem = targetDb.userProfile().getByUid(item.uid)
            if (oldItem == null || item.updatedAt >= oldItem.updatedAt) {
                targetDb.userProfile().insert(item)
            }
        }
    }

    private fun queryTable(
        sourceDb: SQLiteDatabase,
        table: String,
        columns: Array<String>,
        block: (Cursor) -> Unit
    ) {
        runCatching {
            sourceDb.query(table, columns, null, null, null, null, null).use { cursor ->
                while (cursor.moveToNext()) {
                    block(cursor)
                }
            }
        }.onFailure {
            L.e(TAG, it)
        }
    }

    private fun Cursor.getStringOrNull(columnName: String): String? {
        val index = getColumnIndexOrThrow(columnName)
        return if (isNull(index)) null else getString(index)
    }

    private fun Cursor.getInt(columnName: String): Int {
        return getInt(getColumnIndexOrThrow(columnName))
    }

    private fun Cursor.getIntOrNull(columnName: String): Int? {
        val index = getColumnIndexOrThrow(columnName)
        return if (isNull(index)) null else getInt(index)
    }

    private fun Cursor.getLong(columnName: String): Long {
        return getLong(getColumnIndexOrThrow(columnName))
    }

    private fun Cursor.getLongOrNull(columnName: String): Long? {
        val index = getColumnIndexOrThrow(columnName)
        return if (isNull(index)) null else getLong(index)
    }

    companion object {
        private const val TAG = "AppDatabaseBackupMerger"
    }
}
