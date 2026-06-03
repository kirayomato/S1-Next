package me.ykrank.s1next.view.activity

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.ykrank.s1next.data.User

@EntryPoint
@InstallIn(SingletonComponent::class)
interface UserHomeLauncherEntryPoint {
    val user: User
}
