package com.example.auto_saver.data.repository

import android.util.Log
import com.example.auto_saver.data.firestore.FirestoreResult
import com.example.auto_saver.data.firestore.SocialRemoteDataSource
import com.example.auto_saver.data.model.CollaborativeGoal
import com.example.auto_saver.data.model.GoalCategory
import com.example.auto_saver.data.model.GoalContribution
import com.example.auto_saver.data.model.GoalStatus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.withContext

interface UnifiedSocialRepository {
    fun observeMyCollaborativeGoals(uid: String): Flow<List<CollaborativeGoal>>
    fun observeGoalDetails(goalId: String): Flow<CollaborativeGoal?>
    fun observeGoalContributions(goalId: String): Flow<List<GoalContribution>>
    suspend fun createCollaborativeGoal(goal: CollaborativeGoal): Result<String>
    suspend fun addParticipant(goalId: String, participantUid: String, inviterUid: String): Result<Unit>
    suspend fun updateGoalCategory(goalId: String, category: GoalCategory): Result<Unit>
    suspend fun addContribution(contribution: GoalContribution): Result<String>
    suspend fun updateGoalStatus(goalId: String, status: GoalStatus, updaterUid: String): Result<Unit>
    suspend fun leaveGoal(goalId: String, uid: String): Result<Unit>
    suspend fun deleteGoal(goalId: String, creatorUid: String): Result<Unit>
    fun cleanup()
}

class FirestoreSocialRepository(
    private val remoteDataSource: SocialRemoteDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UnifiedSocialRepository {

    override fun observeMyCollaborativeGoals(uid: String): Flow<List<CollaborativeGoal>> {
        return remoteDataSource.observeMyCollaborativeGoals(uid)
            .catch { error ->
                Log.e(TAG, "Failed to observe collaborative goals", error)
                emit(emptyList())
            }
    }

    override fun observeGoalDetails(goalId: String): Flow<CollaborativeGoal?> {
        return remoteDataSource.observeGoalDetails(goalId)
            .catch { error ->
                Log.e(TAG, "Failed to observe goal details", error)
                emit(null)
            }
    }

    override fun observeGoalContributions(goalId: String): Flow<List<GoalContribution>> {
        return remoteDataSource.observeGoalContributions(goalId)
            .catch { error ->
                Log.e(TAG, "Failed to observe goal contributions", error)
                emit(emptyList())
            }
    }

    override suspend fun createCollaborativeGoal(goal: CollaborativeGoal): Result<String> = withContext(dispatcher) {
        when (val result = remoteDataSource.createCollaborativeGoal(goal)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Collaborative goal created: ${result.data}")
                Result.success(result.data)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to create collaborative goal", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun addParticipant(
        goalId: String,
        participantUid: String,
        inviterUid: String
    ): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.addParticipant(goalId, participantUid, inviterUid)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Participant added to goal $goalId")
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to add participant", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun updateGoalCategory(goalId: String, category: GoalCategory): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.updateGoalCategory(goalId, category)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Goal category updated: ${category.name}")
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to update goal category", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun addContribution(contribution: GoalContribution): Result<String> = withContext(dispatcher) {
        when (val result = remoteDataSource.addContribution(contribution)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Contribution added: ${result.data}")
                Result.success(result.data)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to add contribution", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun updateGoalStatus(
        goalId: String,
        status: GoalStatus,
        updaterUid: String
    ): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.updateGoalStatus(goalId, status, updaterUid)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Goal status updated to $status")
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to update goal status", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun leaveGoal(goalId: String, uid: String): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.leaveGoal(goalId, uid)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Left goal: $goalId")
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to leave goal", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun deleteGoal(goalId: String, creatorUid: String): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.deleteGoal(goalId, creatorUid)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Goal deleted: $goalId")
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to delete goal", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override fun cleanup() {
        remoteDataSource.cleanup()
        Log.d(TAG, "Repository cleaned up")
    }

    companion object {
        private const val TAG = "SocialRepository"
    }
}