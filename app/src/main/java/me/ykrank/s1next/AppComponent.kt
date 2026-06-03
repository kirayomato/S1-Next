package me.ykrank.s1next

import com.github.ykrank.androidtools.widget.EditorDiskCache
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.ykrank.s1next.data.User
import me.ykrank.s1next.data.api.ApiCacheProvider
import me.ykrank.s1next.data.api.ProfileProvider
import me.ykrank.s1next.data.api.S1Service
import me.ykrank.s1next.data.api.UserValidator
import me.ykrank.s1next.data.api.app.AppService
import me.ykrank.s1next.data.cache.biz.CacheBiz
import me.ykrank.s1next.data.db.AppDatabaseManager
import me.ykrank.s1next.data.db.biz.BlackListBiz
import me.ykrank.s1next.data.db.biz.BlackWordBiz
import me.ykrank.s1next.data.db.biz.HistoryBiz
import me.ykrank.s1next.data.db.biz.LoginUserBiz
import me.ykrank.s1next.data.db.biz.ReadProgressBiz
import me.ykrank.s1next.data.db.biz.ThreadBiz
import me.ykrank.s1next.data.db.biz.UserProfileBiz
import me.ykrank.s1next.task.AutoSignTask
import me.ykrank.s1next.viewmodel.UserViewModel
import me.ykrank.s1next.widget.EmoticonFactory
import me.ykrank.s1next.widget.glide.AvatarFailUrlsCache
import me.ykrank.s1next.widget.hostcheck.AppHostUrl
import me.ykrank.s1next.widget.hostcheck.NoticeCheckTask
import me.ykrank.s1next.widget.net.Image
import okhttp3.Dns
import okhttp3.OkHttpClient

/**
 * Indicates the class where this module is going to inject dependencies
 * or the dependencies we want to get.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppComponent {
    val preAppComponent: PreAppComponent
    val baseHostUrl: AppHostUrl
    val httpDns: Dns

    @get:Image
    val imageOkHttpClient: OkHttpClient
    val s1Service: S1Service
    val appService: AppService
    val apiCacheProvider: ApiCacheProvider
    val user: User
    val userValidator: UserValidator
    val profileProvider: ProfileProvider
    val userViewModel: UserViewModel
    val noticeCheckTask: NoticeCheckTask
    val editorDiskCache: EditorDiskCache
    val emoticonFactory: EmoticonFactory

    val avatarFailUrlsCache: AvatarFailUrlsCache
    val autoSignTask: AutoSignTask

    //region DataBase
    val appDatabaseManager: AppDatabaseManager

    val blackListBiz: BlackListBiz
    val blackWordBiz: BlackWordBiz
    val readProgressBiz: ReadProgressBiz
    val threadBiz: ThreadBiz
    val historyBiz: HistoryBiz
    val loginUserBiz: LoginUserBiz
    val userProfileBiz: UserProfileBiz
    val cacheBiz: CacheBiz

    //endregion
}
