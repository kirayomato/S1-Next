package me.ykrank.s1next.data.api

import androidx.collection.LruCache
import com.github.ykrank.androidtools.util.L
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.ykrank.s1next.data.api.model.Profile
import me.ykrank.s1next.data.cache.exmodel.BaseCache
import me.ykrank.s1next.data.db.biz.UserProfileBiz
import java.util.concurrent.ConcurrentHashMap

class S1ProfileProvider(
    private val s1Service: S1Service,
    private val userProfileBiz: UserProfileBiz,
) : ProfileProvider {

    private val profileCache = LruCache<String, BaseCache<Profile>>(500)
    private val activeRequests = ConcurrentHashMap<String, Deferred<Profile?>>()
    private val requestSemaphore = Semaphore(MAX_CONCURRENT_PROFILE_REQUESTS)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _profileUpdateFlow = MutableSharedFlow<Pair<String, Profile>>()
    override val profileUpdateFlow = _profileUpdateFlow.asSharedFlow()

    override suspend fun getProfile(userId: String, forceRefresh: Boolean): Profile? {
        var cachedProfile: Profile? = null
        if (!forceRefresh) {
            val memoryCache = profileCache.get(userId)
            if (memoryCache != null && !isExpired(memoryCache.time)) {
                return memoryCache.data
            }
            val dbCache = withContext(Dispatchers.IO) {
                userProfileBiz.getProfile(userId)
            }
            if (dbCache != null) {
                cachedProfile = dbCache.profile
                profileCache.put(userId, BaseCache(dbCache.updatedAt, dbCache.profile))
                if (!isExpired(dbCache.updatedAt)) {
                    return dbCache.profile
                }
            }
        }

        return refreshProfile(userId) ?: cachedProfile
    }

    override fun getProfiles(
        authorIds: List<String>,
        onProfileUpdate: ((uid: String, profile: Profile) -> Unit)?
    ) {
        authorIds.distinct().forEach { authorId ->
            scope.launch {
                val cachedProfile = loadCachedProfile(authorId)
                if (cachedProfile != null) {
                    launch(Dispatchers.Main) {
                        onProfileUpdate?.invoke(authorId, cachedProfile.profile)
                    }
                }
                if (cachedProfile == null || isExpired(cachedProfile.updatedAt)) {
                    refreshProfile(authorId)?.let { profile ->
                        launch(Dispatchers.Main) {
                            onProfileUpdate?.invoke(authorId, profile)
                        }
                    }
                }
            }
        }
    }

    private fun loadCachedProfile(userId: String): UserProfileBiz.CachedProfile? {
        val memoryCache = profileCache.get(userId)
        if (memoryCache != null) {
            return UserProfileBiz.CachedProfile(memoryCache.data, memoryCache.time)
        }
        return userProfileBiz.getProfile(userId)?.also {
            profileCache.put(userId, BaseCache(it.updatedAt, it.profile))
        }
    }

    private suspend fun refreshProfile(userId: String): Profile? {
        val deferred = activeRequests.computeIfAbsent(userId) { id ->
            L.d("ProfileProvider: No active request for $id. Starting new network call.")
            scope.async {
                try {
                    requestSemaphore.withPermit {
                        fetchProfileFromApi(id)?.let { profile ->
                            putProfileCache(id, profile).profile
                        }
                    }
                } catch (e: Exception) {
                    L.report(e)
                    null
                } finally {
                    L.d("ProfileProvider: Request for $id finished. Removing from active requests.")
                    activeRequests.remove(id)
                }
            }
        }

        return try {
            deferred.await()
        } catch (e: Exception) {
            L.e("ProfileProvider: Error awaiting profile for id:$userId", e)
            null
        }
    }

    private suspend fun fetchProfileFromApi(uid: String): Profile? {
        try {
            val htmlResponse = s1Service.getProfileWeb(
                "${Api.BASE_URL}space-uid-${uid}.html", uid
            )
            return Profile.fromHtml(htmlResponse)
        } catch (e: Exception) {
            L.report(e)
            return null
        }
    }

    private fun putProfileCache(uid: String, profile: Profile): UserProfileBiz.CachedProfile {
        val cachedProfile = userProfileBiz.saveProfile(uid, profile)
        profileCache.put(uid, BaseCache(cachedProfile.updatedAt, cachedProfile.profile))
        scope.launch {
            _profileUpdateFlow.emit(uid to cachedProfile.profile)
        }
        return cachedProfile
    }

    private fun isExpired(updatedAt: Long): Boolean {
        return System.currentTimeMillis() - updatedAt > DB_CACHE_PROFILE_MILLS
    }

    companion object {
        const val TAG = "S1Profile"
        const val MAX_CONCURRENT_PROFILE_REQUESTS = 5
        const val DB_CACHE_PROFILE_MILLS = 24 * 60 * 60 * 1_000L
    }
}
