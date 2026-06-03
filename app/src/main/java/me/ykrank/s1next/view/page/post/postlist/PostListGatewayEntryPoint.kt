package me.ykrank.s1next.view.page.post.postlist

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.ykrank.s1next.data.pref.ThemeManager

@EntryPoint
@InstallIn(SingletonComponent::class)
interface PostListGatewayEntryPoint {
    val themeManager: ThemeManager
}
