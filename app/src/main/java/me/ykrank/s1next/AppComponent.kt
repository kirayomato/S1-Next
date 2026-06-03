package me.ykrank.s1next

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.ykrank.s1next.widget.hostcheck.NoticeCheckTask

/**
 * Narrow startup entry point for framework-owned application code.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppComponent {
    val noticeCheckTask: NoticeCheckTask
}
