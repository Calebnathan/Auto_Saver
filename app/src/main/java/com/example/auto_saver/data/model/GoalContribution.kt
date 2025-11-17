package com.example.auto_saver.data.model

data class GoalContribution(
    val id: String = "",
    val goalId: String = "",
    val categoryId: String = "",
    val uid: String = "",
    val amount: Double = 0.0,
    val date: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)