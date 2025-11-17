package com.example.auto_saver.data.firestore

import com.example.auto_saver.data.model.CollaborativeGoal
import com.example.auto_saver.data.model.GoalCategory
import com.example.auto_saver.data.model.GoalContribution
import com.example.auto_saver.data.model.GoalStatus
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

interface SocialRemoteDataSource {
    fun observeMyCollaborativeGoals(uid: String): Flow<List<CollaborativeGoal>>
    fun observeGoalDetails(goalId: String): Flow<CollaborativeGoal?>
    fun observeGoalContributions(goalId: String): Flow<List<GoalContribution>>
    suspend fun createCollaborativeGoal(goal: CollaborativeGoal): FirestoreResult<String>
    suspend fun addParticipant(goalId: String, participantUid: String, inviterUid: String): FirestoreResult<Unit>
    suspend fun updateGoalCategory(goalId: String, category: GoalCategory): FirestoreResult<Unit>
    suspend fun addContribution(contribution: GoalContribution): FirestoreResult<String>
    suspend fun updateGoalStatus(goalId: String, status: GoalStatus, updaterUid: String): FirestoreResult<Unit>
    suspend fun leaveGoal(goalId: String, uid: String): FirestoreResult<Unit>
    suspend fun deleteGoal(goalId: String, creatorUid: String): FirestoreResult<Unit>
    fun cleanup()
}

class FirestoreSocialRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : SocialRemoteDataSource {

    private val activeListeners = mutableListOf<ListenerRegistration>()

    override fun observeMyCollaborativeGoals(uid: String): Flow<List<CollaborativeGoal>> = callbackFlow {
        val registration = firestore.collection("collaborative_goals")
            .whereArrayContains("participants", uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val goals = snapshot?.documents?.mapNotNull { it.toCollaborativeGoal() }.orEmpty()
                trySend(goals).isSuccess
            }
        addRegistration(registration)
        awaitClose { removeRegistration(registration) }
    }

    override fun observeGoalDetails(goalId: String): Flow<CollaborativeGoal?> = callbackFlow {
        val registration = firestore.collection("collaborative_goals")
            .document(goalId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val goal = snapshot?.toCollaborativeGoal()
                trySend(goal).isSuccess
            }
        addRegistration(registration)
        awaitClose { removeRegistration(registration) }
    }

    override fun observeGoalContributions(goalId: String): Flow<List<GoalContribution>> = callbackFlow {
        val registration = firestore.collection("collaborative_goals")
            .document(goalId)
            .collection("contributions")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val contributions = snapshot?.documents?.mapNotNull { it.toGoalContribution() }.orEmpty()
                trySend(contributions).isSuccess
            }
        addRegistration(registration)
        awaitClose { removeRegistration(registration) }
    }

    override suspend fun createCollaborativeGoal(goal: CollaborativeGoal): FirestoreResult<String> = withContext(dispatcher) {
        safeFirestoreCall {
            val goalRef = firestore.collection("collaborative_goals").document()
            val goalId = goalRef.id
            
            val goalData = goal.copy(
                id = goalId,
                participants = listOf(goal.createdBy),
                status = GoalStatus.ACTIVE,
                currentSaved = 0.0
            )
            
            goalRef.set(goalData.toFirestorePayload(isNew = true)).await()
            
            goalId
        }
    }

    override suspend fun addParticipant(goalId: String, participantUid: String, inviterUid: String): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall {
            val goalRef = firestore.collection("collaborative_goals").document(goalId)
            val goal = goalRef.get().await().toCollaborativeGoal()
                ?: throw NoSuchElementException("Goal not found")
            
            if (participantUid in goal.participants) {
                throw IllegalStateException("User is already a participant")
            }
            
            if (inviterUid !in goal.participants) {
                throw IllegalStateException("Only participants can invite others")
            }
            
            if (goal.status != GoalStatus.ACTIVE) {
                throw IllegalStateException("Cannot join inactive goal")
            }
            
            goalRef.update(
                mapOf(
                    "participants" to FieldValue.arrayUnion(participantUid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            
            Unit
        }
    }

    override suspend fun updateGoalCategory(goalId: String, category: GoalCategory): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall {
            val goalRef = firestore.collection("collaborative_goals").document(goalId)
            val goal = goalRef.get().await().toCollaborativeGoal()
                ?: throw NoSuchElementException("Goal not found")
            
            val updatedCategories = goal.categories.toMutableList()
            val existingIndex = updatedCategories.indexOfFirst { it.id == category.id }
            
            if (existingIndex >= 0) {
                updatedCategories[existingIndex] = category
            } else {
                updatedCategories.add(category)
            }
            
            val totalSaved = updatedCategories.sumOf { it.saved }
            
            goalRef.update(
                mapOf(
                    "categories" to updatedCategories.map { it.toFirestorePayload(isNew = false) },
                    "currentSaved" to totalSaved,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            
            Unit
        }
    }

    override suspend fun addContribution(contribution: GoalContribution): FirestoreResult<String> = withContext(dispatcher) {
        safeFirestoreCall {
            val goalRef = firestore.collection("collaborative_goals").document(contribution.goalId)
            val goal = goalRef.get().await().toCollaborativeGoal()
                ?: throw NoSuchElementException("Goal not found")
            
            if (contribution.uid !in goal.participants) {
                throw IllegalStateException("Only participants can contribute")
            }
            
            val contributionRef = goalRef.collection("contributions").document()
            val contributionId = contributionRef.id
            
            val contributionData = contribution.copy(id = contributionId)
            
            val categoryIndex = goal.categories.indexOfFirst { it.id == contribution.categoryId }
            if (categoryIndex < 0) {
                throw IllegalArgumentException("Category not found in goal")
            }
            
            val updatedCategories = goal.categories.toMutableList()
            val category = updatedCategories[categoryIndex]
            val newSaved = category.saved + contribution.amount
            
            if (newSaved > category.budget) {
                throw IllegalArgumentException("Contribution of ${contribution.amount} would exceed category budget. Remaining: ${category.budget - category.saved}")
            }
            
            updatedCategories[categoryIndex] = category.copy(saved = newSaved)
            
            val totalSaved = updatedCategories.sumOf { it.saved }
            
            firestore.runBatch { batch ->
                batch.set(contributionRef, contributionData.toFirestorePayload(isNew = true))
                batch.update(
                    goalRef,
                    mapOf(
                        "categories" to updatedCategories.map { it.toFirestorePayload(isNew = false) },
                        "currentSaved" to totalSaved,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            
            contributionId
        }
    }

    override suspend fun updateGoalStatus(goalId: String, status: GoalStatus, updaterUid: String): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall {
            val goalRef = firestore.collection("collaborative_goals").document(goalId)
            val goal = goalRef.get().await().toCollaborativeGoal()
                ?: throw NoSuchElementException("Goal not found")
            
            if (goal.createdBy != updaterUid) {
                throw IllegalStateException("Only the creator can update goal status")
            }
            
            goalRef.update(
                mapOf(
                    "status" to status.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            
            Unit
        }
    }

    override suspend fun leaveGoal(goalId: String, uid: String): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall {
            val goalRef = firestore.collection("collaborative_goals").document(goalId)
            val goal = goalRef.get().await().toCollaborativeGoal()
                ?: throw NoSuchElementException("Goal not found")
            
            if (goal.createdBy == uid) {
                throw IllegalStateException("Creator cannot leave. Delete the goal instead.")
            }
            
            if (uid !in goal.participants) {
                throw IllegalStateException("You are not a participant in this goal")
            }
            
            goalRef.update(
                mapOf(
                    "participants" to FieldValue.arrayRemove(uid),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            
            Unit
        }
    }

    override suspend fun deleteGoal(goalId: String, creatorUid: String): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall {
            val goalRef = firestore.collection("collaborative_goals").document(goalId)
            val goal = goalRef.get().await().toCollaborativeGoal()
                ?: throw NoSuchElementException("Goal not found")
            
            if (goal.createdBy != creatorUid) {
                throw IllegalStateException("Only the creator can delete the goal")
            }
            
            val contributionsSnapshot = goalRef.collection("contributions").get().await()
            
            firestore.runBatch { batch ->
                contributionsSnapshot.documents.forEach { batch.delete(it.reference) }
                batch.delete(goalRef)
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

    private fun addRegistration(registration: ListenerRegistration) {
        synchronized(activeListeners) { activeListeners.add(registration) }
    }

    private fun removeRegistration(registration: ListenerRegistration) {
        registration.remove()
        synchronized(activeListeners) { activeListeners.remove(registration) }
    }
}