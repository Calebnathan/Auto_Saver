package com.example.auto_saver

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ExpenseTest {

    @Test
    fun `Expense can be created with all required fields`() {
        val expense = Expense(
            id = 1,
            userId = 100,
            categoryId = 5,
            date = "2025-10-06",
            amount = 50.99,
            description = "Lunch at restaurant"
        )

        assertThat(expense.id).isEqualTo(1)
        assertThat(expense.userId).isEqualTo(100)
        assertThat(expense.categoryId).isEqualTo(5)
        assertThat(expense.date).isEqualTo("2025-10-06")
        assertThat(expense.amount).isEqualTo(50.99)
        assertThat(expense.description).isEqualTo("Lunch at restaurant")
    }

    @Test
    fun `Expense can be created with optional time fields`() {
        val expense = Expense(
            id = 1,
            userId = 100,
            categoryId = 5,
            date = "2025-10-06",
            startTime = "12:00",
            endTime = "13:30",
            amount = 50.99
        )

        assertThat(expense.startTime).isEqualTo("12:00")
        assertThat(expense.endTime).isEqualTo("13:30")
    }

    @Test
    fun `Expense can be created with photo path`() {
        val expense = Expense(
            id = 1,
            userId = 100,
            categoryId = 5,
            date = "2025-10-06",
            amount = 50.99,
            photoPath = "/storage/photos/receipt.jpg"
        )

        assertThat(expense.photoPath).isEqualTo("/storage/photos/receipt.jpg")
    }

    @Test
    fun `Expense handles null optional fields`() {
        val expense = Expense(
            userId = 100,
            categoryId = 5,
            date = "2025-10-06",
            amount = 50.99
        )

        assertThat(expense.description).isNull()
        assertThat(expense.startTime).isNull()
        assertThat(expense.endTime).isNull()
        assertThat(expense.photoPath).isNull()
    }

    @Test
    fun `Expense equality works correctly`() {
        val expense1 = Expense(
            id = 1,
            userId = 100,
            categoryId = 5,
            date = "2025-10-06",
            amount = 50.99
        )
        val expense2 = Expense(
            id = 1,
            userId = 100,
            categoryId = 5,
            date = "2025-10-06",
            amount = 50.99
        )
        val expense3 = Expense(
            id = 2,
            userId = 100,
            categoryId = 5,
            date = "2025-10-07",
            amount = 75.00
        )

        assertThat(expense1).isEqualTo(expense2)
        assertThat(expense1).isNotEqualTo(expense3)
    }

    @Test
    fun `Expense handles large amounts`() {
        val expense = Expense(
            userId = 100,
            categoryId = 5,
            date = "2025-10-06",
            amount = 99999.99
        )

        assertThat(expense.amount).isEqualTo(99999.99)
    }

    @Test
    fun `Expense handles zero amount`() {
        val expense = Expense(
            userId = 100,
            categoryId = 5,
            date = "2025-10-06",
            amount = 0.0
        )

        assertThat(expense.amount).isEqualTo(0.0)
    }
}

