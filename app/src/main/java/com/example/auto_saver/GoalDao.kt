package com.example.auto_saver

import androidx.room.*

@Dao
interface GoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("SELECT * FROM goal_table WHERE user_id = :userId AND month = :month LIMIT 1")
    suspend fun getGoalForMonth(userId: Int, month: String): Goal?

    // Alias for consistency
    suspend fun getGoalByMonth(userId: Int, month: String): Goal? = getGoalForMonth(userId, month)
}