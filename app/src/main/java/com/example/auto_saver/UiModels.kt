package com.example.auto_saver

import java.time.LocalDate
import java.time.temporal.ChronoUnit

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

    fun durationInDays(): Long {
        val startDate = LocalDate.parse(start)
        val endDate = LocalDate.parse(end)
        return ChronoUnit.DAYS.between(startDate, endDate) + 1
    }

    fun previousPeriod(): DateRange {
        val startDate = LocalDate.parse(start)
        val periodLength = durationInDays()
        val previousEnd = startDate.minusDays(1)
        val previousStart = previousEnd.minusDays(periodLength - 1)
        return DateRange(previousStart.toString(), previousEnd.toString())
    }

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

        fun getLastNDays(days: Int): DateRange {
            val now = java.time.LocalDate.now()
            val clamped = days.coerceAtLeast(1)
            return DateRange(
                start = now.minusDays(clamped.toLong() - 1).toString(),
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
