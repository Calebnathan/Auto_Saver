package com.example.auto_saver

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

import androidx.room.*

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

    @Query("SELECT SUM(amount) FROM expense_table WHERE category_id = :categoryId AND user_id = :userId")
    suspend fun getTotalSpentByCategory(userId: Int, categoryId: Int): Double?
}