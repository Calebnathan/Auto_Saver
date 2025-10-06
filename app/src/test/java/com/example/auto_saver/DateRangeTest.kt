package com.example.auto_saver

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class DateRangeTest {

    @Test
    fun `getCurrentMonth returns correct date range for current month`() {
        val dateRange = DateRange.getCurrentMonth()
        val now = LocalDate.now()
        val firstDay = now.withDayOfMonth(1)

        assertThat(dateRange.start).isEqualTo(firstDay.toString())
        assertThat(dateRange.end).isEqualTo(now.toString())
    }

    @Test
    fun `getCurrentWeek returns correct date range for current week`() {
        val dateRange = DateRange.getCurrentWeek()
        val now = LocalDate.now()
        val weekStart = now.minusDays(now.dayOfWeek.value.toLong() - 1)

        assertThat(dateRange.start).isEqualTo(weekStart.toString())
        assertThat(dateRange.end).isEqualTo(now.toString())
    }

    @Test
    fun `getLast30Days returns correct date range`() {
        val dateRange = DateRange.getLast30Days()
        val now = LocalDate.now()
        val thirtyDaysAgo = now.minusDays(30)

        assertThat(dateRange.start).isEqualTo(thirtyDaysAgo.toString())
        assertThat(dateRange.end).isEqualTo(now.toString())
    }

    @Test
    fun `DateRange can be created with custom dates`() {
        val startDate = "2025-01-01"
        val endDate = "2025-01-31"
        val dateRange = DateRange(startDate, endDate)

        assertThat(dateRange.start).isEqualTo(startDate)
        assertThat(dateRange.end).isEqualTo(endDate)
    }

    @Test
    fun `DateRange equality works correctly`() {
        val range1 = DateRange("2025-01-01", "2025-01-31")
        val range2 = DateRange("2025-01-01", "2025-01-31")
        val range3 = DateRange("2025-02-01", "2025-02-28")

        assertThat(range1).isEqualTo(range2)
        assertThat(range1).isNotEqualTo(range3)
    }
}

