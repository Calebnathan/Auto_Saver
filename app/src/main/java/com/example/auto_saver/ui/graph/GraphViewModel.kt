package com.example.auto_saver.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auto_saver.DateRange
import com.example.auto_saver.UserPreferences
import com.example.auto_saver.data.model.ExpenseRecord
import com.example.auto_saver.data.repository.UnifiedExpenseRepository
import com.example.auto_saver.data.repository.UnifiedGoalRepository
import com.example.auto_saver.ui.components.GraphDataProvider
import com.example.auto_saver.ui.components.GraphMetric
import com.example.auto_saver.ui.components.GraphUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeParseException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class GraphViewModel(
    private val expenseRepository: UnifiedExpenseRepository,
    private val goalRepository: UnifiedGoalRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val uid: String = try {
        userPreferences.requireUserUid()
    } catch (e: IllegalStateException) {
        ""
    }

    private val _dateRange = MutableStateFlow(DateRange.getLast30Days())
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()

    private val _graphMetric = MutableStateFlow(GraphMetric.DAILY)
    val graphMetric: StateFlow<GraphMetric> = _graphMetric.asStateFlow()

    private val goalLimit: StateFlow<Double?> = dateRange
        .flatMapLatest { range ->
            if (uid.isEmpty()) {
                flowOf<Double?>(null)
            } else {
                val month = resolveGoalMonth(range)
                if (month == null) {
                    flowOf<Double?>(null)
                } else {
                    flow {
                        emit(goalRepository.getGoalForMonth(uid, month)?.maxGoal)
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    val graphState: StateFlow<GraphUiState> = combine(
        expenses,
        previousExpenses,
        dateRange,
        graphMetric,
        goalLimit
    ) { current, previous, range, metric, goal ->
        if (uid.isEmpty()) {
            return@combine GraphUiState.Error("User session expired")
        }

        val renderData = GraphDataProvider.buildRenderData(
            metric = metric,
            dateRange = range,
            expenses = current,
            previousExpenses = previous,
            goalMax = goal
        )

        if (renderData == null) GraphUiState.Empty else GraphUiState.Success(renderData)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GraphUiState.Loading)

    fun setMetric(metric: GraphMetric) {
        _graphMetric.value = metric
    }

    fun setDateRange(start: String, end: String) {
        normalizeRange(start, end)?.let { _dateRange.value = it }
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

    private fun normalizeRange(start: String, end: String): DateRange? {
        return try {
            val startDate = LocalDate.parse(start)
            val endDate = LocalDate.parse(end)
            if (startDate.isAfter(endDate)) {
                DateRange(endDate.toString(), startDate.toString())
            } else {
                DateRange(startDate.toString(), endDate.toString())
            }
        } catch (e: DateTimeParseException) {
            null
        }
    }
}

class GraphViewModelFactory(
    private val expenseRepository: UnifiedExpenseRepository,
    private val goalRepository: UnifiedGoalRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GraphViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GraphViewModel(expenseRepository, goalRepository, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}