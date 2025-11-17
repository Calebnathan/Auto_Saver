package com.example.auto_saver.utils

import com.example.auto_saver.DateRange
import com.example.auto_saver.data.model.ExpenseRecord
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters

/**
 * Utility helpers for aggregating expenses into the time buckets used by the dashboard
 * and full-screen graph surfaces.
 */
object SpendingAggregator {

    data class CategoryTotal(
        val categoryId: String,
        val total: Double,
        val expenseCount: Int
    )

    data class PaginatedResult<T>(
        val data: T,
        val totalCount: Int,
        val hasMore: Boolean
    )

    /**
     * Aggregate by day with pagination support for large datasets
     */
    fun aggregateByDayPaginated(
        expenses: List<ExpenseRecord>,
        dateRange: DateRange,
        limit: Int = 100,
        offset: Int = 0
    ): PaginatedResult<Map<String, Double>> {
        val result = aggregateByDay(expenses, dateRange)
        val entries = result.entries.toList()
        val paginatedEntries = entries.drop(offset).take(limit)
        
        return PaginatedResult(
            data = paginatedEntries.associate { it.key to it.value },
            totalCount = entries.size,
            hasMore = offset + limit < entries.size
        )
    }

    /**
     * Get top spending categories with limit
     */
    fun getTopCategories(
        expenses: List<ExpenseRecord>,
        dateRange: DateRange,
        limit: Int = 5
    ): List<CategoryTotal> {
        return aggregateByCategory(expenses, dateRange)
            .values
            .sortedByDescending { it.total }
            .take(limit)
    }

    /**
     * Calculate statistics for a date range
     */
    fun calculateStatistics(
        expenses: List<ExpenseRecord>,
        dateRange: DateRange
    ): SpendingStatistics {
        val filtered = expenses.filter { expense ->
            val date = expense.safeDate() ?: return@filter false
            val start = LocalDate.parse(dateRange.start)
            val end = LocalDate.parse(dateRange.end)
            !date.isBefore(start) && !date.isAfter(end)
        }

        val total = filtered.sumOf { it.amount }
        val dailyTotals = aggregateByDay(filtered, dateRange)
        val nonZeroDays = dailyTotals.values.count { it > 0.0 }
        
        return SpendingStatistics(
            totalSpent = total,
            expenseCount = filtered.size,
            averagePerExpense = if (filtered.isNotEmpty()) total / filtered.size else 0.0,
            averagePerDay = if (nonZeroDays > 0) total / nonZeroDays else 0.0,
            maxDailySpending = dailyTotals.values.maxOrNull() ?: 0.0,
            minDailySpending = dailyTotals.values.filter { it > 0.0 }.minOrNull() ?: 0.0,
            daysWithExpenses = nonZeroDays
        )
    }

    data class SpendingStatistics(
        val totalSpent: Double,
        val expenseCount: Int,
        val averagePerExpense: Double,
        val averagePerDay: Double,
        val maxDailySpending: Double,
        val minDailySpending: Double,
        val daysWithExpenses: Int
    )

    fun aggregateByDay(
        expenses: List<ExpenseRecord>,
        dateRange: DateRange
    ): Map<String, Double> {
        val totals = expenses.groupByDate()
        val result = linkedMapOf<String, Double>()
        var cursor = LocalDate.parse(dateRange.start)
        val endDate = LocalDate.parse(dateRange.end)

        while (!cursor.isAfter(endDate)) {
            val key = cursor.toString()
            result[key] = totals[key] ?: 0.0
            cursor = cursor.plusDays(1)
        }
        return result
    }

    fun aggregateByWeek(
        expenses: List<ExpenseRecord>,
        dateRange: DateRange
    ): Map<String, Double> {
        val totals = sumByKey(expenses) { date ->
            date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toString()
        }

        val start = LocalDate.parse(dateRange.start)
        val end = LocalDate.parse(dateRange.end)
        var weekCursor = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastWeek = end.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

        val result = linkedMapOf<String, Double>()
        while (!weekCursor.isAfter(lastWeek)) {
            val key = weekCursor.toString()
            result[key] = totals[key] ?: 0.0
            weekCursor = weekCursor.plusWeeks(1)
        }
        return result
    }

    fun aggregateByMonth(
        expenses: List<ExpenseRecord>,
        dateRange: DateRange
    ): Map<String, Double> {
        val totals = sumByKey(expenses) { date ->
            YearMonth.from(date).toString()
        }

        val start = YearMonth.from(LocalDate.parse(dateRange.start))
        val end = YearMonth.from(LocalDate.parse(dateRange.end))

        val result = linkedMapOf<String, Double>()
        var cursor = start
        while (!cursor.isAfter(end)) {
            val key = cursor.toString()
            result[key] = totals[key] ?: 0.0
            cursor = cursor.plusMonths(1)
        }
        return result
    }

    fun aggregateByYear(
        expenses: List<ExpenseRecord>,
        dateRange: DateRange
    ): Map<String, Double> {
        val totals = sumByKey(expenses) { date -> date.year.toString() }

        val startYear = LocalDate.parse(dateRange.start).year
        val endYear = LocalDate.parse(dateRange.end).year

        val result = linkedMapOf<String, Double>()
        for (year in startYear..endYear) {
            val key = year.toString()
            result[key] = totals[key] ?: 0.0
        }
        return result
    }

    fun aggregateByCategory(
        expenses: List<ExpenseRecord>,
        dateRange: DateRange
    ): Map<String, CategoryTotal> {
        val start = LocalDate.parse(dateRange.start)
        val end = LocalDate.parse(dateRange.end)
        val filtered = expenses.filter { expense ->
            val date = expense.safeDate() ?: return@filter false
            !date.isBefore(start) && !date.isAfter(end)
        }

        return filtered.groupBy { it.categoryId }
            .mapValues { (_, items) ->
                CategoryTotal(
                    categoryId = items.first().categoryId,
                    total = items.sumOf { it.amount },
                    expenseCount = items.size
                )
            }
    }

    private fun List<ExpenseRecord>.groupByDate(): Map<String, Double> {
        return this.groupBy { it.date }
            .mapValues { (_, items) -> items.sumOf { it.amount } }
    }

    private fun ExpenseRecord.safeDate(): LocalDate? {
        return try {
            LocalDate.parse(date)
        } catch (e: DateTimeParseException) {
            null
        }
    }

    private fun sumByKey(
        expenses: List<ExpenseRecord>,
        transform: (LocalDate) -> String
    ): Map<String, Double> {
        val result = mutableMapOf<String, Double>()
        expenses.forEach { expense ->
            val date = expense.safeDate() ?: return@forEach
            val key = transform(date)
            result[key] = (result[key] ?: 0.0) + expense.amount
        }
        return result
    }
}