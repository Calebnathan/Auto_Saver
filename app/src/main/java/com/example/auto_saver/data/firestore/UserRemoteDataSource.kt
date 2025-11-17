package com.example.auto_saver.data.firestore

import com.example.auto_saver.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface UserRemoteDataSource {
    suspend fun fetchUser(uid: String): FirestoreResult<UserProfile>
    suspend fun createOrUpdateUser(profile: UserProfile, isNew: Boolean): FirestoreResult<UserProfile>
}

class FirestoreUserRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserRemoteDataSource {

    override suspend fun fetchUser(uid: String): FirestoreResult<UserProfile> = withContext(dispatcher) {
        safeFirestoreCall {
            val snapshot = firestore.collection("users").document(uid).get().await()
            snapshot.toUserProfile() ?: throw NoSuchElementException("User $uid not found")
        }
    }

    override suspend fun createOrUpdateUser(
        profile: UserProfile,
        isNew: Boolean
    ): FirestoreResult<UserProfile> = withContext(dispatcher) {
        safeFirestoreCall {
            firestore.collection("users")
                .document(profile.uid)
                .set(profile.toFirestorePayload(isNew), SetOptions.merge())
                .await()
            profile
        }
    }
}
