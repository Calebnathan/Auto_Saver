package com.example.auto_saver.data.repository

import android.content.Context
import android.net.Uri
import com.example.auto_saver.ExpenseDao
import com.example.auto_saver.UserPreferences
import com.example.auto_saver.data.firestore.ExpenseRemoteDataSource
import com.example.auto_saver.data.firestore.FirestoreResult
import com.example.auto_saver.data.mappers.toCloudRecord
import com.example.auto_saver.data.mappers.toRoomEntity
import com.example.auto_saver.data.model.ExpenseRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import android.util.Log
import java.io.File

data class CategorySummaryCloud(
    val categoryId: String,
    val categoryName: String,
    val total: Double,
    val percentage: Float,
    val expenseCount: Int
)

interface UnifiedExpenseRepository {
    fun observeExpenses(uid: String, startDate: String? = null, endDate: String? = null): Flow<List<ExpenseRecord>>
    suspend fun createExpense(uid: String, expense: ExpenseRecord, photoUri: Uri? = null): Result<String>
    suspend fun updateExpense(uid: String, expense: ExpenseRecord, photoUri: Uri? = null): Result<String>
    suspend fun deleteExpense(uid: String, expenseId: String): Result<Unit>
    fun getTotalSpent(uid: String, startDate: String, endDate: String): Flow<Double>
    fun getCategorySummaries(uid: String, startDate: String, endDate: String): Flow<List<CategorySummaryCloud>>
}

class FirestoreFirstExpenseRepository(
    private val context: Context,
    private val remoteDataSource: ExpenseRemoteDataSource,
    private val expenseDao: ExpenseDao,
    private val userPreferences: UserPreferences,
    private val categoryRepository: FirestoreFirstCategoryRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UnifiedExpenseRepository {

    private val expenseIdMap = ConcurrentHashMap<String, Int>()

    private fun savePhotoLocally(uid: String, expenseId: String, uri: Uri): String? {
        return try {
            val photoFile = File(context.filesDir, "expense_photo_${uid}_${expenseId}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                photoFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            photoFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save expense photo locally", e)
            null
        }
    }

    override fun observeExpenses(uid: String, startDate: String?, endDate: String?): Flow<List<ExpenseRecord>> {
        return remoteDataSource.observeExpenses(uid)
            .map { expenses ->
                if (startDate != null && endDate != null) {
                    expenses.filter { it.date >= startDate && it.date <= endDate }
                } else {
                    expenses
                }
            }
            .onEach { expenses ->
                try {
                    syncToRoom(expenses)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync expenses to Room", e)
                }
            }
            .catch { error ->
                Log.w(TAG, "Firestore fetch failed, falling back to cache", error)
                emit(getCachedExpenses(uid, startDate, endDate))
            }
    }

    override suspend fun createExpense(uid: String, expense: ExpenseRecord, photoUri: Uri?): Result<String> = withContext(dispatcher) {
        val expenseId = if (expense.id.isBlank()) {
            java.util.UUID.randomUUID().toString()
        } else {
            expense.id
        }
        
        val photoPath = if (photoUri != null) {
            savePhotoLocally(uid, expenseId, photoUri)
        } else {
            null
        }
        
        val finalExpense = expense.copy(
            id = expenseId,
            photoPath = photoPath,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        when (val result = remoteDataSource.upsertExpense(uid, finalExpense)) {
            is FirestoreResult.Success -> {
                cacheExpense(finalExpense)
                Result.success(expenseId)
            }
            is FirestoreResult.Error -> {
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun updateExpense(uid: String, expense: ExpenseRecord, photoUri: Uri?): Result<String> = withContext(dispatcher) {
        var photoPath = expense.photoPath

        if (photoUri != null) {
            photoPath = savePhotoLocally(uid, expense.id, photoUri)
        }

        val expenseToSave = expense.copy(
            photoPath = photoPath,
            updatedAt = System.currentTimeMillis()
        )

        when (val result = remoteDataSource.upsertExpense(uid, expenseToSave)) {
            is FirestoreResult.Success -> {
                cacheExpense(expenseToSave)
                Result.success(result.data)
            }
            is FirestoreResult.Error -> {
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun deleteExpense(uid: String, expenseId: String): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.deleteExpense(uid, expenseId)) {
            is FirestoreResult.Success -> {
                removeExpenseFromCache(expenseId)
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Result.failure(result.throwable)
            }
        }
    }

    override fun getTotalSpent(uid: String, startDate: String, endDate: String): Flow<Double> {
        return observeExpenses(uid, startDate, endDate)
            .map { expenses -> expenses.sumOf { it.amount } }
    }

    override fun getCategorySummaries(uid: String, startDate: String, endDate: String): Flow<List<CategorySummaryCloud>> {
        return observeExpenses(uid, startDate, endDate)
            .map { expenses ->
                val total = expenses.sumOf { it.amount }
                val grouped = expenses.groupBy { it.categoryId }
                
                grouped.map { (categoryId, expenseList) ->
                    val categoryTotal = expenseList.sumOf { it.amount }
                    val percentage = if (total > 0) (categoryTotal / total * 100).toFloat() else 0f
                    
                    CategorySummaryCloud(
                        categoryId = categoryId,
                        categoryName = categoryId,
                        total = categoryTotal,
                        percentage = percentage,
                        expenseCount = expenseList.size
                    )
                }.sortedByDescending { it.total }
            }
    }

    private suspend fun syncToRoom(expenses: List<ExpenseRecord>) {
        val legacyUserId = userPreferences.getCurrentUserId()
        if (legacyUserId == -1) {
            Log.d(TAG, "Skipping Room sync - no legacy user ID")
            return
        }

        expenses.forEach { cloudExpense ->
            val roomCategoryId = categoryRepository.getRoomCategoryId(cloudExpense.categoryId)
            if (roomCategoryId == null) {
                Log.w(TAG, "Skipping expense ${cloudExpense.id} - category ${cloudExpense.categoryId} not mapped")
                return@forEach
            }
            
            try {
                val existing = expenseIdMap[cloudExpense.id]
                val roomEntity = cloudExpense.toRoomEntity(legacyUserId, roomCategoryId, existing ?: 0)
                expenseDao.insertExpense(roomEntity)
                
                if (existing == null) {
                    val allExpenses = expenseDao.getExpensesByUser(legacyUserId)
                    val inserted = allExpenses.find { 
                        it.date == cloudExpense.date && 
                        it.amount == cloudExpense.amount && 
                        it.categoryId == roomCategoryId 
                    }
                    inserted?.let {
                        expenseIdMap[cloudExpense.id] = it.id
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync expense ${cloudExpense.id} to Room", e)
            }
        }
    }

    private suspend fun cacheExpense(expense: ExpenseRecord) {
        val legacyUserId = userPreferences.getCurrentUserId()
        if (legacyUserId == -1) return

        val roomCategoryId = categoryRepository.getRoomCategoryId(expense.categoryId) ?: return
        
        val existing = expenseIdMap[expense.id]
        val roomEntity = expense.toRoomEntity(legacyUserId, roomCategoryId, existing ?: 0)
        expenseDao.insertExpense(roomEntity)
        
        if (existing == null) {
            val allExpenses = expenseDao.getExpensesByUser(legacyUserId)
            val inserted = allExpenses.find { 
                it.date == expense.date && 
                it.amount == expense.amount && 
                it.categoryId == roomCategoryId 
            }
            inserted?.let {
                expenseIdMap[expense.id] = it.id
            }
        }
    }

    private suspend fun removeExpenseFromCache(expenseId: String) {
        val legacyUserId = userPreferences.getCurrentUserId()
        if (legacyUserId == -1) return

        val roomId = expenseIdMap[expenseId]
        if (roomId != null) {
            val allExpenses = expenseDao.getExpensesByUser(legacyUserId)
            val expense = allExpenses.find { it.id == roomId }
            expense?.let {
                expenseDao.deleteExpense(it)
                expenseIdMap.remove(expenseId)
            }
        }
    }

    private suspend fun getCachedExpenses(uid: String, startDate: String?, endDate: String?): List<ExpenseRecord> {
        val legacyUserId = userPreferences.getCurrentUserId()
        if (legacyUserId == -1) return emptyList()

        val expenses = expenseDao.getExpensesByUser(legacyUserId)
        val cloudIdsByRoomId = expenseIdMap.entries.associateBy({ it.value }, { it.key })
        
        return expenses
            .filter { expense ->
                if (startDate != null && endDate != null) {
                    expense.date >= startDate && expense.date <= endDate
                } else {
                    true
                }
            }
            .mapNotNull { expense ->
                val cloudExpenseId = cloudIdsByRoomId[expense.id] ?: "unknown_${expense.id}"
                val cloudCategoryId = categoryRepository.getCloudCategoryId(expense.categoryId)
                    ?: return@mapNotNull null
                expense.toCloudRecord(uid, cloudCategoryId, cloudExpenseId, expense.photoPath)
            }
    }
    
    fun clearCache() {
        expenseIdMap.clear()
        Log.d(TAG, "Expense ID mapping cache cleared")
    }
    
    companion object {
        private const val TAG = "ExpenseRepository"
    }
}
