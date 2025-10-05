package com.example.auto_saver

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goal_table")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "user_id")
    val userId: Int,

    @ColumnInfo(name = "month")
    val month: String,

    @ColumnInfo(name = "min_goal")
    val minGoal: Double,

    @ColumnInfo(name = "max_goal")
    val maxGoal: Double
)
