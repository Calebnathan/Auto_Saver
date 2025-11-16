package com.example.auto_saver

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expense_table")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "user_id")
    val userId: Int,

    @ColumnInfo(name = "category_id")
    val categoryId: Int,

    val date: String,
    val amount: Double,
    val description: String? = null,

    @ColumnInfo(name = "start_time")
    val startTime: String? = null,

    @ColumnInfo(name = "end_time")
    val endTime: String? = null,

    @ColumnInfo(name = "photo_path")
    val photoPath: String? = null
)
