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
            // Write to both users and public_users collections
            val userPayload = profile.toFirestorePayload(isNew)
            val publicUserPayload = profile.toPublicUserPayload(isNew)
            
            // Use batch write to ensure consistency
            firestore.runBatch { batch ->
                batch.set(
                    firestore.collection("users").document(profile.uid),
                    userPayload,
                    SetOptions.merge()
                )
                batch.set(
                    firestore.collection("public_users").document(profile.uid),
                    publicUserPayload,
                    SetOptions.merge()
                )
            }.await()
            
            profile
        }
    }
    
    private fun UserProfile.toPublicUserPayload(isNew: Boolean): Map<String, Any?> =
        hashMapOf<String, Any?>(
            "email" to contact,
            "emailLowercase" to contact.lowercase(),
            "displayName" to fullName,
            "photoUrl" to profilePhotoPath,
            "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        ).apply {
            if (isNew) put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp())
        }
}