package com.example.auto_saver.utils

import kotlin.math.min

/**
 * Utility class for calculating goal progress and converting to visual positions
 */
object GoalProgressCalculator {

    /**
     * Calculates progress percentage (0-100)
     * @param currentSpent Amount spent so far
     * @param goalMin Minimum goal threshold
     * @param goalMax Maximum goal threshold
     * @return Progress from 0 (start) to 100 (goal reached)
     */
    fun calculateProgress(
        currentSpent: Double,
        goalMin: Double?,
        goalMax: Double?
    ): Float {
        // No goal set - car stays at start
        if (goalMin == null || goalMax == null) return 0f

        // Handle edge case where min equals max
        if (goalMin == goalMax) {
            return if (currentSpent >= goalMin) 100f else 0f
        }

        // Spending is below minimum goal
        if (currentSpent < goalMin) {
            return ((currentSpent / goalMin) * 50).toFloat()
        }

        // Spending is between min and max
        if (currentSpent <= goalMax) {
            val range = goalMax - goalMin
            val progress = (currentSpent - goalMin) / range
            return (50 + progress * 50).toFloat()
        }

        // Exceeded max goal - car at finish line
        return 100f
    }

    /**
     * Converts progress percentage to angle on circular track
     * Track goes from bottom (270°) clockwise around to finish
     * @param progress 0-100
     * @return Angle in degrees (270° = start, 180° = finish)
     */
    fun progressToAngle(progress: Float): Float {
        // Start at 270° (bottom), go clockwise
        // 270° -> 180° -> 90° -> 0° -> -90° (which is 270°)
        val clampedProgress = min(progress, 100f)

        // Map 0-100 progress to 270° rotation (3/4 circle)
        val rotationDegrees = clampedProgress * 2.7f // 270° total

        // Start at bottom (270°) and subtract rotation to go clockwise
        return 270f - rotationDegrees
    }

    /**
     * Calculate X and Y position on circular track
     * @param centerX Center X of the track
     * @param centerY Center Y of the track
     * @param radius Radius of the track
     * @param angle Angle in degrees
     * @return Pair of (x, y) coordinates
     */
    fun calculatePosition(
        centerX: Float,
        centerY: Float,
        radius: Float,
        angle: Float
    ): Pair<Float, Float> {
        val angleRad = Math.toRadians(angle.toDouble())
        val x = centerX + radius * Math.cos(angleRad).toFloat()
        val y = centerY + radius * Math.sin(angleRad).toFloat()
        return Pair(x, y)
    }
}

