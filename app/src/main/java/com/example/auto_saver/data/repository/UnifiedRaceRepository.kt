package com.example.auto_saver.data.repository

import android.util.Log
import com.example.auto_saver.data.firestore.FirestoreResult
import com.example.auto_saver.data.firestore.RaceRemoteDataSource
import com.example.auto_saver.data.model.ChallengeStatus
import com.example.auto_saver.data.model.RaceChallenge
import com.example.auto_saver.data.model.RaceParticipant
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

interface UnifiedRaceRepository {
    fun observeMyChallenges(uid: String): Flow<List<RaceChallenge>>
    fun observeChallengeDetails(challengeId: String): Flow<RaceChallenge?>
    fun observeLeaderboard(challengeId: String): Flow<List<RaceParticipant>>
    suspend fun createChallenge(challenge: RaceChallenge): Result<String>
    suspend fun joinChallengeByCode(uid: String, email: String, displayName: String?, inviteCode: String): Result<String>
    suspend fun updateParticipantSpending(challengeId: String, uid: String, totalSpent: Double): Result<Unit>
    suspend fun startChallenge(challengeId: String, creatorUid: String): Result<Unit>
    suspend fun endChallenge(challengeId: String, creatorUid: String): Result<Unit>
    suspend fun leaveChallenge(challengeId: String, uid: String): Result<Unit>
    suspend fun deleteChallenge(challengeId: String, creatorUid: String): Result<Unit>
    suspend fun syncMySpendingForActiveChallenges(uid: String): Result<Unit>
    fun cleanup()
}

class FirestoreRaceRepository(
    private val remoteDataSource: RaceRemoteDataSource,
    private val expenseRepository: UnifiedExpenseRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UnifiedRaceRepository {

    override fun observeMyChallenges(uid: String): Flow<List<RaceChallenge>> {
        return remoteDataSource.observeMyChallenges(uid)
            .catch { error ->
                Log.e(TAG, "Failed to observe challenges", error)
                emit(emptyList())
            }
    }

    override fun observeChallengeDetails(challengeId: String): Flow<RaceChallenge?> {
        return remoteDataSource.observeChallengeDetails(challengeId)
            .catch { error ->
                Log.e(TAG, "Failed to observe challenge details", error)
                emit(null)
            }
    }

    override fun observeLeaderboard(challengeId: String): Flow<List<RaceParticipant>> {
        return remoteDataSource.observeLeaderboard(challengeId)
            .catch { error ->
                Log.e(TAG, "Failed to observe leaderboard", error)
                emit(emptyList())
            }
    }

    override suspend fun createChallenge(challenge: RaceChallenge): Result<String> = withContext(dispatcher) {
        when (val result = remoteDataSource.createChallenge(challenge)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Challenge created: ${result.data}")
                Result.success(result.data)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to create challenge", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun joinChallengeByCode(
        uid: String,
        email: String,
        displayName: String?,
        inviteCode: String
    ): Result<String> = withContext(dispatcher) {
        when (val result = remoteDataSource.joinChallengeByCode(uid, email, displayName, inviteCode)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Joined challenge: ${result.data}")
                
                try {
                    syncSpendingForChallenge(result.data, uid)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to sync spending after joining", e)
                }
                
                Result.success(result.data)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to join challenge", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun updateParticipantSpending(
        challengeId: String,
        uid: String,
        totalSpent: Double
    ): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.updateParticipantSpending(challengeId, uid, totalSpent)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Updated spending for challenge $challengeId: $totalSpent")
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to update participant spending", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun startChallenge(challengeId: String, creatorUid: String): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.startChallenge(challengeId, creatorUid)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Challenge started: $challengeId")
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to start challenge", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun endChallenge(challengeId: String, creatorUid: String): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.endChallenge(challengeId, creatorUid)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Challenge ended: $challengeId")
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to end challenge", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun leaveChallenge(challengeId: String, uid: String): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.leaveChallenge(challengeId, uid)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Left challenge: $challengeId")
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to leave challenge", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun deleteChallenge(challengeId: String, creatorUid: String): Result<Unit> = withContext(dispatcher) {
        when (val result = remoteDataSource.deleteChallenge(challengeId, creatorUid)) {
            is FirestoreResult.Success -> {
                Log.d(TAG, "Challenge deleted: $challengeId")
                Result.success(Unit)
            }
            is FirestoreResult.Error -> {
                Log.e(TAG, "Failed to delete challenge", result.throwable)
                Result.failure(result.throwable)
            }
        }
    }

    override suspend fun syncMySpendingForActiveChallenges(uid: String): Result<Unit> = withContext(dispatcher) {
        try {
            val challenges = observeMyChallenges(uid).first()
            val activeChallenges = challenges.filter { 
                it.status == ChallengeStatus.ACTIVE || it.status == ChallengeStatus.PENDING
            }
            
            Log.d(TAG, "Syncing spending for ${activeChallenges.size} active challenges")
            
            for (challenge in activeChallenges) {
                try {
                    syncSpendingForChallenge(challenge.id, uid, challenge.startDate, challenge.endDate)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync spending for challenge ${challenge.id}", e)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync spending for active challenges", e)
            Result.failure(e)
        }
    }

    private suspend fun syncSpendingForChallenge(
        challengeId: String,
        uid: String,
        startDate: String? = null,
        endDate: String? = null
    ) {
        val challenge = if (startDate == null || endDate == null) {
            observeChallengeDetails(challengeId).first() ?: return
        } else {
            null
        }
        
        val actualStartDate = startDate ?: challenge?.startDate ?: return
        val actualEndDate = endDate ?: challenge?.endDate ?: return
        
        val totalSpent = expenseRepository.getTotalSpent(uid, actualStartDate, actualEndDate).first()
        
        Log.d(TAG, "Syncing challenge $challengeId: spent $totalSpent from $actualStartDate to $actualEndDate")
        
        updateParticipantSpending(challengeId, uid, totalSpent)
    }

    override fun cleanup() {
        remoteDataSource.cleanup()
        Log.d(TAG, "Repository cleaned up")
    }

    companion object {
        private const val TAG = "RaceRepository"
    }
}