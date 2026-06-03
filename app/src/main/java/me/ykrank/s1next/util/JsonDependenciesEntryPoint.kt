package me.ykrank.s1next.util

import com.fasterxml.jackson.databind.ObjectMapper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface JsonDependenciesEntryPoint {
    val jsonMapper: ObjectMapper
}
