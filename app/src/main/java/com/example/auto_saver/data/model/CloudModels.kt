package com.example.auto_saver.data.model

/**
 * Canonical data representations that mirror the Firestore schema defined in CLOUD.md.
 * These models are used throughout the remote layer and mapped to/from Room entities.
 */
data class UserProfile(
    val uid: String,
    val fullName: String = "",
    val contact: String = "",
    val contactLowercase: String? = null,
    val profilePhotoPath: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

enum class FriendRequestStatus {
    PENDING,
    ACCEPTED,
    DECLINED
}

data class FriendRequest(
    val id: String,
    val fromUid: String,
    val toUid: String,
    val fromEmail: String,
    val toEmail: String,
    val status: FriendRequestStatus,
    val createdAt: Long,
    val updatedAt: Long
)

data class FriendProfile(
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val photoUrl: String? = null,
    val since: Long = 0L
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

// ========================================
// Race/Challenge Models
// ========================================

enum class ChallengeStatus {
    PENDING,    // Created but not started
    ACTIVE,     // In progress
    COMPLETED,  // Ended, winner declared
    CANCELLED   // Cancelled by creator
}

data class RaceChallenge(
    val id: String,
    val name: String,
    val createdBy: String,
    val createdByEmail: String,
    val budget: Double,
    val startDate: String,      // yyyy-MM-dd
    val endDate: String,        // yyyy-MM-dd
    val status: ChallengeStatus,
    val participants: List<String> = emptyList(),  // participant UIDs
    val inviteCode: String = "",  // 6-char code for joining
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class RaceParticipant(
    val uid: String,
    val email: String,
    val displayName: String? = null,
    val totalSpent: Double = 0.0,
    val rank: Int = 1,
    val joinedAt: Long = 0L,
    val lastSyncedAt: Long = 0L
)