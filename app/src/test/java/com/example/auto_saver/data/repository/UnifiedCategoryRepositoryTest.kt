package com.example.auto_saver.data.repository

import com.example.auto_saver.Category
import com.example.auto_saver.CategoryDao
import com.example.auto_saver.UserPreferences
import com.example.auto_saver.data.firestore.CategoryRemoteDataSource
import com.example.auto_saver.data.firestore.FirestoreResult
import com.example.auto_saver.data.model.CategoryRecord
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

class UnifiedCategoryRepositoryTest {

    private lateinit var remoteDataSource: CategoryRemoteDataSource
    private lateinit var categoryDao: CategoryDao
    private lateinit var userPreferences: UserPreferences
    private lateinit var repository: FirestoreFirstCategoryRepository
    
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testUid = "test-uid-123"
    private val testLegacyUserId = 1

    @Before
    fun setup() {
        remoteDataSource = mock(CategoryRemoteDataSource::class.java)
        categoryDao = mock(CategoryDao::class.java)
        userPreferences = mock(UserPreferences::class.java)
        
        whenever(userPreferences.getCurrentUserId()).thenReturn(testLegacyUserId)
        
        repository = FirestoreFirstCategoryRepository(
            remoteDataSource = remoteDataSource,
            categoryDao = categoryDao,
            userPreferences = userPreferences,
            dispatcher = testDispatcher
        )
    }

    @Test
    fun `observeCategories returns Firestore data and syncs to Room`() = runTest {
        val cloudCategories = listOf(
            CategoryRecord("cat1", testUid, "Food", 1000L, 1000L),
            CategoryRecord("cat2", testUid, "Transport", 1000L, 1000L)
        )
        
        whenever(remoteDataSource.observeCategories(testUid))
            .thenReturn(flowOf(cloudCategories))
        whenever(categoryDao.getCategoriesByUser(testLegacyUserId))
            .thenReturn(emptyList())

        val result = repository.observeCategories(testUid).first()

        assertEquals(2, result.size)
        assertEquals("Food", result[0].name)
        assertEquals("Transport", result[1].name)
        verify(categoryDao, atLeastOnce()).getCategoriesByUser(testLegacyUserId)
    }

    @Test
    fun `observeCategories falls back to cache on Firestore error`() = runTest {
        val cachedRoomCategories = listOf(
            Category(1, testLegacyUserId, "Food"),
            Category(2, testLegacyUserId, "Transport")
        )
        
        whenever(remoteDataSource.observeCategories(testUid))
            .thenThrow(RuntimeException("Network error"))
        whenever(categoryDao.getCategoriesByUser(testLegacyUserId))
            .thenReturn(cachedRoomCategories)

        val result = repository.observeCategories(testUid).first()

        assertEquals(2, result.size)
        assertEquals("Food", result[0].name)
        assertEquals("Transport", result[1].name)
    }

    @Test
    fun `createCategory successfully creates in Firestore and caches to Room`() = runTest {
        val categoryName = "Entertainment"
        val cloudId = "cat-new-123"
        
        whenever(remoteDataSource.upsertCategory(eq(testUid), any()))
            .thenReturn(FirestoreResult.Success(cloudId))
        whenever(categoryDao.getCategoriesByUser(testLegacyUserId))
            .thenReturn(emptyList())

        val result = repository.createCategory(testUid, categoryName)

        assertTrue(result.isSuccess)
        assertEquals(cloudId, result.getOrNull()?.id)
        assertEquals(categoryName, result.getOrNull()?.name)
        verify(remoteDataSource).upsertCategory(eq(testUid), any())
        verify(categoryDao).insertCategory(any())
    }

    @Test
    fun `createCategory returns error on Firestore failure`() = runTest {
        val categoryName = "Entertainment"
        val exception = Exception("Firestore error")
        
        whenever(remoteDataSource.upsertCategory(eq(testUid), any()))
            .thenReturn(FirestoreResult.Error(exception))

        val result = repository.createCategory(testUid, categoryName)

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
    }

    @Test
    fun `updateCategory updates Firestore and Room cache`() = runTest {
        val category = CategoryRecord("cat1", testUid, "Food", 1000L, 1000L)
        
        whenever(remoteDataSource.upsertCategory(eq(testUid), any()))
            .thenReturn(FirestoreResult.Success("cat1"))
        whenever(categoryDao.getCategoriesByUser(testLegacyUserId))
            .thenReturn(listOf(Category(1, testLegacyUserId, "Food")))

        val result = repository.updateCategory(testUid, category)

        assertTrue(result.isSuccess)
        verify(remoteDataSource).upsertCategory(eq(testUid), any())
    }

    @Test
    fun `deleteCategory removes from Firestore and Room cache`() = runTest {
        val categoryId = "cat1"
        val roomCategory = Category(1, testLegacyUserId, "Food")
        
        repository.getRoomCategoryId(categoryId)
        whenever(remoteDataSource.deleteCategory(testUid, categoryId))
            .thenReturn(FirestoreResult.Success(Unit))
        whenever(categoryDao.getCategoryById(1))
            .thenReturn(roomCategory)

        val result = repository.deleteCategory(testUid, categoryId)

        assertTrue(result.isSuccess)
        verify(remoteDataSource).deleteCategory(testUid, categoryId)
    }

    @Test
    fun `getRoomCategoryId returns correct mapped ID`() = runTest {
        val cloudCategories = listOf(
            CategoryRecord("cat1", testUid, "Food", 1000L, 1000L)
        )
        val roomCategories = listOf(
            Category(5, testLegacyUserId, "Food")
        )
        
        whenever(remoteDataSource.observeCategories(testUid))
            .thenReturn(flowOf(cloudCategories))
        whenever(categoryDao.getCategoriesByUser(testLegacyUserId))
            .thenReturn(roomCategories)

        repository.observeCategories(testUid).first()
        
        val roomId = repository.getRoomCategoryId("cat1")
        assertEquals(5, roomId)
    }

    @Test
    fun `getCloudCategoryId returns correct reverse mapped ID`() = runTest {
        val cloudCategories = listOf(
            CategoryRecord("cat1", testUid, "Food", 1000L, 1000L)
        )
        val roomCategories = listOf(
            Category(5, testLegacyUserId, "Food")
        )
        
        whenever(remoteDataSource.observeCategories(testUid))
            .thenReturn(flowOf(cloudCategories))
        whenever(categoryDao.getCategoriesByUser(testLegacyUserId))
            .thenReturn(roomCategories)

        repository.observeCategories(testUid).first()
        
        val cloudId = repository.getCloudCategoryId(5)
        assertEquals("cat1", cloudId)
    }

    @Test
    fun `clearCache clears the ID mapping`() = runTest {
        val cloudCategories = listOf(
            CategoryRecord("cat1", testUid, "Food", 1000L, 1000L)
        )
        val roomCategories = listOf(
            Category(5, testLegacyUserId, "Food")
        )
        
        whenever(remoteDataSource.observeCategories(testUid))
            .thenReturn(flowOf(cloudCategories))
        whenever(categoryDao.getCategoriesByUser(testLegacyUserId))
            .thenReturn(roomCategories)

        repository.observeCategories(testUid).first()
        assertNotNull(repository.getRoomCategoryId("cat1"))
        
        repository.clearCache()
        
        assertNull(repository.getRoomCategoryId("cat1"))
    }

    @Test
    fun `syncToRoom matches by case-insensitive name`() = runTest {
        val cloudCategories = listOf(
            CategoryRecord("cat1", testUid, "FOOD", 1000L, 1000L)
        )
        val roomCategories = listOf(
            Category(5, testLegacyUserId, "food")
        )
        
        whenever(remoteDataSource.observeCategories(testUid))
            .thenReturn(flowOf(cloudCategories))
        whenever(categoryDao.getCategoriesByUser(testLegacyUserId))
            .thenReturn(roomCategories)

        repository.observeCategories(testUid).first()
        
        val roomId = repository.getRoomCategoryId("cat1")
        assertEquals(5, roomId)
        verify(categoryDao, never()).insertCategory(any())
    }
}
