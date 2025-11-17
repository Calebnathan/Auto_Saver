package com.example.auto_saver.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auto_saver.Category
import com.example.auto_saver.Expense
import com.example.auto_saver.UserPreferences
import com.example.auto_saver.data.repository.UnifiedCategoryRepository
import com.example.auto_saver.data.repository.UnifiedExpenseRepository
import com.example.auto_saver.models.ExpenseListItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CategoriesViewModel(
    private val expenseRepository: UnifiedExpenseRepository,
    private val categoryRepository: UnifiedCategoryRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val uid: String? = try {
        userPreferences.requireUserUid()
    } catch (e: IllegalStateException) {
        null
    }

    private val expandedCategories = MutableStateFlow<Set<String>>(emptySet())

    private val categories: StateFlow<List<Category>> =
        if (uid != null) {
            categoryRepository.observeCategories(uid)
                .mapToDomainCategories()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        } else {
            MutableStateFlow(emptyList())
        }

    private val expenses: StateFlow<List<Expense>> =
        if (uid != null) {
            expenseRepository.observeExpenses(uid, null, null)
                .mapToDomainExpenses()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        } else {
            MutableStateFlow(emptyList())
        }

    val items: StateFlow<List<ExpenseListItem>> = combine(
        categories,
        expenses,
        expandedCategories
    ) { categories, expenses, expanded ->
        buildItems(categories, expenses, expanded)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasContent: StateFlow<Boolean> = items
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun toggleCategory(categoryId: String) {
        expandedCategories.value = expandedCategories.value.toMutableSet().apply {
            if (contains(categoryId)) remove(categoryId) else add(categoryId)
        }
    }

    private fun buildItems(
        categories: List<Category>,
        expenses: List<Expense>,
        expanded: Set<String>
    ): List<ExpenseListItem> {
        if (categories.isEmpty()) return emptyList()

        val expensesByCategory = expenses.groupBy { it.categoryId }
        val result = mutableListOf<ExpenseListItem>()

        categories.forEach { category ->
            val categoryExpenses = expensesByCategory[category.id] ?: emptyList()
            val isExpanded = expanded.contains(category.id.toString())
            result.add(
                ExpenseListItem.CategoryHeader(
                    category = category,
                    expenses = categoryExpenses,
                    isExpanded = isExpanded
                )
            )
            if (isExpanded) {
                result.addAll(
                    categoryExpenses.map { expense ->
                        ExpenseListItem.ExpenseItem(
                            expense = expense,
                            categoryName = category.name
                        )
                    }
                )
            }
        }
        return result
    }

    private fun kotlinx.coroutines.flow.Flow<List<com.example.auto_saver.data.model.CategoryRecord>>.mapToDomainCategories() =
        flatMapLatest { records ->
            flowOf(
                records.map {
                    Category(
                        id = it.id.hashCode(),
                        userId = userPreferences.getCurrentUserId(),
                        name = it.name
                    )
                }
            )
        }

    private fun kotlinx.coroutines.flow.Flow<List<com.example.auto_saver.data.model.ExpenseRecord>>.mapToDomainExpenses() =
        flatMapLatest { records ->
            flowOf(
                records.map {
                    Expense(
                        id = it.id.hashCode(),
                        userId = userPreferences.getCurrentUserId(),
                        // Keep category grouping consistent with Category.id, which uses hashCode()
                        categoryId = it.categoryId.hashCode(),
                        date = it.date,
                        amount = it.amount,
                        description = it.description,
                        startTime = it.startTime,
                        endTime = it.endTime,
                        photoPath = it.photoPath
                    )
                }
            )
        }
}

class CategoriesViewModelFactory(
    private val expenseRepository: UnifiedExpenseRepository,
    private val categoryRepository: UnifiedCategoryRepository,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoriesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoriesViewModel(expenseRepository, categoryRepository, userPreferences) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
