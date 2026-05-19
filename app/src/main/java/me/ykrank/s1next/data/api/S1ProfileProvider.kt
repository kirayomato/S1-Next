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
import me.ykrank.s1next.data.api.model.Profile
import me.ykrank.s1next.data.cache.CacheConstants
import me.ykrank.s1next.data.cache.biz.CacheBiz
import me.ykrank.s1next.data.cache.exmodel.BaseCache
import me.ykrank.s1next.data.pref.DownloadPreferencesManager
import java.util.concurrent.ConcurrentHashMap

class S1ProfileProvider(
    private val s1Service: S1Service,
    private val cacheBiz: CacheBiz,
    private val downloadPreferencesManager: DownloadPreferencesManager,
) : ProfileProvider {

    private val profileCache = LruCache<String, BaseCache<Profile>>(500)
    private val activeRequests = ConcurrentHashMap<String, Deferred<Profile?>>()
    private val requestSemaphore = Semaphore(MAX_CONCURRENT_PROFILE_REQUESTS)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _profileUpdateFlow = MutableSharedFlow<Pair<String, Profile>>()
    override val profileUpdateFlow = _profileUpdateFlow.asSharedFlow()

    override fun getProfileCaches(userId: String): Profile? {
        val cache = profileCache.get(userId)
        if (cache != null && System.currentTimeMillis() - cache.time < MEMORY_CACHE_PROFILE_MILLS) {
            return cache.data
        }
        return getProfileDiskCache(userId)?.also {
            profileCache.put(userId, BaseCache(System.currentTimeMillis(), it))
        }
    }

    override suspend fun getProfile(userId: String, forceRefresh: Boolean): Profile? {
        if (!forceRefresh) {
            getProfileCaches(userId)?.let { return it }
        }

        val deferred = activeRequests.computeIfAbsent(userId) { id ->
            L.d("ProfileProvider: No active request for $id. Starting new network call.")
            scope.async {
                try {
                    requestSemaphore.withPermit {
                        fetchProfileFromApi(id)
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
            deferred.await()?.also { profile ->
                putProfileCache(userId, profile)
            }
        } catch (e: Exception) {
            L.e("ProfileProvider: Error awaiting profile for id:$userId", e)
            null
        }
    }

    override fun getProfiles(
        authorIds: List<String>,
        onProfileUpdate: ((uid: String, profile: Profile) -> Unit)?
    ) {
        authorIds.distinct().forEach { authorId ->
            scope.launch {
                getProfile(authorId)?.let { profile ->
                    launch(Dispatchers.Main) {
                        onProfileUpdate?.invoke(authorId, profile)
                    }
                }
            }

        }
    }


    private suspend fun fetchProfileFromApi(uid: String): Profile? {
        try {
            val htmlResponse = s1Service.getProfileWeb(
                "${Api.BASE_URL}space-uid-${uid}.html", uid
            )
            val profile = Profile.fromHtml(htmlResponse)
            saveProfileDiskCache(uid, htmlResponse)
            return profile
        } catch (e: Exception) {
            L.report(e)
            return null
        }
    }

    private fun putProfileCache(uid: String, profile: Profile) {
        profileCache.put(uid, BaseCache(System.currentTimeMillis(), profile))
        scope.launch {
            _profileUpdateFlow.emit(uid to profile)
        }
    }

    private fun getProfileDiskCache(uid: String): Profile? {
        val cache = cacheBiz.getTextZipNewest(listOf(CacheConstants.GROUP_PROFILE, uid)) ?: return null
        if (System.currentTimeMillis() - cache.timestamp > DISK_CACHE_PROFILE_MILLS) {
            return null
        }
        val html = cache.decodeZipString ?: return null
        return runCatching {
            Profile.fromHtml(html)
        }.onFailure {
            L.report(it)
        }.getOrNull()
    }

    private fun saveProfileDiskCache(uid: String, html: String) {
        cacheBiz.saveZipAsync(
            profileCacheKey(uid),
            uid.toIntOrNull(),
            html,
            maxSize = downloadPreferencesManager.totalDataCacheSize,
            groups = listOf(CacheConstants.GROUP_PROFILE, uid)
        )
    }

    private fun profileCacheKey(uid: String): String {
        return "${CacheConstants.GROUP_PROFILE}#$uid"
    }

    companion object {
        const val TAG = "S1Profile"
        const val MAX_CONCURRENT_PROFILE_REQUESTS = 5
        const val MEMORY_CACHE_PROFILE_MILLS = 5 * 60 * 1_000L
        const val DISK_CACHE_PROFILE_MILLS = 24 * 60 * 60 * 1_000L
    }
}
