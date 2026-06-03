package me.ykrank.s1next.data.db.biz

import androidx.annotation.WorkerThread
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.ykrank.androidtools.util.L
import me.ykrank.s1next.data.api.model.Profile
import me.ykrank.s1next.data.db.AppDatabase
import me.ykrank.s1next.data.db.AppDatabaseManager
import me.ykrank.s1next.data.db.dbmodel.UserProfile

class UserProfileBiz(
    private val manager: AppDatabaseManager,
    private val objectMapper: ObjectMapper,
) {

    data class CachedProfile(
        val profile: Profile,
        val updatedAt: Long,
    )

    private val userProfileDao
        get() = session.userProfile()

    private val session: AppDatabase
        get() = manager.getOrBuildDb()

    @WorkerThread
    fun getProfile(uid: String): CachedProfile? {
        return userProfileDao.getByUid(uid)?.toCachedProfile()
    }

    @WorkerThread
    fun getProfiles(uids: List<String>): List<CachedProfile> {
        if (uids.isEmpty()) {
            return emptyList()
        }
        return userProfileDao.getByUids(uids).map { it.toCachedProfile() }
    }

    @WorkerThread
    fun saveProfile(uid: String, profile: Profile): CachedProfile {
        val now = System.currentTimeMillis()
        val normalized = profile.normalized(uid)
        val entity = UserProfile(
            uid = uid,
            username = normalized.username,
            homeUsername = normalized.homeUsername,
            signHtml = normalized.signHtml,
            friends = normalized.friends,
            replies = normalized.replies,
            threads = normalized.threads,
            groupTitle = normalized.groupTitle,
            onlineHour = normalized.onlineHour,
            regDate = normalized.regDate,
            lastVisitDate = normalized.lastVisitDate,
            lastActiveDate = normalized.lastActiveDate,
            lastPostDate = normalized.lastPostDate,
            goose = normalized.gooseValue,
            statsJson = encodeStats(normalized.stats),
            managerJson = encodeManager(normalized.manager),
            updatedAt = now,
            lastRequestAt = now,
        )
        userProfileDao.insert(entity)
        return entity.toCachedProfile()
    }

    private fun UserProfile.toCachedProfile(): CachedProfile {
        val profile = Profile().apply {
            uid = this@toCachedProfile.uid
            username = this@toCachedProfile.username
            homeUid = this@toCachedProfile.uid
            homeUsername = this@toCachedProfile.homeUsername ?: this@toCachedProfile.username
            signHtml = this@toCachedProfile.signHtml
            friends = this@toCachedProfile.friends ?: 0
            replies = this@toCachedProfile.replies ?: 0
            threads = this@toCachedProfile.threads ?: 0
            groupTitle = this@toCachedProfile.groupTitle
            onlineHour = this@toCachedProfile.onlineHour ?: 0
            regDate = this@toCachedProfile.regDate
            lastVisitDate = this@toCachedProfile.lastVisitDate
            lastActiveDate = this@toCachedProfile.lastActiveDate
            lastPostDate = this@toCachedProfile.lastPostDate
            gooseValue = this@toCachedProfile.goose
            stats = decodeStats(this@toCachedProfile.statsJson)
            manager = decodeManager(this@toCachedProfile.managerJson)
        }
        return CachedProfile(profile, updatedAt)
    }

    private fun Profile.normalized(uid: String): Profile {
        if (this.uid.isNullOrBlank()) {
            this.uid = uid
        }
        if (homeUid.isNullOrBlank()) {
            homeUid = uid
        }
        if (username.isNullOrBlank()) {
            username = homeUsername
        }
        if (homeUsername.isNullOrBlank()) {
            homeUsername = username
        }
        if (gooseValue == null) {
            gooseValue = goose?.trim()?.toIntOrNull()
        }
        return this
    }

    private fun encodeStats(stats: List<Pair<String, String>>): String? {
        if (stats.isEmpty()) {
            return null
        }
        return runCatching {
            objectMapper.writeValueAsString(stats.map { listOf(it.first, it.second) })
        }.onFailure {
            L.report(it)
        }.getOrNull()
    }

    private fun decodeStats(statsJson: String?): List<Pair<String, String>> {
        if (statsJson.isNullOrBlank()) {
            return emptyList()
        }
        return runCatching {
            objectMapper.readValue(statsJson, STATS_TYPE)
                .mapNotNull { values ->
                    if (values.size >= 2) values[0] to values[1] else null
                }
        }.onFailure {
            L.report(it)
        }.getOrDefault(emptyList())
    }

    private fun encodeManager(manager: List<String>?): String? {
        if (manager.isNullOrEmpty()) {
            return null
        }
        return runCatching {
            objectMapper.writeValueAsString(manager)
        }.onFailure {
            L.report(it)
        }.getOrNull()
    }

    private fun decodeManager(managerJson: String?): List<String>? {
        if (managerJson.isNullOrBlank()) {
            return null
        }
        return runCatching {
            objectMapper.readValue(managerJson, MANAGER_TYPE)
        }.onFailure {
            L.report(it)
        }.getOrNull()
    }

    companion object {
        private val STATS_TYPE = object : TypeReference<List<List<String>>>() {}
        private val MANAGER_TYPE = object : TypeReference<List<String>>() {}

        val instance: UserProfileBiz
            get() = bizDependencies().userProfileBiz
    }
}
