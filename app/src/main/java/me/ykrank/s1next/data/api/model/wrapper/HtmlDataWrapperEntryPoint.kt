package me.ykrank.s1next.data.api.model.wrapper

import com.github.ykrank.androidtools.widget.EventBus
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface HtmlDataWrapperEntryPoint {
    val eventBus: EventBus
}
