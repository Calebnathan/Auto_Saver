package com.example.auto_saver.data.model

data class CollaborativeGoal(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val createdBy: String = "",
    val participants: List<String> = emptyList(),
    val totalBudget: Double = 0.0,
    val currentSaved: Double = 0.0,
    val categories: List<GoalCategory> = emptyList(),
    val deadline: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val status: GoalStatus = GoalStatus.ACTIVE
) {
    val progress: Float
        get() = if (totalBudget > 0) ((currentSaved / totalBudget) * 100).toFloat() else 0f
}

enum class GoalStatus {
    ACTIVE,
    COMPLETED,
    CANCELLED
}