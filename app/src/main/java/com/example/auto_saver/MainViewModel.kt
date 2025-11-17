package com.example.auto_saver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auto_saver.data.model.ExpenseRecord
import com.example.auto_saver.data.repository.CategorySummaryCloud
import com.example.auto_saver.data.repository.UnifiedCategoryRepository
import com.example.auto_saver.data.repository.UnifiedExpenseRepository
import com.example.auto_saver.data.repository.UnifiedGoalRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * ViewModel for the main screen containing expense list, summaries, and filters
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val expenseRepository: UnifiedExpenseRepository,
    private val categoryRepository: UnifiedCategoryRepository,
    private val goalRepository: UnifiedGoalRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val uid: String = try {
        userPreferences.requireUserUid()
    } catch (e: IllegalStateException) {
        ""
    }

    // Date range filter
    private val _dateRange = MutableStateFlow(DateRange.getCurrentMonth())
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()

    // Expenses filtered by date range
    val expenses: StateFlow<List<ExpenseRecord>> = dateRange
        .flatMapLatest { range ->
            if (uid.isNotEmpty()) {
                expenseRepository.observeExpenses(uid, range.start, range.end)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category summaries
    val categorySummaries: StateFlow<List<CategorySummaryCloud>> = dateRange
        .flatMapLatest { range ->
            if (uid.isNotEmpty()) {
                expenseRepository.getCategorySummaries(uid, range.start, range.end)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total spent in current date range
    val totalSpent: StateFlow<Double> = dateRange
        .flatMapLatest { range ->
            if (uid.isNotEmpty()) {
                expenseRepository.getTotalSpent(uid, range.start, range.end)
            } else {
                flowOf(0.0)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // All categories for dropdowns
    val categories: StateFlow<List<com.example.auto_saver.data.model.CategoryRecord>> = if (uid.isNotEmpty()) {
        categoryRepository.observeCategories(uid)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    // Check if user has any expenses (for empty state)
    val hasExpenses: StateFlow<Boolean> = expenses
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Spending summary with goal progress
    val spendingSummary: StateFlow<SpendingSummary?> = combine(
        totalSpent,
        dateRange
    ) { total, range ->
        if (uid.isEmpty()) return@combine null
        
        // Get goal for current month
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

    /**
     * Update the date range filter
     */
    fun updateDateRange(start: String, end: String) {
        _dateRange.value = DateRange(start, end)
    }

    /**
     * Set predefined date ranges
     */
    fun setThisMonth() {
        _dateRange.value = DateRange.getCurrentMonth()
    }

    fun setThisWeek() {
        _dateRange.value = DateRange.getCurrentWeek()
    }

    fun setLast30Days() {
        _dateRange.value = DateRange.getLast30Days()
    }

    /**
     * Delete an expense
     */
    fun deleteExpense(expenseId: String) {
        if (uid.isEmpty()) return
        
        viewModelScope.launch {
            expenseRepository.deleteExpense(uid, expenseId)
        }
    }

    /**
     * Get user name for display
     */
    fun getUserName(): String {
        return userPreferences.getUserName() ?: "User"
    }
}

/**
 * Factory for creating MainViewModel with dependencies
 */
class MainViewModelFactory(
    private val expenseRepository: UnifiedExpenseRepository,
    private val categoryRepository: UnifiedCategoryRepository,
    private val goalRepository: UnifiedGoalRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(expenseRepository, categoryRepository, goalRepository, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
