package com.example.auto_saver.data.firestore

/**
 * Lightweight wrapper to avoid leaking Firebase Task APIs into the rest of the app.
 */
sealed class FirestoreResult<out T> {
    data class Success<T>(val data: T) : FirestoreResult<T>()
    data class Error(val throwable: Throwable) : FirestoreResult<Nothing>()
}

inline fun <T> FirestoreResult<T>.onSuccess(action: (T) -> Unit): FirestoreResult<T> {
    if (this is FirestoreResult.Success) {
        action(data)
    }
    return this
}

inline fun <T> FirestoreResult<T>.onError(action: (Throwable) -> Unit): FirestoreResult<T> {
    if (this is FirestoreResult.Error) {
        action(throwable)
    }
    return this
}

fun <T> FirestoreResult<T>.getOrNull(): T? = (this as? FirestoreResult.Success)?.data

fun FirestoreResult<*>.exceptionOrNull(): Throwable? = (this as? FirestoreResult.Error)?.throwable

suspend inline fun <T> safeFirestoreCall(crossinline block: suspend () -> T): FirestoreResult<T> =
    try {
        FirestoreResult.Success(block())
    } catch (error: Exception) {
        FirestoreResult.Error(error)
    }
