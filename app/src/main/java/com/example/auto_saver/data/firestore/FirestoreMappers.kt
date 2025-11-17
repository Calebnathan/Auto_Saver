package com.example.auto_saver.data.firestore

import com.example.auto_saver.data.model.CategoryRecord
import com.example.auto_saver.data.model.ChallengeStatus
import com.example.auto_saver.data.model.ExpenseRecord
import com.example.auto_saver.data.model.FriendProfile
import com.example.auto_saver.data.model.FriendRequest
import com.example.auto_saver.data.model.FriendRequestStatus
import com.example.auto_saver.data.model.GoalRecord
import com.example.auto_saver.data.model.RaceChallenge
import com.example.auto_saver.data.model.RaceParticipant
import com.example.auto_saver.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

fun DocumentSnapshot.toUserProfile(): UserProfile? {
    if (!exists()) return null
    val fullName = getString("fullName") ?: ""
    val contact = getString("contact") ?: ""
    val profilePhotoPath = getString("profilePhotoPath")
    return UserProfile(
        uid = id,
        fullName = fullName,
        contact = contact,
        contactLowercase = getString("contactLowercase"),
        profilePhotoPath = profilePhotoPath,
        createdAt = getTimestamp("createdAt").toEpochMilli(),
        updatedAt = getTimestamp("updatedAt").toEpochMilli()
    )
}

fun DocumentSnapshot.toCategoryRecord(uid: String): CategoryRecord? {
    if (!exists()) return null
    val name = getString("name") ?: return null
    return CategoryRecord(
        id = id,
        uid = uid,
        name = name,
        createdAt = getTimestamp("createdAt").toEpochMilli(),
        updatedAt = getTimestamp("updatedAt").toEpochMilli()
    )
}

fun DocumentSnapshot.toGoalRecord(uid: String): GoalRecord? {
    if (!exists()) return null
    val month = getString("month") ?: id
    val minGoal = getNumber("minGoal")?.toDouble() ?: return null
    val maxGoal = getNumber("maxGoal")?.toDouble() ?: return null
    return GoalRecord(
        id = id,
        uid = uid,
        month = month,
        minGoal = minGoal,
        maxGoal = maxGoal,
        createdAt = getTimestamp("createdAt").toEpochMilli(),
        updatedAt = getTimestamp("updatedAt").toEpochMilli()
    )
}

fun DocumentSnapshot.toExpenseRecord(uid: String): ExpenseRecord? {
    if (!exists()) return null
    val amount = getNumber("amount")?.toDouble() ?: return null
    val date = getString("date") ?: return null
    val categoryId = getString("categoryId") ?: return null
    return ExpenseRecord(
        id = id,
        uid = uid,
        categoryId = categoryId,
        date = date,
        amount = amount,
        description = getString("description"),
        startTime = getString("startTime"),
        endTime = getString("endTime"),
        photoPath = getString("photoPath"),
        createdAt = getTimestamp("createdAt").toEpochMilli(),
        updatedAt = getTimestamp("updatedAt").toEpochMilli()
    )
}

fun UserProfile.toFirestorePayload(isNew: Boolean): Map<String, Any?> =
    hashMapOf<String, Any?>(
        "fullName" to fullName,
        "contact" to contact,
        "contactLowercase" to contact.lowercase(),
        "profilePhotoPath" to profilePhotoPath,
        "updatedAt" to FieldValue.serverTimestamp()
    ).apply {
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
    }

fun CategoryRecord.toFirestorePayload(isNew: Boolean): Map<String, Any?> =
    hashMapOf<String, Any?>(
        "name" to name,
        "updatedAt" to FieldValue.serverTimestamp()
    ).apply {
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
    }

fun GoalRecord.toFirestorePayload(isNew: Boolean): Map<String, Any?> =
    hashMapOf<String, Any?>(
        "month" to month,
        "minGoal" to minGoal,
        "maxGoal" to maxGoal,
        "updatedAt" to FieldValue.serverTimestamp()
    ).apply {
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
    }

fun ExpenseRecord.toFirestorePayload(isNew: Boolean): Map<String, Any?> =
    hashMapOf<String, Any?>(
        "categoryId" to categoryId,
        "date" to date,
        "amount" to amount,
        "description" to description,
        "startTime" to startTime,
        "endTime" to endTime,
        "photoPath" to photoPath,
        "updatedAt" to FieldValue.serverTimestamp()
    ).apply {
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
    }

fun DocumentSnapshot.toFriendRequest(): FriendRequest? {
    if (!exists()) return null
    val fromUid = getString("fromUid") ?: return null
    val toUid = getString("toUid") ?: return null
    val fromEmail = getString("fromEmail") ?: ""
    val toEmail = getString("toEmail") ?: ""
    val statusValue = getString("status") ?: FriendRequestStatus.PENDING.name
    return FriendRequest(
        id = id,
        fromUid = fromUid,
        toUid = toUid,
        fromEmail = fromEmail,
        toEmail = toEmail,
        status = runCatching { FriendRequestStatus.valueOf(statusValue) }.getOrDefault(FriendRequestStatus.PENDING),
        createdAt = getTimestamp("createdAt").toEpochMilli(),
        updatedAt = getTimestamp("updatedAt").toEpochMilli()
    )
}

fun DocumentSnapshot.toFriendProfile(): FriendProfile? {
    if (!exists()) return null
    val friendUid = getString("friendUid") ?: return null
    val email = getString("friendEmail") ?: ""
    return FriendProfile(
        uid = friendUid,
        email = email,
        displayName = getString("displayName"),
        photoUrl = getString("photoUrl"),
        since = getTimestamp("since").toEpochMilli()
    )
}

fun DocumentSnapshot.toRaceChallenge(): RaceChallenge? {
    if (!exists()) return null
    val name = getString("name") ?: return null
    val createdBy = getString("createdBy") ?: return null
    val createdByEmail = getString("createdByEmail") ?: ""
    val budget = getNumber("budget")?.toDouble() ?: return null
    val startDate = getString("startDate") ?: return null
    val endDate = getString("endDate") ?: return null
    val statusValue = getString("status") ?: ChallengeStatus.PENDING.name
    val participants = get("participants") as? List<*>
    val participantsList = participants?.filterIsInstance<String>() ?: emptyList()
    val inviteCode = getString("inviteCode") ?: ""
    
    return RaceChallenge(
        id = id,
        name = name,
        createdBy = createdBy,
        createdByEmail = createdByEmail,
        budget = budget,
        startDate = startDate,
        endDate = endDate,
        status = runCatching { ChallengeStatus.valueOf(statusValue) }.getOrDefault(ChallengeStatus.PENDING),
        participants = participantsList,
        inviteCode = inviteCode,
        createdAt = getTimestamp("createdAt").toEpochMilli(),
        updatedAt = getTimestamp("updatedAt").toEpochMilli()
    )
}

fun DocumentSnapshot.toRaceParticipant(): RaceParticipant? {
    if (!exists()) return null
    val uid = getString("uid") ?: id
    val email = getString("email") ?: ""
    val totalSpent = getNumber("totalSpent")?.toDouble() ?: 0.0
    val rank = getNumber("rank")?.toInt() ?: 1
    
    return RaceParticipant(
        uid = uid,
        email = email,
        displayName = getString("displayName"),
        totalSpent = totalSpent,
        rank = rank,
        joinedAt = getTimestamp("joinedAt").toEpochMilli(),
        lastSyncedAt = getTimestamp("lastSyncedAt").toEpochMilli()
    )
}

fun RaceChallenge.toFirestorePayload(isNew: Boolean): Map<String, Any?> =
    hashMapOf<String, Any?>(
        "name" to name,
        "createdBy" to createdBy,
        "createdByEmail" to createdByEmail,
        "budget" to budget,
        "startDate" to startDate,
        "endDate" to endDate,
        "status" to status.name,
        "participants" to participants,
        "inviteCode" to inviteCode,
        "updatedAt" to FieldValue.serverTimestamp()
    ).apply {
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
    }

fun RaceParticipant.toFirestorePayload(isNew: Boolean): Map<String, Any?> =
    hashMapOf<String, Any?>(
        "uid" to uid,
        "email" to email,
        "displayName" to displayName,
        "totalSpent" to totalSpent,
        "rank" to rank,
        "lastSyncedAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    ).apply {
        if (isNew) put("joinedAt", FieldValue.serverTimestamp())
    }

private fun DocumentSnapshot.getNumber(key: String): Number? = when (val value = get(key)) {
    is Number -> value
    else -> null
}

private fun Timestamp?.toEpochMilli(): Long = this?.toDate()?.time ?: System.currentTimeMillis()