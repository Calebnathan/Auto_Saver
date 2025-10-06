package com.example.auto_saver

/**
 * UI-specific data models for the presentation layer
 */

data class CategorySummary(
    val categoryId: Int,
    val categoryName: String,
    val total: Double,
    val percentage: Float,
    val expenseCount: Int
)

data class DateRange(
    val start: String,
    val end: String
) {
    companion object {
        fun getCurrentMonth(): DateRange {
            val now = java.time.LocalDate.now()
            val firstDay = now.withDayOfMonth(1)
            return DateRange(
                start = firstDay.toString(),
                end = now.toString()
            )
        }

        fun getCurrentWeek(): DateRange {
            val now = java.time.LocalDate.now()
            val weekStart = now.minusDays(now.dayOfWeek.value.toLong() - 1)
            return DateRange(
                start = weekStart.toString(),
                end = now.toString()
            )
        }

        fun getLast30Days(): DateRange {
            val now = java.time.LocalDate.now()
            return DateRange(
                start = now.minusDays(30).toString(),
                end = now.toString()
            )
        }
    }
}

data class SpendingSummary(
    val totalSpent: Double,
    val goalMin: Double?,
    val goalMax: Double?,
    val percentageOfGoal: Float,
    val isOverBudget: Boolean
)

data class ExpenseWithCategory(
    val expense: Expense,
    val categoryName: String
)

