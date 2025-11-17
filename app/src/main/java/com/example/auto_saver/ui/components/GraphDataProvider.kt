package com.example.auto_saver.ui.components

import com.example.auto_saver.DateRange
import com.example.auto_saver.data.model.ExpenseRecord
import com.example.auto_saver.utils.SpendingAggregator
import com.github.mikephil.charting.data.Entry
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

enum class GraphMetric {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY
}

sealed interface GraphUiState {
    object Loading : GraphUiState
    object Empty : GraphUiState
    data class Error(val message: String) : GraphUiState
    data class Success(val data: GraphRenderData) : GraphUiState
}

data class GraphRenderData(
    val metric: GraphMetric,
    val dateRange: DateRange,
    val entries: List<Entry>,
    val comparisonEntries: List<Entry>,
    val labels: List<String>,
    val rangeLabel: String,
    val goalMax: Double?,
    val totalSpent: Double,
    val comparisonTotal: Double
) {
    val isOverBudget: Boolean = goalMax?.let { totalSpent > it } ?: false
}

object GraphDataProvider {

    fun buildRenderData(
        metric: GraphMetric,
        dateRange: DateRange,
        expenses: List<ExpenseRecord>,
        previousExpenses: List<ExpenseRecord>,
        goalMax: Double?
    ): GraphRenderData? {
        val previousRange = dateRange.previousPeriod()

        val currentBuckets = when (metric) {
            GraphMetric.DAILY -> SpendingAggregator.aggregateByDay(expenses, dateRange)
            GraphMetric.WEEKLY -> SpendingAggregator.aggregateByWeek(expenses, dateRange)
            GraphMetric.MONTHLY -> SpendingAggregator.aggregateByMonth(expenses, dateRange)
            GraphMetric.YEARLY -> SpendingAggregator.aggregateByYear(expenses, dateRange)
        }

        if (currentBuckets.values.all { it == 0.0 }) {
            return null
        }

        val previousBuckets = when (metric) {
            GraphMetric.DAILY -> SpendingAggregator.aggregateByDay(previousExpenses, previousRange)
            GraphMetric.WEEKLY -> SpendingAggregator.aggregateByWeek(previousExpenses, previousRange)
            GraphMetric.MONTHLY -> SpendingAggregator.aggregateByMonth(previousExpenses, previousRange)
            GraphMetric.YEARLY -> SpendingAggregator.aggregateByYear(previousExpenses, previousRange)
        }

        val currentPoints = mapToEntries(currentBuckets, metric)
        val comparisonEntries = alignComparisonEntries(
            currentPoints.entries.size,
            previousBuckets.values.toList()
        )

        return GraphRenderData(
            metric = metric,
            dateRange = dateRange,
            entries = currentPoints.entries,
            comparisonEntries = comparisonEntries,
            labels = currentPoints.labels,
            rangeLabel = formatRangeLabel(dateRange),
            goalMax = goalMax,
            totalSpent = expenses.sumOf { it.amount },
            comparisonTotal = previousExpenses.sumOf { it.amount }
        )
    }

    private fun mapToEntries(
        data: Map<String, Double>,
        metric: GraphMetric
    ): GraphPoints {
        val labels = data.keys.map { raw -> formatLabel(raw, metric) }
        val entries = data.values.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }
        return GraphPoints(entries, labels)
    }

    private fun alignComparisonEntries(
        targetSize: Int,
        previousValues: List<Double>
    ): List<Entry> {
        if (targetSize == 0) return emptyList()

        val alignedValues = when {
            previousValues.isEmpty() -> List(targetSize) { 0.0 }
            previousValues.size == targetSize -> previousValues
            previousValues.size > targetSize -> previousValues.takeLast(targetSize)
            else -> List(targetSize - previousValues.size) { 0.0 } + previousValues
        }

        return alignedValues.mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }
    }

    private fun formatLabel(raw: String, metric: GraphMetric): String {
        return when (metric) {
            GraphMetric.DAILY -> LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("MMM d"))
            GraphMetric.WEEKLY -> LocalDate.parse(raw).format(DateTimeFormatter.ofPattern("MMM d"))
            GraphMetric.MONTHLY -> YearMonth.parse(raw).format(DateTimeFormatter.ofPattern("MMM yyyy"))
            GraphMetric.YEARLY -> raw
        }
    }

    private fun formatRangeLabel(dateRange: DateRange): String {
        val formatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        val startDate = LocalDate.parse(dateRange.start).format(formatter)
        val endDate = LocalDate.parse(dateRange.end).format(formatter)
        return "$startDate – $endDate"
    }

    private data class GraphPoints(
        val entries: List<Entry>,
        val labels: List<String>
    )
}
