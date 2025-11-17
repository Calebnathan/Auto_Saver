package com.example.auto_saver.data.repository

import android.content.Context
import android.net.Uri
import com.example.auto_saver.Expense
import com.example.auto_saver.ExpenseDao
import com.example.auto_saver.UserPreferences
import com.example.auto_saver.data.firestore.ExpenseRemoteDataSource
import com.example.auto_saver.data.firestore.FirestoreResult
import com.example.auto_saver.data.model.ExpenseRecord
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

class UnifiedExpenseRepositoryTest {

    private lateinit var context: Context
    private lateinit var remoteDataSource: ExpenseRemoteDataSource
    private lateinit var expenseDao: ExpenseDao
    private lateinit var userPreferences: UserPreferences
    private lateinit var categoryRepository: FirestoreFirstCategoryRepository
    private lateinit var repository: FirestoreFirstExpenseRepository
    
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testUid = "test-uid-123"
    private val testLegacyUserId = 1
    private val testCategoryId = "cat1"
    private val testRoomCategoryId = 5

    @Before
    fun setup() {
        context = mock(Context::class.java)
        remoteDataSource = mock(ExpenseRemoteDataSource::class.java)
        expenseDao = mock(ExpenseDao::class.java)
        userPreferences = mock(UserPreferences::class.java)
        categoryRepository = mock(FirestoreFirstCategoryRepository::class.java)
        
        whenever(userPreferences.getCurrentUserId()).thenReturn(testLegacyUserId)
        whenever(categoryRepository.getRoomCategoryId(testCategoryId)).thenReturn(testRoomCategoryId)
        whenever(categoryRepository.getCloudCategoryId(testRoomCategoryId)).thenReturn(testCategoryId)
        
        repository = FirestoreFirstExpenseRepository(
            context = context,
            remoteDataSource = remoteDataSource,
            expenseDao = expenseDao,
            userPreferences = userPreferences,
            categoryRepository = categoryRepository,
            dispatcher = testDispatcher
        )
    }

    @Test
    fun `observeExpenses returns Firestore data and syncs to Room`() = runTest {
        val cloudExpenses = listOf(
            ExpenseRecord("exp1", testUid, testCategoryId, "2025-01-15", 5.0, "Coffee", null, null, null, 1000L, 1000L),
            ExpenseRecord("exp2", testUid, testCategoryId, "2025-01-15", 12.0, "Lunch", null, null, null, 1000L, 1000L)
        )
        
        whenever(remoteDataSource.observeExpenses(testUid))
            .thenReturn(flowOf(cloudExpenses))
        whenever(expenseDao.getExpensesByUser(testLegacyUserId))
            .thenReturn(emptyList())

        val result = repository.observeExpenses(testUid).first()

        assertEquals(2, result.size)
        assertEquals("Coffee", result[0].description)
        assertEquals("Lunch", result[1].description)
        verify(expenseDao, atLeastOnce()).insertExpense(any())
    }

    @Test
    fun `observeExpenses filters by date range`() = runTest {
        val cloudExpenses = listOf(
            ExpenseRecord("exp1", testUid, testCategoryId, "2025-01-15", 5.0, "Coffee", null, null, null, 1000L, 1000L),
            ExpenseRecord("exp2", testUid, testCategoryId, "2025-01-20", 12.0, "Lunch", null, null, null, 1000L, 1000L),
            ExpenseRecord("exp3", testUid, testCategoryId, "2025-01-25", 25.0, "Dinner", null, null, null, 1000L, 1000L)
        )
        
        whenever(remoteDataSource.observeExpenses(testUid))
            .thenReturn(flowOf(cloudExpenses))
        whenever(expenseDao.getExpensesByUser(testLegacyUserId))
            .thenReturn(emptyList())

        val result = repository.observeExpenses(testUid, "2025-01-15", "2025-01-20").first()

        assertEquals(2, result.size)
        assertEquals("Coffee", result[0].description)
        assertEquals("Lunch", result[1].description)
    }

    @Test
    fun `observeExpenses falls back to cache on Firestore error`() = runTest {
        val cachedRoomExpenses = listOf(
            Expense(1, testLegacyUserId, testRoomCategoryId, "2025-01-15", 5.0, "Coffee", null, null, null)
        )
        
        whenever(remoteDataSource.observeExpenses(testUid))
            .thenThrow(RuntimeException("Network error"))
        whenever(expenseDao.getExpensesByUser(testLegacyUserId))
            .thenReturn(cachedRoomExpenses)

        val result = repository.observeExpenses(testUid).first()

        assertEquals(1, result.size)
        assertEquals("Coffee", result[0].description)
    }

    @Test
    fun `createExpense successfully creates in Firestore without photo`() = runTest {
        val expense = ExpenseRecord("", testUid, testCategoryId, "2025-01-15", 5.0, "Coffee", null, null, null, 0L, 0L)
        val cloudId = "exp-new-123"
        
        whenever(remoteDataSource.upsertExpense(eq(testUid), any()))
            .thenReturn(FirestoreResult.Success(cloudId))
        whenever(expenseDao.getExpensesByUser(testLegacyUserId))
            .thenReturn(emptyList())

        val result = repository.createExpense(testUid, expense, null)

        assertTrue(result.isSuccess)
        assertEquals(cloudId, result.getOrNull())
        verify(remoteDataSource).upsertExpense(eq(testUid), any())
    }

    @Test
    fun `updateExpense updates Firestore and Room cache`() = runTest {
        val expense = ExpenseRecord("exp1", testUid, testCategoryId, "2025-01-15", 6.0, "Coffee Updated", null, null, null, 1000L, 1000L)
        
        whenever(remoteDataSource.upsertExpense(eq(testUid), any()))
            .thenReturn(FirestoreResult.Success("exp1"))
        whenever(expenseDao.getExpensesByUser(testLegacyUserId))
            .thenReturn(emptyList())

        val result = repository.updateExpense(testUid, expense, null)

        assertTrue(result.isSuccess)
        verify(remoteDataSource).upsertExpense(eq(testUid), any())
    }

    @Test
    fun `deleteExpense removes from Firestore and Room cache`() = runTest {
        val expenseId = "exp1"
        
        whenever(remoteDataSource.deleteExpense(testUid, expenseId))
            .thenReturn(FirestoreResult.Success(Unit))
        whenever(expenseDao.getExpensesByUser(testLegacyUserId))
            .thenReturn(emptyList())

        val result = repository.deleteExpense(testUid, expenseId)

        assertTrue(result.isSuccess)
        verify(remoteDataSource).deleteExpense(testUid, expenseId)
    }

    @Test
    fun `getTotalSpent returns correct sum for date range`() = runTest {
        val cloudExpenses = listOf(
            ExpenseRecord("exp1", testUid, testCategoryId, "2025-01-15", 5.0, "Coffee", null, null, null, 1000L, 1000L),
            ExpenseRecord("exp2", testUid, testCategoryId, "2025-01-16", 12.0, "Lunch", null, null, null, 1000L, 1000L),
            ExpenseRecord("exp3", testUid, testCategoryId, "2025-01-17", 25.0, "Dinner", null, null, null, 1000L, 1000L)
        )
        
        whenever(remoteDataSource.observeExpenses(testUid))
            .thenReturn(flowOf(cloudExpenses))
        whenever(expenseDao.getExpensesByUser(testLegacyUserId))
            .thenReturn(emptyList())

        val result = repository.getTotalSpent(testUid, "2025-01-15", "2025-01-16").first()

        assertEquals(17.0, result, 0.01)
    }

    @Test
    fun `getCategorySummaries returns correct aggregated data`() = runTest {
        val cat2 = "cat2"
        val roomCat2 = 6
        whenever(categoryRepository.getRoomCategoryId(cat2)).thenReturn(roomCat2)
        
        val cloudExpenses = listOf(
            ExpenseRecord("exp1", testUid, testCategoryId, "2025-01-15", 5.0, "Coffee", null, null, null, 1000L, 1000L),
            ExpenseRecord("exp2", testUid, testCategoryId, "2025-01-16", 15.0, "Lunch", null, null, null, 1000L, 1000L),
            ExpenseRecord("exp3", testUid, cat2, "2025-01-17", 30.0, "Gas", null, null, null, 1000L, 1000L)
        )
        
        whenever(remoteDataSource.observeExpenses(testUid))
            .thenReturn(flowOf(cloudExpenses))
        whenever(expenseDao.getExpensesByUser(testLegacyUserId))
            .thenReturn(emptyList())

        val result = repository.getCategorySummaries(testUid, "2025-01-01", "2025-01-31").first()

        assertEquals(2, result.size)
        assertEquals(cat2, result[0].categoryId)
        assertEquals(30.0, result[0].total, 0.01)
        assertEquals(60.0f, result[0].percentage, 0.1f)
        assertEquals(1, result[0].expenseCount)
        
        assertEquals(testCategoryId, result[1].categoryId)
        assertEquals(20.0, result[1].total, 0.01)
        assertEquals(40.0f, result[1].percentage, 0.1f)
        assertEquals(2, result[1].expenseCount)
    }

    @Test
    fun `clearCache clears the ID mapping`() = runTest {
        repository.clearCache()
    }
}
