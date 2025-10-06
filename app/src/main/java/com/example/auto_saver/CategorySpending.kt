package com.example.auto_saver

data class CategorySpending(
    val categoryId: Int,
    val categoryName: String,
    val totalAmount: Double,
    val percentage: Float,
    val expenseCount: Int
)

