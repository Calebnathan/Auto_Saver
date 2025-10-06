package com.example.auto_saver

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * ViewModel for the main screen containing expense list, summaries, and filters
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: ExpenseRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val currentUserId: Int = userPreferences.getCurrentUserId()

    // Date range filter
    private val _dateRange = MutableStateFlow(DateRange.getCurrentMonth())
    val dateRange: StateFlow<DateRange> = _dateRange.asStateFlow()

    // Expenses filtered by date range
    val expenses: StateFlow<List<Expense>> = dateRange
        .flatMapLatest { range ->
            repository.getExpensesByDateRange(currentUserId, range.start, range.end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category summaries
    val categorySummaries: StateFlow<List<CategorySummary>> = dateRange
        .flatMapLatest { range ->
            repository.getCategorySummaries(currentUserId, range.start, range.end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total spent in current date range
    val totalSpent: StateFlow<Double> = dateRange
        .flatMapLatest { range ->
            repository.getTotalSpent(currentUserId, range.start, range.end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // All categories for dropdowns
    val categories: StateFlow<List<Category>> = repository.getCategories(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Check if user has any expenses (for empty state)
    val hasExpenses: StateFlow<Boolean> = repository.hasExpenses(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // Spending summary with goal progress
    val spendingSummary: StateFlow<SpendingSummary?> = totalSpent
        .map { total ->
            val goal = repository.getCurrentMonthGoal(currentUserId)
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
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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
    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
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
    private val repository: ExpenseRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
