package com.example.auto_saver.data.repository

import com.example.auto_saver.GoalDao
import com.example.auto_saver.UserPreferences
import com.example.auto_saver.data.firestore.FirestoreResult
import com.example.auto_saver.data.firestore.GoalRemoteDataSource
import com.example.auto_saver.data.mappers.toCloudRecord
import com.example.auto_saver.data.mappers.toRoomEntity
import com.example.auto_saver.data.model.GoalRecord
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

interface UnifiedGoalRepository {
    fun observeGoals(uid: String): Flow<List<GoalRecord>>
    suspend fun upsertGoal(uid: String, goal: GoalRecord): Result<String>
    suspend fun deleteGoal(uid: String, goalId: String): Result<Unit>
    suspend fun getGoalForMonth(uid: String, month: String): GoalRecord?
}

class FirestoreFirstGoalRepository(
    private val remoteDataSource: GoalRemoteDataSource,
    private val goalDao: GoalDao,
    private val userPreferences: UserPreferences,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UnifiedGoalRepository {

    override fun observeGoals(uid: String): Flow<List<GoalRecord>> {
        return remoteDataSource.observeGoals(uid)
            .onEach { goals ->
                syncToRoom(goals)
            }
            .catch { error ->
                emit(getCachedGoals(uid))
            }
    }

    override suspend fun upsertGoal(uid: String, goal: GoalRecord): Result<String> = withContext(dispatcher) {
        val goalToSave = goal.copy(
            id = goal.id.ifBlank { goal.month },
            updatedAt = System.currentTimeMillis()
        )

        when (val result = remoteDataSource.upsertGoal(uid, goalToSave)) {
            is FirestoreResult.Success -> {
                cacheGoal(goalToSave)
                Result.success(result.data)
            }
            is FirestoreResult.Error -> {
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun deleteGoal(uid: String, goalId: String): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.deleteGoal(uid, goalId)) {
            is FirestoreResult.Success -> {
                removeGoalFromCache(goalId)
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun getGoalForMonth(uid: String, month: String): GoalRecord? = withContext(dispatcher) {
        val legacyUserId = userPreferences.getCurrentUserId()
        if (legacyUserId != -1) {
            val cached = goalDao.getGoalForMonth(legacyUserId, month)
            if (cached != null) {
                return@withContext cached.toCloudRecord(uid)
            }
        }
        null
    }

    private suspend fun syncToRoom(goals: List<GoalRecord>) {
        val legacyUserId = userPreferences.getCurrentUserId()
        if (legacyUserId == -1) return

        goals.forEach { cloudGoal ->
            val existing = goalDao.getGoalForMonth(legacyUserId, cloudGoal.month)
            val roomEntity = cloudGoal.toRoomEntity(legacyUserId, existing?.id ?: 0)
            goalDao.insertGoal(roomEntity)
        }
    }

    private suspend fun cacheGoal(goal: GoalRecord) {
        val legacyUserId = userPreferences.getCurrentUserId()
        if (legacyUserId == -1) return

        val existing = goalDao.getGoalForMonth(legacyUserId, goal.month)
        val roomEntity = goal.toRoomEntity(legacyUserId, existing?.id ?: 0)
        goalDao.insertGoal(roomEntity)
    }

    private suspend fun removeGoalFromCache(goalId: String) {
        val legacyUserId = userPreferences.getCurrentUserId()
        if (legacyUserId == -1) return

        val existing = goalDao.getGoalForMonth(legacyUserId, goalId)
        existing?.let {
            goalDao.deleteGoal(it)
        }
    }

    private suspend fun getCachedGoals(uid: String): List<GoalRecord> {
        return emptyList()
    }
}
