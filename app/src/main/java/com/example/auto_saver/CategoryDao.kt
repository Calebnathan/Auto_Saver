package com.example.auto_saver

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT * FROM category_table ORDER BY id ASC")
    suspend fun getAllCategories(): List<Category>

    // Flow-based queries - Fixed column name to category_name
    @Query("SELECT * FROM category_table WHERE user_id = :userId ORDER BY category_name ASC")
    fun getCategoriesByUserFlow(userId: Int): Flow<List<Category>>

    @Query("SELECT * FROM category_table WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Int): Category?
}