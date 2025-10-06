package com.example.auto_saver

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExpenseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
        categoryDao = db.categoryDao()
        expenseDao = db.expenseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndQueryExpensesByUser_andTotalByCategory() = runBlocking {
        // Arrange: create a user, category, and some expenses
        val userId = userDao.insert(User(loginId = "user1", password = "pass", fullName = "Test User")).toInt()
        val category = Category(userId = userId, name = "Food")
        categoryDao.insertCategory(category)
        val categories = categoryDao.getCategoriesByUser(userId)
        val categoryId = categories.first().id

        val e1 = Expense(userId = userId, categoryId = categoryId, date = "2025-10-01", amount = 12.50, description = "Lunch")
        val e2 = Expense(userId = userId, categoryId = categoryId, date = "2025-10-02", amount = 7.25, description = "Snack")
        expenseDao.insertExpense(e1)
        expenseDao.insertExpense(e2)

        // Act
        val expenses = expenseDao.getExpensesByUser(userId)
        val totalFood = expenseDao.getTotalSpentByCategory(userId, categoryId) ?: 0.0

        // Assert
        assertThat(expenses).hasSize(2)
        assertThat(expenses.map { it.description }).containsAtLeast("Lunch", "Snack")
        assertThat(totalFood).isWithin(1e-6).of(19.75)
    }
}
