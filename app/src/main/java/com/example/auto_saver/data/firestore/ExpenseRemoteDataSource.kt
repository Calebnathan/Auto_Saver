package com.example.auto_saver.data.firestore

import com.example.auto_saver.data.model.ExpenseRecord
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

interface ExpenseRemoteDataSource {
    fun observeExpenses(uid: String): Flow<List<ExpenseRecord>>
    suspend fun upsertExpense(uid: String, expense: ExpenseRecord): FirestoreResult<String>
    suspend fun deleteExpense(uid: String, expenseId: String): FirestoreResult<Unit>
}

class FirestoreExpenseRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ExpenseRemoteDataSource {

    override fun observeExpenses(uid: String): Flow<List<ExpenseRecord>> = callbackFlow {
        val registration: ListenerRegistration = firestore.collection("users")
            .document(uid)
            .collection("expenses")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val expenses = snapshot?.documents
                    ?.mapNotNull { it.toExpenseRecord(uid) }
                    .orEmpty()
                trySend(expenses).isSuccess
            }

        awaitClose { registration.remove() }
    }

    override suspend fun upsertExpense(
        uid: String,
        expense: ExpenseRecord
    ): FirestoreResult<String> = withContext(dispatcher) {
        safeFirestoreCall {
            val expensesRef = firestore.collection("users")
                .document(uid)
                .collection("expenses")
            val documentId = expense.id.ifBlank { expensesRef.document().id }
            val payload = expense.copy(id = documentId).toFirestorePayload(expense.id.isBlank())
            expensesRef.document(documentId).set(payload, SetOptions.merge()).await()
            documentId
        }
    }

    override suspend fun deleteExpense(
        uid: String,
        expenseId: String
    ): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall<Unit> {
            firestore.collection("users")
                .document(uid)
                .collection("expenses")
                .document(expenseId)
                .delete()
                .await()
            Unit
        }
    }
}
