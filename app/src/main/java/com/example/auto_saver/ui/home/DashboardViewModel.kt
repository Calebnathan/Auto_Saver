package com.example.auto_saver.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auto_saver.DailySpendingPoint
import com.example.auto_saver.DateRange
import com.example.auto_saver.SpendingSummary
import com.example.auto_saver.UserPreferences
import com.example.auto_saver.data.model.ExpenseRecord
import com.example.auto_saver.data.repository.UnifiedExpenseRepository
import com.example.auto_saver.data.repository.UnifiedGoalRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
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

    // Daily aggregated spending for the selected date range
    val dailySpending: StateFlow<List<DailySpendingPoint>> = combine(
        expenses,
        dateRange
    ) { expenses, range ->
        val totalsByDate = expenses.groupBy { it.date }
            .mapValues { (_, items) -> items.sumOf { it.amount } }

        val start = LocalDate.parse(range.start)
        val end = LocalDate.parse(range.end)

        val points = mutableListOf<DailySpendingPoint>()
        var current = start
        while (!current.isAfter(end)) {
            val dateString = current.toString()
            val totalForDay = totalsByDate[dateString] ?: 0.0
            points.add(DailySpendingPoint(date = dateString, total = totalForDay))
            current = current.plusDays(1)
        }
        points
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setLast7Days() {
        _dateRange.value = DateRange.getLastNDays(7)
    }

    fun setLast30Days() {
        _dateRange.value = DateRange.getLastNDays(30)
    }

    fun setLast90Days() {
        _dateRange.value = DateRange.getLastNDays(90)
    }
}

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
