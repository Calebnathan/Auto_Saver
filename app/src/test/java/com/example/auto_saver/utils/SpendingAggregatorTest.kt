package com.example.auto_saver.utils

import com.example.auto_saver.DateRange
import com.example.auto_saver.data.model.ExpenseRecord
import org.junit.Assert.assertEquals
import org.junit.Test

class SpendingAggregatorTest {

    @Test
    fun aggregateByDay_fillsMissingDays() {
        val expenses = listOf(
            expense(date = "2025-03-01", amount = 10.0),
            expense(date = "2025-03-03", amount = 5.0)
        )
        val range = DateRange(start = "2025-03-01", end = "2025-03-03")

        val result = SpendingAggregator.aggregateByDay(expenses, range)

        assertEquals(3, result.size)
        assertEquals(10.0, result["2025-03-01"] ?: 0.0, 0.001)
        assertEquals(0.0, result["2025-03-02"] ?: 0.0, 0.001)
        assertEquals(5.0, result["2025-03-03"] ?: 0.0, 0.001)
    }

    @Test
    fun aggregateByWeek_groupsToWeekStart() {
        val expenses = listOf(
            expense(date = "2025-03-03", amount = 10.0), // Monday
            expense(date = "2025-03-04", amount = 5.0),
            expense(date = "2025-03-11", amount = 7.0)
        )
        val range = DateRange(start = "2025-03-01", end = "2025-03-15")

        val result = SpendingAggregator.aggregateByWeek(expenses, range)

        val nonZero = result.filterValues { it > 0.0 }
        assertEquals(2, nonZero.size)
        assertEquals(15.0, nonZero["2025-03-03"] ?: 0.0, 0.001)
        assertEquals(7.0, nonZero["2025-03-10"] ?: 0.0, 0.001)
    }

    @Test
    fun aggregateByMonth_coversEachMonth() {
        val expenses = listOf(
            expense(date = "2025-01-15", amount = 30.0),
            expense(date = "2025-02-10", amount = 20.0)
        )
        val range = DateRange(start = "2025-01-01", end = "2025-03-01")

        val result = SpendingAggregator.aggregateByMonth(expenses, range)

        assertEquals(3, result.size)
        assertEquals(30.0, result["2025-01"] ?: 0.0, 0.001)
        assertEquals(20.0, result["2025-02"] ?: 0.0, 0.001)
        assertEquals(0.0, result["2025-03"] ?: 0.0, 0.001)
    }

    private fun expense(date: String, amount: Double): ExpenseRecord {
        return ExpenseRecord(
            id = date,
            uid = "uid",
            categoryId = "cat",
            date = date,
            amount = amount,
            description = null,
            startTime = null,
            endTime = null,
            photoPath = null
        )
    }
}
