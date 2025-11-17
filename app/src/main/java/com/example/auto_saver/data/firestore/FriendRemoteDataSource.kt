package com.example.auto_saver.data.firestore

import com.example.auto_saver.data.model.FriendProfile
import com.example.auto_saver.data.model.FriendRequest
import com.example.auto_saver.data.model.FriendRequestStatus
import com.example.auto_saver.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface FriendRemoteDataSource {
    fun observeFriends(uid: String): Flow<List<FriendProfile>>
    fun observeFriendRequests(uid: String): Flow<List<FriendRequest>>
    suspend fun sendFriendRequest(currentUid: String, currentEmail: String, targetEmail: String): FirestoreResult<Unit>
    suspend fun acceptFriendRequest(currentUid: String, requestId: String): FirestoreResult<Unit>
    suspend fun declineFriendRequest(currentUid: String, requestId: String): FirestoreResult<Unit>
    suspend fun removeFriend(currentUid: String, friendUid: String): FirestoreResult<Unit>
    fun cleanup()
}

class FirestoreFriendRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : FriendRemoteDataSource {

    private val activeListeners = mutableListOf<ListenerRegistration>()

    override fun observeFriends(uid: String): Flow<List<FriendProfile>> = callbackFlow {
        val registration = friendsCollection(uid)
            .orderBy("displayName", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val friends = snapshot?.documents?.mapNotNull { it.toFriendProfile() }.orEmpty()
                trySend(friends).isSuccess
            }
        addRegistration(registration)
        awaitClose { removeRegistration(registration) }
    }

    override fun observeFriendRequests(uid: String): Flow<List<FriendRequest>> = callbackFlow {
        val registration = friendRequestsCollection(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val requests = snapshot?.documents?.mapNotNull { it.toFriendRequest() }?.filter {
                    it.status == FriendRequestStatus.PENDING
                }.orEmpty()
                trySend(requests).isSuccess
            }
        addRegistration(registration)
        awaitClose { removeRegistration(registration) }
    }

    override suspend fun sendFriendRequest(
        currentUid: String,
        currentEmail: String,
        targetEmail: String
    ): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall<Unit> {
            val sanitizedTarget = targetEmail.trim().lowercase()
            require(currentEmail.trim().lowercase() != sanitizedTarget) { "You cannot add yourself." }

            // Query public_users collection instead of private users collection
            val targetSnapshot = firestore.collection("public_users")
                .whereEqualTo("emailLowercase", sanitizedTarget)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?: firestore.collection("public_users")
                    .whereEqualTo("email", targetEmail)
                    .limit(1)
                    .get()
                    .await()
                    .documents
                    .firstOrNull()
                    ?: throw NoSuchElementException("No Auto Saver account found for $targetEmail")

            val targetUid = targetSnapshot.id
            val existingFriend = friendsCollection(currentUid).document(targetUid).get().await()
            if (existingFriend.exists()) {
                throw IllegalStateException("You are already friends with this user")
            }

            val requestRef = friendRequestsCollection(targetUid).document(currentUid)
            val now = FieldValue.serverTimestamp()
            val payload = mapOf(
                "fromUid" to currentUid,
                "toUid" to targetUid,
                "fromEmail" to currentEmail,
                "toEmail" to sanitizedTarget,
                "status" to FriendRequestStatus.PENDING.name,
                "createdAt" to now,
                "updatedAt" to now
            )
            requestRef.set(payload).await()
            Unit
        }
    }

    override suspend fun acceptFriendRequest(currentUid: String, requestId: String): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall<Unit> {
            val requestRef = friendRequestsCollection(currentUid).document(requestId)
            val requestSnapshot = requestRef.get().await()
            val request = requestSnapshot.toFriendRequest() ?: throw NoSuchElementException("Request not found")
            if (request.status != FriendRequestStatus.PENDING) {
                throw IllegalStateException("Request already processed")
            }

            val requesterProfile = firestore.collection("users").document(request.fromUid).get().await()
                .toUserProfile() ?: throw NoSuchElementException("Requester profile missing")
            val currentProfile = firestore.collection("users").document(currentUid).get().await()
                .toUserProfile() ?: throw NoSuchElementException("Your profile is missing")

            firestore.runBatch { batch ->
                batch.update(requestRef, mapOf(
                    "status" to FriendRequestStatus.ACCEPTED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))

                batch.set(
                    friendsCollection(currentUid).document(request.fromUid),
                    friendPayloadFromProfile(request.fromUid, requesterProfile)
                )

                batch.set(
                    friendsCollection(request.fromUid).document(currentUid),
                    friendPayloadFromProfile(currentUid, currentProfile)
                )
            }.await()
            Unit
        }
    }

    override suspend fun declineFriendRequest(currentUid: String, requestId: String): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall<Unit> {
            val requestRef = friendRequestsCollection(currentUid).document(requestId)
            requestRef.update(
                mapOf(
                    "status" to FriendRequestStatus.DECLINED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Unit
        }
    }

    override suspend fun removeFriend(currentUid: String, friendUid: String): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall<Unit> {
            firestore.runBatch { batch ->
                batch.delete(friendsCollection(currentUid).document(friendUid))
                batch.delete(friendsCollection(friendUid).document(currentUid))
            }.await()
            Unit
        }
    }

    override fun cleanup() {
        synchronized(activeListeners) {
            activeListeners.forEach { it.remove() }
            activeListeners.clear()
        }
    }

    private fun friendsCollection(uid: String) = firestore.collection("users")
        .document(uid)
        .collection("friends")

    private fun friendRequestsCollection(uid: String) = firestore.collection("users")
        .document(uid)
        .collection("friend_requests")

    private fun friendPayloadFromProfile(friendUid: String, profile: UserProfile): Map<String, Any?> {
        val now = FieldValue.serverTimestamp()
        val email = profile.contact.takeIf { it.isNotBlank() } ?: profile.contactLowercase.orEmpty()
        return mapOf(
            "friendUid" to friendUid,
            "friendEmail" to email.lowercase(),
            "displayName" to profile.fullName,
            "photoUrl" to profile.profilePhotoPath,
            "since" to now,
            "createdAt" to now,
            "updatedAt" to now
        )
    }

    private fun addRegistration(registration: ListenerRegistration) {
        synchronized(activeListeners) { activeListeners.add(registration) }
    }

    private fun removeRegistration(registration: ListenerRegistration) {
        registration.remove()
        synchronized(activeListeners) { activeListeners.remove(registration) }
    }
}