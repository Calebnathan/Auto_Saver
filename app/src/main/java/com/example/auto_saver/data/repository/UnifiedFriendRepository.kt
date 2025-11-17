package com.example.auto_saver.data.repository

import com.example.auto_saver.data.firestore.FriendRemoteDataSource
import com.example.auto_saver.data.firestore.FirestoreResult
import com.example.auto_saver.data.model.FriendProfile
import com.example.auto_saver.data.model.FriendRequest
import kotlinx.coroutines.flow.Flow

interface UnifiedFriendRepository {
    fun observeFriends(uid: String): Flow<List<FriendProfile>>
    fun observeFriendRequests(uid: String): Flow<List<FriendRequest>>
    suspend fun sendFriendRequest(currentUid: String, currentEmail: String, targetEmail: String): Result<Unit>
    suspend fun acceptFriendRequest(currentUid: String, requestId: String): Result<Unit>
    suspend fun declineFriendRequest(currentUid: String, requestId: String): Result<Unit>
    suspend fun removeFriend(currentUid: String, friendUid: String): Result<Unit>
}

class FirestoreFriendRepository(
    private val remoteDataSource: FriendRemoteDataSource
) : UnifiedFriendRepository {

    override fun observeFriends(uid: String): Flow<List<FriendProfile>> = remoteDataSource.observeFriends(uid)

    override fun observeFriendRequests(uid: String): Flow<List<FriendRequest>> = remoteDataSource.observeFriendRequests(uid)

    override suspend fun sendFriendRequest(
        currentUid: String,
        currentEmail: String,
        targetEmail: String
    ): Result<Unit> = remoteDataSource.sendFriendRequest(currentUid, currentEmail, targetEmail).toKotlinResult()

    override suspend fun acceptFriendRequest(currentUid: String, requestId: String): Result<Unit> =
        remoteDataSource.acceptFriendRequest(currentUid, requestId).toKotlinResult()

    override suspend fun declineFriendRequest(currentUid: String, requestId: String): Result<Unit> =
        remoteDataSource.declineFriendRequest(currentUid, requestId).toKotlinResult()

    override suspend fun removeFriend(currentUid: String, friendUid: String): Result<Unit> =
        remoteDataSource.removeFriend(currentUid, friendUid).toKotlinResult()

    private fun <T> FirestoreResult<T>.toKotlinResult(): Result<T> = when (this) {
        is FirestoreResult.Success -> Result.success(data)
        is FirestoreResult.Error -> Result.failure(throwable)
    }
}
