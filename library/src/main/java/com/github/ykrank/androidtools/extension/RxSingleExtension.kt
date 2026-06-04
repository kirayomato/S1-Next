package com.github.ykrank.androidtools.extension

import io.reactivex.Single
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

suspend fun <T> Single<T>.await(): T {
    return suspendCancellableCoroutine { continuation ->
        val disposable = subscribe(
            { value ->
                if (continuation.isActive) {
                    continuation.resume(value)
                }
            },
            { error ->
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            },
        )
        continuation.invokeOnCancellation {
            disposable.dispose()
        }
    }
}
