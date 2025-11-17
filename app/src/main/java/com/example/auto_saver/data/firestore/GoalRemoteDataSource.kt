package com.example.auto_saver.data.firestore

import com.example.auto_saver.data.model.GoalRecord
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

interface GoalRemoteDataSource {
    fun observeGoals(uid: String): Flow<List<GoalRecord>>
    suspend fun upsertGoal(uid: String, goal: GoalRecord): FirestoreResult<String>
    suspend fun deleteGoal(uid: String, goalId: String): FirestoreResult<Unit>
}

class FirestoreGoalRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : GoalRemoteDataSource {

    override fun observeGoals(uid: String): Flow<List<GoalRecord>> = callbackFlow {
        val registration: ListenerRegistration = firestore.collection("users")
            .document(uid)
            .collection("goals")
            .orderBy("month", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val goals = snapshot?.documents
                    ?.mapNotNull { it.toGoalRecord(uid) }
                    .orEmpty()
                trySend(goals).isSuccess
            }

        awaitClose { registration.remove() }
    }

    override suspend fun upsertGoal(uid: String, goal: GoalRecord): FirestoreResult<String> =
        withContext(dispatcher) {
            safeFirestoreCall {
                val goalsRef = firestore.collection("users")
                    .document(uid)
                    .collection("goals")
                val documentId = goal.id.ifBlank { goal.month }
                val payload = goal.copy(id = documentId).toFirestorePayload(goal.id.isBlank())
                goalsRef.document(documentId).set(payload, SetOptions.merge()).await()
                documentId
            }
        }

    override suspend fun deleteGoal(uid: String, goalId: String): FirestoreResult<Unit> =
        withContext(dispatcher) {
            safeFirestoreCall<Unit> {
                firestore.collection("users")
                    .document(uid)
                    .collection("goals")
                    .document(goalId)
                    .delete()
                    .await()
                Unit
            }
        }
}
