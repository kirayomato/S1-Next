package me.ykrank.s1next.data.db

import android.content.Context
import com.fasterxml.jackson.databind.ObjectMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.ykrank.s1next.data.cache.CacheDatabaseManager
import me.ykrank.s1next.data.cache.CacheDatabaseManagerImpl
import me.ykrank.s1next.data.cache.biz.CacheBiz
import me.ykrank.s1next.data.cache.biz.CacheGroupBiz
import me.ykrank.s1next.data.db.biz.BlackListBiz
import me.ykrank.s1next.data.db.biz.BlackWordBiz
import me.ykrank.s1next.data.db.biz.HistoryBiz
import me.ykrank.s1next.data.db.biz.LoginUserBiz
import me.ykrank.s1next.data.db.biz.ReadProgressBiz
import me.ykrank.s1next.data.db.biz.ThreadBiz
import me.ykrank.s1next.data.db.biz.UserProfileBiz
import me.ykrank.s1next.widget.encrypt.AndroidStoreEncryption
import me.ykrank.s1next.widget.encrypt.Encryption
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DbModule {
    @Provides
    @Singleton
    fun provideAppDatabaseManager(context: Context): AppDatabaseManager {
        return AppDatabaseManagerImpl(context)
    }

    @Provides
    @Singleton
    fun provideBlackListBiz(manager: AppDatabaseManager): BlackListBiz {
        return BlackListBiz(manager)
    }

    @Provides
    @Singleton
    fun provideBlackWordBiz(manager: AppDatabaseManager): BlackWordBiz {
        return BlackWordBiz(manager)
    }

    @Provides
    @Singleton
    fun provideReadProgressDbWrapper(manager: AppDatabaseManager): ReadProgressBiz {
        return ReadProgressBiz(manager)
    }

    @Provides
    @Singleton
    fun provideThreadBiz(manager: AppDatabaseManager): ThreadBiz {
        return ThreadBiz(manager)
    }

    @Provides
    @Singleton
    fun provideHistoryBiz(manager: AppDatabaseManager): HistoryBiz {
        return HistoryBiz(manager)
    }

    @Provides
    @Singleton
    fun provideLoginUserBiz(manager: AppDatabaseManager, encryption: Encryption): LoginUserBiz {
        return LoginUserBiz(manager, encryption)
    }

    @Provides
    @Singleton
    fun provideUserProfileBiz(manager: AppDatabaseManager, objectMapper: ObjectMapper): UserProfileBiz {
        return UserProfileBiz(manager, objectMapper)
    }

    @Provides
    @Singleton
    fun provideDbEncryption(): Encryption {
        return AndroidStoreEncryption("s1next_db")
    }

    @Provides
    @Singleton
    fun provideCacheDatabaseManager(
        context: Context,
        appManager: AppDatabaseManager
    ): CacheDatabaseManager {
        return CacheDatabaseManagerImpl(context, appManager)
    }

    @Provides
    @Singleton
    fun provideCacheBiz(manager: CacheDatabaseManager, objectMapper: ObjectMapper): CacheBiz {
        return CacheBiz(manager, objectMapper)
    }

    @Provides
    @Singleton
    fun provideCacheGroupBiz(manager: CacheDatabaseManager): CacheGroupBiz {
        return CacheGroupBiz(manager)
    }
}
