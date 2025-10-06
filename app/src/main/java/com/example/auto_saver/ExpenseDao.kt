package com.example.auto_saver

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT * FROM expense_table WHERE user_id = :userId ORDER BY date DESC")
    suspend fun getExpensesByUser(userId: Int): List<Expense>

    // Flow-based query for reactive UI
    @Query("SELECT * FROM expense_table WHERE user_id = :userId ORDER BY date DESC")
    fun getExpensesByUserFlow(userId: Int): Flow<List<Expense>>

    // Date range filtering with Flow
    @Query("SELECT * FROM expense_table WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getExpensesByDateRangeFlow(userId: Int, startDate: String, endDate: String): Flow<List<Expense>>

    @Query("SELECT SUM(amount) FROM expense_table WHERE category_id = :categoryId AND user_id = :userId")
    suspend fun getTotalSpentByCategory(userId: Int, categoryId: Int): Double?

    // Get total spent in date range
    @Query("SELECT SUM(amount) FROM expense_table WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate")
    fun getTotalSpentInRange(userId: Int, startDate: String, endDate: String): Flow<Double?>

    // Get expenses count for empty state
    @Query("SELECT COUNT(*) FROM expense_table WHERE user_id = :userId")
    fun getExpenseCount(userId: Int): Flow<Int>
}