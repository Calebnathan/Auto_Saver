package com.example.auto_saver

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SpendingSummaryTest {

    @Test
    fun `SpendingSummary correctly identifies when over budget`() {
        val summary = SpendingSummary(
            totalSpent = 1500.0,
            goalMin = 500.0,
            goalMax = 1000.0,
            percentageOfGoal = 150f,
            isOverBudget = true
        )

        assertThat(summary.isOverBudget).isTrue()
        assertThat(summary.totalSpent).isGreaterThan(summary.goalMax!!)
    }

    @Test
    fun `SpendingSummary correctly identifies when under budget`() {
        val summary = SpendingSummary(
            totalSpent = 750.0,
            goalMin = 500.0,
            goalMax = 1000.0,
            percentageOfGoal = 75f,
            isOverBudget = false
        )

        assertThat(summary.isOverBudget).isFalse()
        assertThat(summary.totalSpent).isLessThan(summary.goalMax!!)
    }

    @Test
    fun `SpendingSummary handles no goal set`() {
        val summary = SpendingSummary(
            totalSpent = 500.0,
            goalMin = null,
            goalMax = null,
            percentageOfGoal = 0f,
            isOverBudget = false
        )

        assertThat(summary.goalMin).isNull()
        assertThat(summary.goalMax).isNull()
        assertThat(summary.isOverBudget).isFalse()
    }

    @Test
    fun `SpendingSummary calculates correct percentage`() {
        val totalSpent = 750.0
        val goalMax = 1000.0
        val expectedPercentage = (totalSpent / goalMax * 100).toFloat()

        val summary = SpendingSummary(
            totalSpent = totalSpent,
            goalMin = 500.0,
            goalMax = goalMax,
            percentageOfGoal = expectedPercentage,
            isOverBudget = false
        )

        assertThat(summary.percentageOfGoal).isEqualTo(75f)
    }

    @Test
    fun `SpendingSummary handles zero spending`() {
        val summary = SpendingSummary(
            totalSpent = 0.0,
            goalMin = 500.0,
            goalMax = 1000.0,
            percentageOfGoal = 0f,
            isOverBudget = false
        )

        assertThat(summary.totalSpent).isEqualTo(0.0)
        assertThat(summary.percentageOfGoal).isEqualTo(0f)
        assertThat(summary.isOverBudget).isFalse()
    }
}

