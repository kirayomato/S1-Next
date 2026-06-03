package me.ykrank.s1next.data.db.biz

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import me.ykrank.s1next.App

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BizDependenciesEntryPoint {
    val blackListBiz: BlackListBiz
    val blackWordBiz: BlackWordBiz
    val historyBiz: HistoryBiz
    val loginUserBiz: LoginUserBiz
    val readProgressBiz: ReadProgressBiz
    val threadBiz: ThreadBiz
    val userProfileBiz: UserProfileBiz
}

internal fun bizDependencies(): BizDependenciesEntryPoint {
    return EntryPointAccessors.fromApplication(
        App.get(),
        BizDependenciesEntryPoint::class.java
    )
}
