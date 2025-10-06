package com.example.auto_saver

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class CategorySummaryTest {

    @Test
    fun `CategorySummary can be created with valid data`() {
        val summary = CategorySummary(
            categoryId = 1,
            categoryName = "Food",
            total = 250.50,
            percentage = 35.5f,
            expenseCount = 10
        )

        assertThat(summary.categoryId).isEqualTo(1)
        assertThat(summary.categoryName).isEqualTo("Food")
        assertThat(summary.total).isEqualTo(250.50)
        assertThat(summary.percentage).isEqualTo(35.5f)
        assertThat(summary.expenseCount).isEqualTo(10)
    }

    @Test
    fun `CategorySummary equality works correctly`() {
        val summary1 = CategorySummary(1, "Food", 250.50, 35.5f, 10)
        val summary2 = CategorySummary(1, "Food", 250.50, 35.5f, 10)
        val summary3 = CategorySummary(2, "Transport", 100.0, 20.0f, 5)

        assertThat(summary1).isEqualTo(summary2)
        assertThat(summary1).isNotEqualTo(summary3)
    }

    @Test
    fun `CategorySummary handles zero values`() {
        val summary = CategorySummary(
            categoryId = 1,
            categoryName = "Empty Category",
            total = 0.0,
            percentage = 0f,
            expenseCount = 0
        )

        assertThat(summary.total).isEqualTo(0.0)
        assertThat(summary.percentage).isEqualTo(0f)
        assertThat(summary.expenseCount).isEqualTo(0)
    }
}

