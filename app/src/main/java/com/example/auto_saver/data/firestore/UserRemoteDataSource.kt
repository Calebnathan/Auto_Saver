package com.example.auto_saver.data.firestore

import android.net.Uri
import com.example.auto_saver.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface UserRemoteDataSource {
    suspend fun fetchUser(uid: String): FirestoreResult<UserProfile>
    suspend fun createOrUpdateUser(profile: UserProfile, isNew: Boolean): FirestoreResult<UserProfile>
    suspend fun uploadProfilePhoto(uid: String, photoUri: Uri): FirestoreResult<String>
}

class FirestoreUserRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
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

    override suspend fun uploadProfilePhoto(uid: String, photoUri: Uri): FirestoreResult<String> =
        withContext(dispatcher) {
            safeFirestoreCall {
                val reference = storage.reference.child("profile_photos/$uid/profile.jpg")
                reference.putFile(photoUri).await()
                reference.downloadUrl.await().toString()
            }
        }
}
