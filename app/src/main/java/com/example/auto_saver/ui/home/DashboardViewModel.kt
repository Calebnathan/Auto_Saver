package com.example.auto_saver.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auto_saver.DateRange
import com.example.auto_saver.SpendingSummary
import com.example.auto_saver.UserPreferences
import com.example.auto_saver.data.model.ExpenseRecord
import com.example.auto_saver.data.repository.UnifiedExpenseRepository
import com.example.auto_saver.data.repository.UnifiedGoalRepository
import com.example.auto_saver.ui.components.GraphDataProvider
import com.example.auto_saver.ui.components.GraphMetric
import com.example.auto_saver.ui.components.GraphUiState
import com.example.auto_saver.utils.SpendingAggregator
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * ViewModel that exposes dashboard metrics for the Home > Dashboard tab.
 *
 * It mirrors the key behaviours of MainViewModel but is scoped to the
 * dashboard use case and ready for future dashboard-specific extensions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val expenseRepository: UnifiedExpenseRepository,
    private val goalRepository: UnifiedGoalRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val uid: String = try {
        userPreferences.requireUserUid()
    } catch (e: IllegalStateException) {
        ""
    }

    // Date range for the dashboard (defaults to last 30 days for the graph)
    private val _dateRange = MutableStateFlow(DateRange.getLast30Days())
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()

    private val _graphMetric = MutableStateFlow(GraphMetric.DAILY)
    val graphMetric: StateFlow<GraphMetric> = _graphMetric.asStateFlow()

    // Expenses for the selected period
    val expenses: StateFlow<List<ExpenseRecord>> = dateRange
        .flatMapLatest { range ->
            if (uid.isNotEmpty()) {
                expenseRepository.observeExpenses(uid, range.start, range.end)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val previousExpenses: StateFlow<List<ExpenseRecord>> = dateRange
        .flatMapLatest { range ->
            if (uid.isNotEmpty()) {
                val previous = range.previousPeriod()
                expenseRepository.observeExpenses(uid, previous.start, previous.end)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total spending for the selected period
    val totalSpent: StateFlow<Double> = dateRange
        .flatMapLatest { range ->
            if (uid.isNotEmpty()) {
                expenseRepository.getTotalSpent(uid, range.start, range.end)
            } else {
                flowOf(0.0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Count of expenses in the selected period
    val expenseCount: StateFlow<Int> = expenses
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Spending summary with goal progress for the current month
    val spendingSummary: StateFlow<SpendingSummary?> = combine(
        totalSpent,
        dateRange
    ) { total, _ ->
        if (uid.isEmpty()) return@combine null

        val currentMonth = java.time.YearMonth.now().toString()
        val goal = goalRepository.getGoalForMonth(uid, currentMonth)

        if (goal != null) {
            val percentage = if (goal.maxGoal > 0) {
                ((total / goal.maxGoal) * 100).toFloat()
            } else 0f

            SpendingSummary(
                totalSpent = total,
                goalMin = goal.minGoal,
                goalMax = goal.maxGoal,
                percentageOfGoal = percentage,
                isOverBudget = total > goal.maxGoal
            )
        } else {
            SpendingSummary(
                totalSpent = total,
                goalMin = null,
                goalMax = null,
                percentageOfGoal = 0f,
                isOverBudget = false
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val goalLimit: StateFlow<Double?> = dateRange
        .flatMapLatest { range ->
            if (uid.isEmpty()) {
                flowOf<Double?>(null)
            } else {
                val goalMonth = resolveGoalMonth(range)
                if (goalMonth == null) {
                    flowOf<Double?>(null)
                } else {
                    flow {
                        emit(goalRepository.getGoalForMonth(uid, goalMonth)?.maxGoal)
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Quick stats for dashboard
    val quickStats: StateFlow<QuickStats?> = combine(
        expenses,
        dateRange
    ) { currentExpenses, range ->
        if (uid.isEmpty() || currentExpenses.isEmpty()) return@combine null

        val statistics = SpendingAggregator.calculateStatistics(currentExpenses, range)
        val topCategories = SpendingAggregator.getTopCategories(currentExpenses, range, limit = 1)
        val topCategory = topCategories.firstOrNull()

        val today = LocalDate.now()
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
        val daysUntilReset = ChronoUnit.DAYS.between(today, endOfMonth).toInt() + 1

        QuickStats(
            averageDailySpending = statistics.averagePerDay,
            topCategoryId = topCategory?.categoryId,
            topCategoryAmount = topCategory?.total ?: 0.0,
            daysUntilBudgetReset = daysUntilReset,
            maxDailySpending = statistics.maxDailySpending,
            daysWithExpenses = statistics.daysWithExpenses
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val graphState: StateFlow<GraphUiState> = combine(
        expenses,
        previousExpenses,
        dateRange,
        graphMetric,
        goalLimit
    ) { currentExpenses, previousExpenses, range, metric, goalMax ->
        if (uid.isEmpty()) {
            return@combine GraphUiState.Error("User session expired")
        }

        val renderData = GraphDataProvider.buildRenderData(
            metric = metric,
            dateRange = range,
            expenses = currentExpenses,
            previousExpenses = previousExpenses,
            goalMax = goalMax
        )

        if (renderData == null) {
            GraphUiState.Empty
        } else {
            GraphUiState.Success(renderData)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GraphUiState.Loading)

    fun setLast7Days() {
        _dateRange.value = DateRange.getLastNDays(7)
    }

    fun setLast30Days() {
        _dateRange.value = DateRange.getLastNDays(30)
    }

    fun setLast90Days() {
        _dateRange.value = DateRange.getLastNDays(90)
    }

    fun setGraphMetric(metric: GraphMetric) {
        _graphMetric.value = metric
    }

    private fun resolveGoalMonth(range: DateRange): String? {
        return try {
            val startMonth = YearMonth.from(LocalDate.parse(range.start))
            val endMonth = YearMonth.from(LocalDate.parse(range.end))
            if (startMonth == endMonth) endMonth.toString() else null
        } catch (e: Exception) {
            null
        }
    }
}

data class QuickStats(
    val averageDailySpending: Double,
    val topCategoryId: String?,
    val topCategoryAmount: Double,
    val daysUntilBudgetReset: Int,
    val maxDailySpending: Double,
    val daysWithExpenses: Int
)

class DashboardViewModelFactory(
    private val expenseRepository: UnifiedExpenseRepository,
    private val goalRepository: UnifiedGoalRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DashboardViewModel(expenseRepository, goalRepository, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}