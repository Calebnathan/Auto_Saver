package com.example.auto_saver.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class GoalProgressCalculatorTest {

    @Test
    fun `calculateProgress returns 0 when no goal is set`() {
        val progress = GoalProgressCalculator.calculateProgress(
            currentSpent = 100.0,
            goalMin = null,
            goalMax = null
        )
        assertThat(progress).isEqualTo(0f)
    }

    @Test
    fun `calculateProgress returns 0 when spending is zero`() {
        val progress = GoalProgressCalculator.calculateProgress(
            currentSpent = 0.0,
            goalMin = 500.0,
            goalMax = 1000.0
        )
        assertThat(progress).isEqualTo(0f)
    }

    @Test
    fun `calculateProgress returns 50 percent when spending equals goalMin`() {
        val progress = GoalProgressCalculator.calculateProgress(
            currentSpent = 500.0,
            goalMin = 500.0,
            goalMax = 1000.0
        )
        assertThat(progress).isEqualTo(50f)
    }

    @Test
    fun `calculateProgress returns 100 percent when spending equals goalMax`() {
        val progress = GoalProgressCalculator.calculateProgress(
            currentSpent = 1000.0,
            goalMin = 500.0,
            goalMax = 1000.0
        )
        assertThat(progress).isEqualTo(100f)
    }

    @Test
    fun `calculateProgress returns 100 percent when spending exceeds goalMax`() {
        val progress = GoalProgressCalculator.calculateProgress(
            currentSpent = 1500.0,
            goalMin = 500.0,
            goalMax = 1000.0
        )
        assertThat(progress).isEqualTo(100f)
    }

    @Test
    fun `calculateProgress returns correct value between 0 and goalMin`() {
        // Spending $250 out of $500 min should be 25% progress
        val progress = GoalProgressCalculator.calculateProgress(
            currentSpent = 250.0,
            goalMin = 500.0,
            goalMax = 1000.0
        )
        assertThat(progress).isEqualTo(25f)
    }

    @Test
    fun `calculateProgress returns correct value between goalMin and goalMax`() {
        // Spending $750 (halfway between 500 and 1000) should be 75% progress
        val progress = GoalProgressCalculator.calculateProgress(
            currentSpent = 750.0,
            goalMin = 500.0,
            goalMax = 1000.0
        )
        assertThat(progress).isEqualTo(75f)
    }

    @Test
    fun `calculateProgress handles edge case where goalMin equals goalMax`() {
        val progressUnder = GoalProgressCalculator.calculateProgress(
            currentSpent = 400.0,
            goalMin = 500.0,
            goalMax = 500.0
        )
        assertThat(progressUnder).isEqualTo(0f)

        val progressMet = GoalProgressCalculator.calculateProgress(
            currentSpent = 500.0,
            goalMin = 500.0,
            goalMax = 500.0
        )
        assertThat(progressMet).isEqualTo(100f)
    }

    @Test
    fun `progressToAngle returns 270 degrees at start (0 percent progress)`() {
        val angle = GoalProgressCalculator.progressToAngle(0f)
        assertThat(angle).isEqualTo(270f)
    }

    @Test
    fun `progressToAngle returns 0 degrees at finish (100 percent progress)`() {
        val angle = GoalProgressCalculator.progressToAngle(100f)
        assertThat(angle).isEqualTo(0f)
    }

    @Test
    fun `progressToAngle returns correct angle for 50 percent progress`() {
        val angle = GoalProgressCalculator.progressToAngle(50f)
        assertThat(angle).isEqualTo(135f)
    }

    @Test
    fun `progressToAngle clamps values over 100 percent`() {
        val angle = GoalProgressCalculator.progressToAngle(150f)
        assertThat(angle).isEqualTo(0f)
    }

    @Test
    fun `calculatePosition returns correct coordinates on circle`() {
        val centerX = 100f
        val centerY = 100f
        val radius = 50f
        val angle = 0f // Right side of circle

        val (x, y) = GoalProgressCalculator.calculatePosition(centerX, centerY, radius, angle)

        // At 0 degrees, point should be at (centerX + radius, centerY)
        assertThat(x).isWithin(0.1f).of(150f)
        assertThat(y).isWithin(0.1f).of(100f)
    }

    @Test
    fun `calculatePosition returns correct coordinates at 90 degrees`() {
        val centerX = 100f
        val centerY = 100f
        val radius = 50f
        val angle = 90f // Top of circle

        val (x, y) = GoalProgressCalculator.calculatePosition(centerX, centerY, radius, angle)

        // At 90 degrees, point should be at (centerX, centerY + radius)
        assertThat(x).isWithin(0.1f).of(100f)
        assertThat(y).isWithin(0.1f).of(150f)
    }

    @Test
    fun `calculatePosition returns correct coordinates at 180 degrees`() {
        val centerX = 100f
        val centerY = 100f
        val radius = 50f
        val angle = 180f // Left side of circle

        val (x, y) = GoalProgressCalculator.calculatePosition(centerX, centerY, radius, angle)

        // At 180 degrees, point should be at (centerX - radius, centerY)
        assertThat(x).isWithin(0.1f).of(50f)
        assertThat(y).isWithin(0.1f).of(100f)
    }

    @Test
    fun `calculatePosition returns correct coordinates at 270 degrees`() {
        val centerX = 100f
        val centerY = 100f
        val radius = 50f
        val angle = 270f // Bottom of circle

        val (x, y) = GoalProgressCalculator.calculatePosition(centerX, centerY, radius, angle)

        // At 270 degrees, point should be at (centerX, centerY - radius)
        assertThat(x).isWithin(0.1f).of(100f)
        assertThat(y).isWithin(0.1f).of(50f)
    }
}

