package com.example.auto_saver.data.firestore

import com.example.auto_saver.data.model.ChallengeStatus
import com.example.auto_saver.data.model.RaceChallenge
import com.example.auto_saver.data.model.RaceParticipant
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
import kotlin.random.Random

interface RaceRemoteDataSource {
    fun observeMyChallenges(uid: String): Flow<List<RaceChallenge>>
    fun observeChallengeDetails(challengeId: String): Flow<RaceChallenge?>
    fun observeLeaderboard(challengeId: String): Flow<List<RaceParticipant>>
    suspend fun createChallenge(challenge: RaceChallenge): FirestoreResult<String>
    suspend fun joinChallengeByCode(uid: String, email: String, displayName: String?, inviteCode: String): FirestoreResult<String>
    suspend fun updateParticipantSpending(challengeId: String, uid: String, totalSpent: Double): FirestoreResult<Unit>
    suspend fun startChallenge(challengeId: String, creatorUid: String): FirestoreResult<Unit>
    suspend fun endChallenge(challengeId: String, creatorUid: String): FirestoreResult<Unit>
    suspend fun leaveChallenge(challengeId: String, uid: String): FirestoreResult<Unit>
    suspend fun deleteChallenge(challengeId: String, creatorUid: String): FirestoreResult<Unit>
    fun cleanup()
}

class FirestoreRaceRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : RaceRemoteDataSource {

    private val activeListeners = mutableListOf<ListenerRegistration>()

    override fun observeMyChallenges(uid: String): Flow<List<RaceChallenge>> = callbackFlow {
        val registration = firestore.collection("challenges")
            .whereArrayContains("participants", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val challenges = snapshot?.documents?.mapNotNull { it.toRaceChallenge() }.orEmpty()
                trySend(challenges).isSuccess
            }
        addRegistration(registration)
        awaitClose { removeRegistration(registration) }
    }

    override fun observeChallengeDetails(challengeId: String): Flow<RaceChallenge?> = callbackFlow {
        val registration = firestore.collection("challenges")
            .document(challengeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val challenge = snapshot?.toRaceChallenge()
                trySend(challenge).isSuccess
            }
        addRegistration(registration)
        awaitClose { removeRegistration(registration) }
    }

    override fun observeLeaderboard(challengeId: String): Flow<List<RaceParticipant>> = callbackFlow {
        val registration = firestore.collection("challenges")
            .document(challengeId)
            .collection("participants")
            .orderBy("totalSpent", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val participants = snapshot?.documents?.mapNotNull { it.toRaceParticipant() }.orEmpty()
                val rankedParticipants = participants.mapIndexed { index, participant ->
                    participant.copy(rank = index + 1)
                }
                trySend(rankedParticipants).isSuccess
            }
        addRegistration(registration)
        awaitClose { removeRegistration(registration) }
    }

    override suspend fun createChallenge(challenge: RaceChallenge): FirestoreResult<String> = withContext(dispatcher) {
        safeFirestoreCall {
            val inviteCode = generateUniqueInviteCode()
            val challengeRef = firestore.collection("challenges").document()
            val challengeId = challengeRef.id
            
            val challengeData = challenge.copy(
                id = challengeId,
                inviteCode = inviteCode,
                participants = listOf(challenge.createdBy),
                status = ChallengeStatus.PENDING
            )
            
            firestore.runBatch { batch ->
                batch.set(challengeRef, challengeData.toFirestorePayload(isNew = true))
                
                val participantRef = challengeRef.collection("participants").document(challenge.createdBy)
                val participantData = RaceParticipant(
                    uid = challenge.createdBy,
                    email = challenge.createdByEmail,
                    displayName = null,
                    totalSpent = 0.0,
                    rank = 1,
                    joinedAt = System.currentTimeMillis(),
                    lastSyncedAt = 0L
                )
                batch.set(participantRef, participantData.toFirestorePayload(isNew = true))
            }.await()
            
            challengeId
        }
    }

    override suspend fun joinChallengeByCode(
        uid: String,
        email: String,
        displayName: String?,
        inviteCode: String
    ): FirestoreResult<String> = withContext(dispatcher) {
        safeFirestoreCall {
            val sanitizedCode = inviteCode.trim().uppercase()
            
            val querySnapshot = firestore.collection("challenges")
                .whereEqualTo("inviteCode", sanitizedCode)
                .limit(1)
                .get()
                .await()
            
            val challengeDoc = querySnapshot.documents.firstOrNull()
                ?: throw NoSuchElementException("Challenge not found with code: $sanitizedCode")
            
            val challenge = challengeDoc.toRaceChallenge()
                ?: throw IllegalStateException("Invalid challenge data")
            
            if (uid in challenge.participants) {
                throw IllegalStateException("You are already in this challenge")
            }
            
            if (challenge.status == ChallengeStatus.COMPLETED || challenge.status == ChallengeStatus.CANCELLED) {
                throw IllegalStateException("This challenge has ended")
            }
            
            val challengeRef = firestore.collection("challenges").document(challenge.id)
            val participantRef = challengeRef.collection("participants").document(uid)
            
            firestore.runBatch { batch ->
                batch.update(
                    challengeRef,
                    mapOf(
                        "participants" to FieldValue.arrayUnion(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                
                val participantData = RaceParticipant(
                    uid = uid,
                    email = email,
                    displayName = displayName,
                    totalSpent = 0.0,
                    rank = challenge.participants.size + 1,
                    joinedAt = System.currentTimeMillis(),
                    lastSyncedAt = 0L
                )
                batch.set(participantRef, participantData.toFirestorePayload(isNew = true))
            }.await()
            
            challenge.id
        }
    }

    override suspend fun updateParticipantSpending(
        challengeId: String,
        uid: String,
        totalSpent: Double
    ): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall {
            val participantRef = firestore.collection("challenges")
                .document(challengeId)
                .collection("participants")
                .document(uid)
            
            participantRef.update(
                mapOf(
                    "totalSpent" to totalSpent,
                    "lastSyncedAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            
            Unit
        }
    }

    override suspend fun startChallenge(challengeId: String, creatorUid: String): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall {
            val challengeRef = firestore.collection("challenges").document(challengeId)
            val challenge = challengeRef.get().await().toRaceChallenge()
                ?: throw NoSuchElementException("Challenge not found")
            
            if (challenge.createdBy != creatorUid) {
                throw IllegalStateException("Only the creator can start the challenge")
            }
            
            if (challenge.status != ChallengeStatus.PENDING) {
                throw IllegalStateException("Challenge is not in pending state")
            }
            
            challengeRef.update(
                mapOf(
                    "status" to ChallengeStatus.ACTIVE.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            
            Unit
        }
    }

    override suspend fun endChallenge(challengeId: String, creatorUid: String): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall {
            val challengeRef = firestore.collection("challenges").document(challengeId)
            val challenge = challengeRef.get().await().toRaceChallenge()
                ?: throw NoSuchElementException("Challenge not found")
            
            if (challenge.createdBy != creatorUid) {
                throw IllegalStateException("Only the creator can end the challenge")
            }
            
            if (challenge.status != ChallengeStatus.ACTIVE) {
                throw IllegalStateException("Challenge is not active")
            }
            
            challengeRef.update(
                mapOf(
                    "status" to ChallengeStatus.COMPLETED.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            
            Unit
        }
    }

    override suspend fun leaveChallenge(challengeId: String, uid: String): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall {
            val challengeRef = firestore.collection("challenges").document(challengeId)
            val challenge = challengeRef.get().await().toRaceChallenge()
                ?: throw NoSuchElementException("Challenge not found")
            
            if (challenge.createdBy == uid) {
                throw IllegalStateException("Creator cannot leave. Delete the challenge instead.")
            }
            
            if (uid !in challenge.participants) {
                throw IllegalStateException("You are not in this challenge")
            }
            
            val participantRef = challengeRef.collection("participants").document(uid)
            
            firestore.runBatch { batch ->
                batch.update(
                    challengeRef,
                    mapOf(
                        "participants" to FieldValue.arrayRemove(uid),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.delete(participantRef)
            }.await()
            
            Unit
        }
    }

    override suspend fun deleteChallenge(challengeId: String, creatorUid: String): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall {
            val challengeRef = firestore.collection("challenges").document(challengeId)
            val challenge = challengeRef.get().await().toRaceChallenge()
                ?: throw NoSuchElementException("Challenge not found")
            
            if (challenge.createdBy != creatorUid) {
                throw IllegalStateException("Only the creator can delete the challenge")
            }
            
            val participantsSnapshot = challengeRef.collection("participants").get().await()
            
            firestore.runBatch { batch ->
                participantsSnapshot.documents.forEach { batch.delete(it.reference) }
                batch.delete(challengeRef)
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

    private suspend fun generateUniqueInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        var code: String
        var attempts = 0
        val maxAttempts = 10
        
        do {
            code = (1..6)
                .map { chars[Random.nextInt(chars.length)] }
                .joinToString("")
            
            val existing = firestore.collection("challenges")
                .whereEqualTo("inviteCode", code)
                .limit(1)
                .get()
                .await()
            
            if (existing.isEmpty) {
                return code
            }
            
            attempts++
        } while (attempts < maxAttempts)
        
        throw IllegalStateException("Failed to generate unique invite code after $maxAttempts attempts")
    }

    private fun addRegistration(registration: ListenerRegistration) {
        synchronized(activeListeners) { activeListeners.add(registration) }
    }

    private fun removeRegistration(registration: ListenerRegistration) {
        registration.remove()
        synchronized(activeListeners) { activeListeners.remove(registration) }
    }
}