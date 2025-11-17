package com.example.auto_saver.data.repository

import com.example.auto_saver.Category
import com.example.auto_saver.CategoryDao
import com.example.auto_saver.UserPreferences
import com.example.auto_saver.data.firestore.CategoryRemoteDataSource
import com.example.auto_saver.data.firestore.FirestoreResult
import com.example.auto_saver.data.mappers.toCloudRecord
import com.example.auto_saver.data.mappers.toRoomEntity
import com.example.auto_saver.data.model.CategoryRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import android.util.Log

interface UnifiedCategoryRepository {
    fun observeCategories(uid: String): Flow<List<CategoryRecord>>
    suspend fun createCategory(uid: String, name: String): Result<CategoryRecord>
    suspend fun updateCategory(uid: String, category: CategoryRecord): Result<CategoryRecord>
    suspend fun deleteCategory(uid: String, categoryId: String): Result<Unit>
}

class FirestoreFirstCategoryRepository(
    private val remoteDataSource: CategoryRemoteDataSource,
    private val categoryDao: CategoryDao,
    private val userPreferences: UserPreferences,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UnifiedCategoryRepository {

    private val categoryIdMap = ConcurrentHashMap<String, Int>()

    override fun observeCategories(uid: String): Flow<List<CategoryRecord>> {
        return remoteDataSource.observeCategories(uid)
            .onEach { categories ->
                try {
                    syncToRoom(categories)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync categories to Room", e)
                }
            }
            .catch { error ->
                Log.w(TAG, "Firestore fetch failed, falling back to cache", error)
                emit(getCachedCategories(uid))
            }
    }

    override suspend fun createCategory(uid: String, name: String): Result<CategoryRecord> = withContext(dispatcher) {
        when (val existingResult = remoteDataSource.getCategoryByName(uid, name)) {
            is FirestoreResult.Success -> {
                if (existingResult.data != null) {
                    Log.w(TAG, "Category with name '$name' already exists")
                    return@withContext Result.failure(
                        IllegalArgumentException("Category with name '$name' already exists")
                    )
                }
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Error checking for existing category", existingResult.throwable)
            }
        }

        val newCategory = CategoryRecord(
            id = "",
            uid = uid,
            name = name,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        when (val result = remoteDataSource.upsertCategory(uid, newCategory)) {
            is FirestoreResult.Success -> {
                val createdCategory = newCategory.copy(id = result.data)
                cacheCategory(createdCategory)
                Result.success(createdCategory)
            }
            is FirestoreResult.Error -> {
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun updateCategory(uid: String, category: CategoryRecord): Result<CategoryRecord> = withContext(dispatcher) {
        val updatedCategory = category.copy(updatedAt = System.currentTimeMillis())
        
        when (val result = remoteDataSource.upsertCategory(uid, updatedCategory)) {
            is FirestoreResult.Success -> {
                cacheCategory(updatedCategory)
                Result.success(updatedCategory)
            }
            is FirestoreResult.Error -> {
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun deleteCategory(uid: String, categoryId: String): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.deleteCategory(uid, categoryId)) {
            is FirestoreResult.Success -> {
                removeCategoryFromCache(categoryId)
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Result.failure(result.throwable)
            }
        }
    }

    private suspend fun syncToRoom(categories: List<CategoryRecord>) {
        val legacyUserId = userPreferences.getCurrentUserId()
        if (legacyUserId == -1) {
            Log.d(TAG, "Skipping Room sync - no legacy user ID")
            return
        }

        val existingCategories = categoryDao.getCategoriesByUser(legacyUserId)
        val existingByName = existingCategories.associateBy { it.name.lowercase() }

        categories.forEach { cloudCategory ->
            try {
                val existing = existingByName[cloudCategory.name.lowercase()]
                if (existing != null) {
                    categoryIdMap[cloudCategory.id] = existing.id
                } else {
                    val roomEntity = cloudCategory.toRoomEntity(legacyUserId)
                    categoryDao.insertCategory(roomEntity)
                    val inserted = categoryDao.getCategoriesByUser(legacyUserId)
                        .find { it.name == cloudCategory.name }
                    inserted?.let {
                        categoryIdMap[cloudCategory.id] = it.id
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync category ${cloudCategory.name} to Room", e)
            }
        }
    }

    private suspend fun cacheCategory(category: CategoryRecord) {
        val legacyUserId = userPreferences.getCurrentUserId()
        if (legacyUserId == -1) return

        val existing = categoryDao.getCategoriesByUser(legacyUserId)
            .find { it.name.equals(category.name, ignoreCase = true) }
        
        if (existing != null) {
            categoryIdMap[category.id] = existing.id
        } else {
            val roomEntity = category.toRoomEntity(legacyUserId)
            categoryDao.insertCategory(roomEntity)
            val inserted = categoryDao.getCategoriesByUser(legacyUserId)
                .find { it.name == category.name }
            inserted?.let {
                categoryIdMap[category.id] = it.id
            }
        }
    }

    private suspend fun removeCategoryFromCache(categoryId: String) {
        val legacyUserId = userPreferences.getCurrentUserId()
        if (legacyUserId == -1) return

        val roomId = categoryIdMap[categoryId]
        if (roomId != null) {
            categoryDao.getCategoryById(roomId)?.let { category ->
                categoryDao.deleteCategory(category)
                categoryIdMap.remove(categoryId)
            }
        }
    }

    private suspend fun getCachedCategories(uid: String): List<CategoryRecord> {
        val legacyUserId = userPreferences.getCurrentUserId()
        if (legacyUserId == -1) return emptyList()

        return categoryDao.getCategoriesByUser(legacyUserId)
            .map { it.toCloudRecord(uid, "") }
    }

    fun getRoomCategoryId(cloudCategoryId: String): Int? {
        return categoryIdMap[cloudCategoryId]
    }
    
    fun getCloudCategoryId(roomCategoryId: Int): String? {
        return categoryIdMap.entries.find { it.value == roomCategoryId }?.key
    }
    
    fun clearCache() {
        categoryIdMap.clear()
        Log.d(TAG, "Category ID mapping cache cleared")
    }
    
    companion object {
        private const val TAG = "CategoryRepository"
    }
}
