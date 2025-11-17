package com.example.auto_saver.utils

import com.example.auto_saver.DateRange
import com.example.auto_saver.data.model.ExpenseRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
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

    @Test
    fun aggregateByDayPaginated_returnsCorrectPageSize() {
        val expenses = (1..150).map { day ->
            expense(date = "2025-01-${day.toString().padStart(2, '0')}", amount = 10.0)
        }
        val range = DateRange(start = "2025-01-01", end = "2025-05-30")

        val result = SpendingAggregator.aggregateByDayPaginated(expenses, range, limit = 50, offset = 0)

        assertEquals(50, result.data.size)
        assertTrue(result.hasMore)
        assertTrue(result.totalCount > 50)
    }

    @Test
    fun aggregateByDayPaginated_lastPageHasNoMore() {
        val expenses = listOf(
            expense(date = "2025-01-01", amount = 10.0),
            expense(date = "2025-01-02", amount = 20.0)
        )
        val range = DateRange(start = "2025-01-01", end = "2025-01-03")

        val result = SpendingAggregator.aggregateByDayPaginated(expenses, range, limit = 10, offset = 0)

        assertEquals(3, result.data.size) // 3 days in range
        assertFalse(result.hasMore)
        assertEquals(3, result.totalCount)
    }

    @Test
    fun getTopCategories_returnsTopN() {
        val expenses = listOf(
            expense(date = "2025-01-01", amount = 100.0, categoryId = "food"),
            expense(date = "2025-01-02", amount = 50.0, categoryId = "transport"),
            expense(date = "2025-01-03", amount = 75.0, categoryId = "shopping"),
            expense(date = "2025-01-04", amount = 25.0, categoryId = "utilities"),
            expense(date = "2025-01-05", amount = 150.0, categoryId = "food")
        )
        val range = DateRange(start = "2025-01-01", end = "2025-01-10")

        val result = SpendingAggregator.getTopCategories(expenses, range, limit = 3)

        assertEquals(3, result.size)
        assertEquals("food", result[0].categoryId)
        assertEquals(250.0, result[0].total, 0.001)
        assertEquals("shopping", result[1].categoryId)
        assertEquals("transport", result[2].categoryId)
    }

    @Test
    fun calculateStatistics_correctAverages() {
        val expenses = listOf(
            expense(date = "2025-01-01", amount = 100.0),
            expense(date = "2025-01-01", amount = 50.0),
            expense(date = "2025-01-03", amount = 75.0)
        )
        val range = DateRange(start = "2025-01-01", end = "2025-01-05")

        val stats = SpendingAggregator.calculateStatistics(expenses, range)

        assertEquals(225.0, stats.totalSpent, 0.001)
        assertEquals(3, stats.expenseCount)
        assertEquals(75.0, stats.averagePerExpense, 0.001) // 225/3
        assertEquals(112.5, stats.averagePerDay, 0.001) // 225/2 days with expenses
        assertEquals(150.0, stats.maxDailySpending, 0.001)
        assertEquals(75.0, stats.minDailySpending, 0.001)
        assertEquals(2, stats.daysWithExpenses)
    }

    @Test
    fun calculateStatistics_handlesEmptyData() {
        val expenses = emptyList<ExpenseRecord>()
        val range = DateRange(start = "2025-01-01", end = "2025-01-10")

        val stats = SpendingAggregator.calculateStatistics(expenses, range)

        assertEquals(0.0, stats.totalSpent, 0.001)
        assertEquals(0, stats.expenseCount)
        assertEquals(0.0, stats.averagePerExpense, 0.001)
        assertEquals(0.0, stats.averagePerDay, 0.001)
        assertEquals(0, stats.daysWithExpenses)
    }

    @Test
    fun aggregateByCategory_groupsByCategory() {
        val expenses = listOf(
            expense(date = "2025-01-01", amount = 50.0, categoryId = "food"),
            expense(date = "2025-01-02", amount = 30.0, categoryId = "food"),
            expense(date = "2025-01-03", amount = 20.0, categoryId = "transport")
        )
        val range = DateRange(start = "2025-01-01", end = "2025-01-10")

        val result = SpendingAggregator.aggregateByCategory(expenses, range)

        assertEquals(2, result.size)
        assertEquals(80.0, result["food"]?.total ?: 0.0, 0.001)
        assertEquals(2, result["food"]?.expenseCount ?: 0)
        assertEquals(20.0, result["transport"]?.total ?: 0.0, 0.001)
        assertEquals(1, result["transport"]?.expenseCount ?: 0)
    }

    @Test
    fun aggregateByYear_handlesMultipleYears() {
        val expenses = listOf(
            expense(date = "2024-12-15", amount = 100.0),
            expense(date = "2025-01-10", amount = 200.0),
            expense(date = "2025-06-15", amount = 150.0)
        )
        val range = DateRange(start = "2024-01-01", end = "2025-12-31")

        val result = SpendingAggregator.aggregateByYear(expenses, range)

        assertEquals(2, result.size)
        assertEquals(100.0, result["2024"] ?: 0.0, 0.001)
        assertEquals(350.0, result["2025"] ?: 0.0, 0.001)
    }

    private fun expense(date: String, amount: Double, categoryId: String = "cat"): ExpenseRecord {
        return ExpenseRecord(
            id = date,
            uid = "uid",
            categoryId = categoryId,
            date = date,
            amount = amount,
            description = null,
            startTime = null,
            endTime = null,
            photoPath = null
        )
    }
}