package com.example.auto_saver

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ExpenseRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var categoryDao: CategoryDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var goalDao: GoalDao
    private lateinit var repository: ExpenseRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        userDao = db.userDao()
        categoryDao = db.categoryDao()
        expenseDao = db.expenseDao()
        goalDao = db.goalDao()
        repository = ExpenseRepository(expenseDao, categoryDao, goalDao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun categorySummaries_aggregatesTotalsAndPercentages() = runTest {
        val userId = userDao.insert(User(loginId = "user1", password = "pass")).toInt()
        val catFood = Category(userId = userId, name = "Food")
        val catFuel = Category(userId = userId, name = "Fuel")
        categoryDao.insertCategory(catFood)
        categoryDao.insertCategory(catFuel)
        val cats = categoryDao.getCategoriesByUser(userId)
        val idFood = cats.first { it.name == "Food" }.id
        val idFuel = cats.first { it.name == "Fuel" }.id

        expenseDao.insertExpense(Expense(userId = userId, categoryId = idFood, date = "2025-10-01", amount = 30.0))
        expenseDao.insertExpense(Expense(userId = userId, categoryId = idFood, date = "2025-10-02", amount = 20.0))
        expenseDao.insertExpense(Expense(userId = userId, categoryId = idFuel, date = "2025-10-02", amount = 50.0))

        val start = "2025-10-01"
        val end = "2025-10-31"
        val summaries = repository.getCategorySummaries(userId, start, end).first()

        assertThat(summaries).hasSize(2)
        val food = summaries.first { it.categoryName == "Food" }
        val fuel = summaries.first { it.categoryName == "Fuel" }

        assertThat(food.total).isWithin(1e-6).of(50.0)
        assertThat(fuel.total).isWithin(1e-6).of(50.0)
        // Both categories are 50% of total (100)
        assertThat(food.percentage).isWithin(1e-3f).of(50f)
        assertThat(fuel.percentage).isWithin(1e-3f).of(50f)
    }

    @Test
    fun totalSpent_returnsZeroWhenNoExpenses() = runTest {
        val userId = userDao.insert(User(loginId = "user2", password = "pass")).toInt()
        val total = repository.getTotalSpent(userId, "2025-10-01", "2025-10-31").first()
        assertThat(total).isWithin(1e-6).of(0.0)
    }
}
