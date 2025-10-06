package com.example.auto_saver.models

import com.example.auto_saver.Category
import com.example.auto_saver.Expense

/**
 * Sealed class representing different types of items in the expense list
 */
sealed class ExpenseListItem {
    /**
     * A category header that can be expanded/collapsed
     */
    data class CategoryHeader(
        val category: Category,
        val expenses: List<Expense>,
        val isExpanded: Boolean = false
    ) : ExpenseListItem()

    /**
     * A single expense item
     */
    data class ExpenseItem(
        val expense: Expense,
        val categoryName: String
    ) : ExpenseListItem()
}

