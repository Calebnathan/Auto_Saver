package com.example.auto_saver.data.repository

import com.example.auto_saver.Goal
import com.example.auto_saver.GoalDao
import com.example.auto_saver.UserPreferences
import com.example.auto_saver.data.firestore.FirestoreResult
import com.example.auto_saver.data.firestore.GoalRemoteDataSource
import com.example.auto_saver.data.model.GoalRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

class UnifiedGoalRepositoryTest {

    private lateinit var remoteDataSource: GoalRemoteDataSource
    private lateinit var goalDao: GoalDao
    private lateinit var userPreferences: UserPreferences
    private lateinit var repository: FirestoreFirstGoalRepository
    
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testUid = "test-uid-123"
    private val testLegacyUserId = 1

    @Before
    fun setup() {
        remoteDataSource = mock()
        goalDao = mock()
        userPreferences = mock()
        
        whenever(userPreferences.getCurrentUserId()).thenReturn(testLegacyUserId)
        
        repository = FirestoreFirstGoalRepository(
            remoteDataSource = remoteDataSource,
            goalDao = goalDao,
            userPreferences = userPreferences,
            dispatcher = testDispatcher
        )
    }

    @Test
    fun `observeGoals returns Firestore data and syncs to Room`() = runTest {
        val cloudGoals = listOf(
            GoalRecord("2025-01", testUid, "2025-01", 500.0, 1000.0, 1000L, 1000L),
            GoalRecord("2025-02", testUid, "2025-02", 600.0, 1200.0, 1000L, 1000L)
        )
        
        whenever(remoteDataSource.observeGoals(testUid))
            .thenReturn(flowOf(cloudGoals))
        whenever(goalDao.getGoalForMonth(testLegacyUserId, "2025-01"))
            .thenReturn(null)
        whenever(goalDao.getGoalForMonth(testLegacyUserId, "2025-02"))
            .thenReturn(null)

        val result = repository.observeGoals(testUid).first()

        assertEquals(2, result.size)
        assertEquals("2025-01", result[0].month)
        assertEquals(500.0, result[0].minGoal, 0.01)
        assertEquals("2025-02", result[1].month)
        assertEquals(600.0, result[1].minGoal, 0.01)
        verify(goalDao, atLeastOnce()).insertGoal(any())
    }

    @Test
    fun `observeGoals falls back to cache on Firestore error`() = runTest {
        whenever(remoteDataSource.observeGoals(testUid))
            .thenReturn(kotlinx.coroutines.flow.flow { throw RuntimeException("Network error") })

        val result = repository.observeGoals(testUid).first()

        assertEquals(0, result.size)
    }

    @Test
    fun `upsertGoal successfully creates goal in Firestore`() = runTest {
        val goal = GoalRecord("", testUid, "2025-01", 500.0, 1000.0, 0L, 0L)
        
        whenever(remoteDataSource.upsertGoal(eq(testUid), any()))
            .thenReturn(FirestoreResult.Success("2025-01"))
        whenever(goalDao.getGoalForMonth(testLegacyUserId, "2025-01"))
            .thenReturn(null)

        val result = repository.upsertGoal(testUid, goal)

        assertTrue(result.isSuccess)
        assertEquals("2025-01", result.getOrNull())
        verify(remoteDataSource).upsertGoal(eq(testUid), any())
        verify(goalDao).insertGoal(any())
    }

    @Test
    fun `upsertGoal uses month as ID if blank`() = runTest {
        val goal = GoalRecord("", testUid, "2025-03", 750.0, 1500.0, 0L, 0L)
        
        whenever(remoteDataSource.upsertGoal(eq(testUid), any()))
            .thenReturn(FirestoreResult.Success("2025-03"))
        whenever(goalDao.getGoalForMonth(testLegacyUserId, "2025-03"))
            .thenReturn(null)

        val result = repository.upsertGoal(testUid, goal)

        assertTrue(result.isSuccess)
        assertEquals("2025-03", result.getOrNull())
    }

    @Test
    fun `upsertGoal updates existing goal`() = runTest {
        val existingGoal = Goal(1, testLegacyUserId, "2025-01", 500.0, 1000.0)
        val updatedGoal = GoalRecord("2025-01", testUid, "2025-01", 600.0, 1200.0, 1000L, 1000L)
        
        whenever(remoteDataSource.upsertGoal(eq(testUid), any()))
            .thenReturn(FirestoreResult.Success("2025-01"))
        whenever(goalDao.getGoalForMonth(testLegacyUserId, "2025-01"))
            .thenReturn(existingGoal)

        val result = repository.upsertGoal(testUid, updatedGoal)

        assertTrue(result.isSuccess)
        assertEquals("2025-01", result.getOrNull())
        verify(remoteDataSource).upsertGoal(eq(testUid), any())
    }

    @Test
    fun `upsertGoal returns error on Firestore failure`() = runTest {
        val goal = GoalRecord("", testUid, "2025-01", 500.0, 1000.0, 0L, 0L)
        val exception = Exception("Firestore error")
        
        whenever(remoteDataSource.upsertGoal(eq(testUid), any()))
            .thenReturn(FirestoreResult.Error(exception))

        val result = repository.upsertGoal(testUid, goal)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `deleteGoal removes from Firestore and Room cache`() = runTest {
        val goalId = "2025-01"
        val roomGoal = Goal(1, testLegacyUserId, "2025-01", 500.0, 1000.0)
        
        whenever(remoteDataSource.deleteGoal(testUid, goalId))
            .thenReturn(FirestoreResult.Success(Unit))
        whenever(goalDao.getGoalForMonth(testLegacyUserId, goalId))
            .thenReturn(roomGoal)

        val result = repository.deleteGoal(testUid, goalId)

        assertTrue(result.isSuccess)
        verify(remoteDataSource).deleteGoal(testUid, goalId)
        verify(goalDao).deleteGoal(roomGoal)
    }

    @Test
    fun `deleteGoal returns error on Firestore failure`() = runTest {
        val goalId = "2025-01"
        val exception = Exception("Firestore error")
        
        whenever(remoteDataSource.deleteGoal(testUid, goalId))
            .thenReturn(FirestoreResult.Error(exception))

        val result = repository.deleteGoal(testUid, goalId)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `getGoalForMonth returns cached goal`() = runTest {
        val month = "2025-01"
        val roomGoal = Goal(1, testLegacyUserId, month, 500.0, 1000.0)
        
        whenever(goalDao.getGoalForMonth(testLegacyUserId, month))
            .thenReturn(roomGoal)

        val result = repository.getGoalForMonth(testUid, month)

        assertNotNull(result)
        assertEquals(month, result?.month)
        assertEquals(500.0, result?.minGoal ?: 0.0, 0.01)
    }

    @Test
    fun `getGoalForMonth returns null if not found`() = runTest {
        val month = "2025-01"
        
        whenever(goalDao.getGoalForMonth(testLegacyUserId, month))
            .thenReturn(null)

        val result = repository.getGoalForMonth(testUid, month)

        assertNull(result)
    }

    @Test
    fun `syncToRoom updates existing goals by month`() = runTest {
        val existingGoal = Goal(5, testLegacyUserId, "2025-01", 400.0, 800.0)
        val cloudGoals = listOf(
            GoalRecord("2025-01", testUid, "2025-01", 500.0, 1000.0, 2000L, 2000L)
        )
        
        whenever(remoteDataSource.observeGoals(testUid))
            .thenReturn(flowOf(cloudGoals))
        whenever(goalDao.getGoalForMonth(testLegacyUserId, "2025-01"))
            .thenReturn(existingGoal)

        repository.observeGoals(testUid).first()

        verify(goalDao).insertGoal(any())
    }
}
