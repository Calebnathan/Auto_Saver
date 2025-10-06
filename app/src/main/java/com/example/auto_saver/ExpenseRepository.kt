package com.example.auto_saver

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Repository layer that abstracts data sources and provides clean API for ViewModels
 */
class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val goalDao: GoalDao
) {

    /**
     * Get expenses for a user within a date range as a Flow
     */
    fun getExpensesByDateRange(userId: Int, startDate: String, endDate: String): Flow<List<Expense>> {
        return expenseDao.getExpensesByDateRangeFlow(userId, startDate, endDate)
    }

    /**
     * Get all expenses for a user
     */
    fun getAllExpenses(userId: Int): Flow<List<Expense>> {
        return expenseDao.getExpensesByUserFlow(userId)
    }

    /**
     * Get category summaries with totals and percentages
     */
    fun getCategorySummaries(userId: Int, startDate: String, endDate: String): Flow<List<CategorySummary>> {
        return combine(
            expenseDao.getExpensesByDateRangeFlow(userId, startDate, endDate),
            categoryDao.getCategoriesByUserFlow(userId)
        ) { expenses, categories ->
            val categoryMap = categories.associateBy { it.id }
            val totalSpent = expenses.sumOf { it.amount }

            expenses.groupBy { it.categoryId }
                .mapNotNull { (categoryId, expenseList) ->
                    val category = categoryMap[categoryId] ?: return@mapNotNull null
                    val categoryTotal = expenseList.sumOf { it.amount }
                    val percentage = if (totalSpent > 0) {
                        ((categoryTotal / totalSpent) * 100).toFloat()
                    } else 0f

                    CategorySummary(
                        categoryId = categoryId,
                        categoryName = category.name,
                        total = categoryTotal,
                        percentage = percentage,
                        expenseCount = expenseList.size
                    )
                }
                .sortedByDescending { it.total }
        }
    }

    /**
     * Get total spent in a date range
     */
    fun getTotalSpent(userId: Int, startDate: String, endDate: String): Flow<Double> {
        return expenseDao.getTotalSpentInRange(userId, startDate, endDate)
            .map { it ?: 0.0 }
    }

    /**
     * Insert a new expense
     */
    suspend fun insertExpense(expense: Expense) {
        expenseDao.insertExpense(expense)
    }

    /**
     * Update an existing expense
     */
    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    /**
     * Delete an expense
     */
    suspend fun deleteExpense(expense: Expense) {
        expenseDao.deleteExpense(expense)
    }

    /**
     * Get all categories for a user
     */
    fun getCategories(userId: Int): Flow<List<Category>> {
        return categoryDao.getCategoriesByUserFlow(userId)
    }

    /**
     * Get spending goal for current month
     */
    suspend fun getCurrentMonthGoal(userId: Int): Goal? {
        val currentMonth = java.time.YearMonth.now().toString()
        return goalDao.getGoalByMonth(userId, currentMonth)
    }

    /**
     * Check if user has any expenses
     */
    fun hasExpenses(userId: Int): Flow<Boolean> {
        return expenseDao.getExpenseCount(userId).map { it > 0 }
    }
}

