package me.ykrank.s1next

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.ykrank.s1next.widget.net.AppData
import me.ykrank.s1next.widget.net.Data
import me.ykrank.s1next.widget.net.Image
import okhttp3.OkHttpClient
import javax.inject.Singleton

/**
 * Provides instances of the objects according to build type when we need to inject.
 */
@Module
@InstallIn(SingletonComponent::class)
class BuildTypeModule {
    @Data
    @Provides
    @Singleton
    fun providerDataOkHttpClient(@Data builder: OkHttpClient.Builder): OkHttpClient {
        return builder.build()
    }

    @Image
    @Provides
    @Singleton
    fun providerImageOkHttpClient(@Image builder: OkHttpClient.Builder): OkHttpClient {
        return builder.build()
    }

    @AppData
    @Provides
    @Singleton
    fun providerAppdataOkHttpClient(@AppData builder: OkHttpClient.Builder): OkHttpClient {
        return builder.build()
    }
}
