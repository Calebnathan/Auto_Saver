package com.example.auto_saver.data.model

/**
 * Canonical data representations that mirror the Firestore schema defined in CLOUD.md.
 * These models are used throughout the remote layer and mapped to/from Room entities.
 */
data class UserProfile(
    val uid: String,
    val fullName: String = "",
    val contact: String = "",
    val profilePhotoPath: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class CategoryRecord(
    val id: String,
    val uid: String,
    val name: String,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class GoalRecord(
    val id: String,
    val uid: String,
    val month: String,
    val minGoal: Double,
    val maxGoal: Double,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class ExpenseRecord(
    val id: String,
    val uid: String,
    val categoryId: String,
    val date: String,
    val amount: Double,
    val description: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val photoPath: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)
