package com.example.auto_saver.data.firestore

import com.example.auto_saver.data.model.CategoryRecord
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

interface CategoryRemoteDataSource {
    fun observeCategories(uid: String): Flow<List<CategoryRecord>>
    suspend fun upsertCategory(uid: String, category: CategoryRecord): FirestoreResult<String>
    suspend fun deleteCategory(uid: String, categoryId: String): FirestoreResult<Unit>
    suspend fun getCategoryByName(uid: String, name: String): FirestoreResult<CategoryRecord?>
    fun cleanup()
}

class FirestoreCategoryRemoteDataSource(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CategoryRemoteDataSource {

    private val activeListeners = mutableListOf<ListenerRegistration>()

    override fun observeCategories(uid: String): Flow<List<CategoryRecord>> = callbackFlow {
        val registration: ListenerRegistration = firestore.collection("users")
            .document(uid)
            .collection("categories")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val categories = snapshot?.documents
                    ?.mapNotNull { it.toCategoryRecord(uid) }
                    .orEmpty()
                trySend(categories).isSuccess
            }

        synchronized(activeListeners) {
            activeListeners.add(registration)
        }

        awaitClose {
            registration.remove()
            synchronized(activeListeners) {
                activeListeners.remove(registration)
            }
        }
    }

    override suspend fun upsertCategory(
        uid: String,
        category: CategoryRecord
    ): FirestoreResult<String> = withContext(dispatcher) {
        safeFirestoreCall {
            val categoriesRef = firestore.collection("users")
                .document(uid)
                .collection("categories")

            val documentId = category.id.ifBlank { categoriesRef.document().id }
            val payload = category.copy(id = documentId).toFirestorePayload(category.id.isBlank())
            categoriesRef.document(documentId).set(payload, SetOptions.merge()).await()
            documentId
        }
    }

    override suspend fun deleteCategory(
        uid: String,
        categoryId: String
    ): FirestoreResult<Unit> = withContext(dispatcher) {
        safeFirestoreCall<Unit> {
            firestore.collection("users")
                .document(uid)
                .collection("categories")
                .document(categoryId)
                .delete()
                .await()
            Unit
        }
    }

    override suspend fun getCategoryByName(
        uid: String,
        name: String
    ): FirestoreResult<CategoryRecord?> = withContext(dispatcher) {
        safeFirestoreCall {
            val snapshot = firestore.collection("users")
                .document(uid)
                .collection("categories")
                .whereEqualTo("name", name)
                .limit(1)
                .get()
                .await()

            snapshot.documents.firstOrNull()?.toCategoryRecord(uid)
        }
    }

    override fun cleanup() {
        synchronized(activeListeners) {
            activeListeners.forEach { it.remove() }
            activeListeners.clear()
        }
    }
}
