package com.example.auto_saver.data.model

data class GoalCategory(
    val id: String = "",
    val name: String = "",
    val budget: Double = 0.0,
    val saved: Double = 0.0,
    val color: String = "#4CAF50"
) {
    val progress: Float
        get() = if (budget > 0) ((saved / budget) * 100).toFloat() else 0f
    
    val remaining: Double
        get() = (budget - saved).coerceAtLeast(0.0)
}