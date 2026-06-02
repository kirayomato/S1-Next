package me.ykrank.s1next.data.api

import kotlinx.coroutines.flow.Flow
import me.ykrank.s1next.data.api.model.Profile


interface ProfileProvider {
    val profileUpdateFlow: Flow<Pair<String, Profile>>

    suspend fun getProfile(userId: String, forceRefresh: Boolean = false): Profile?

    fun getProfiles(
        authorIds: List<String>, onProfileUpdate: ((uid: String, profile: Profile) -> Unit)? = null
    )
}
