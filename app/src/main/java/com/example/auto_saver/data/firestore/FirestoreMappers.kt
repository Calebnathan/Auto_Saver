package com.example.auto_saver.data.firestore

import com.example.auto_saver.data.model.CategoryRecord
import com.example.auto_saver.data.model.ChallengeStatus
import com.example.auto_saver.data.model.CollaborativeGoal
import com.example.auto_saver.data.model.ExpenseRecord
import com.example.auto_saver.data.model.FriendProfile
import com.example.auto_saver.data.model.FriendRequest
import com.example.auto_saver.data.model.FriendRequestStatus
import com.example.auto_saver.data.model.GoalCategory
import com.example.auto_saver.data.model.GoalContribution
import com.example.auto_saver.data.model.GoalRecord
import com.example.auto_saver.data.model.GoalStatus
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

fun DocumentSnapshot.toCollaborativeGoal(): CollaborativeGoal? {
    if (!exists()) return null
    val name = getString("name") ?: return null
    val createdBy = getString("createdBy") ?: return null
    val totalBudget = getNumber("totalBudget")?.toDouble() ?: return null
    val currentSaved = getNumber("currentSaved")?.toDouble() ?: 0.0
    val participants = get("participants") as? List<*>
    val participantsList = participants?.filterIsInstance<String>() ?: emptyList()
    val categoriesData = get("categories") as? List<*>
    val categoriesList = categoriesData?.mapNotNull { it.toGoalCategory() } ?: emptyList()
    val statusValue = getString("status") ?: GoalStatus.ACTIVE.name
    
    return CollaborativeGoal(
        id = id,
        name = name,
        description = getString("description") ?: "",
        createdBy = createdBy,
        participants = participantsList,
        totalBudget = totalBudget,
        currentSaved = currentSaved,
        categories = categoriesList,
        deadline = getString("deadline"),
        createdAt = getTimestamp("createdAt").toEpochMilli(),
        updatedAt = getTimestamp("updatedAt").toEpochMilli(),
        status = runCatching { GoalStatus.valueOf(statusValue) }.getOrDefault(GoalStatus.ACTIVE)
    )
}

fun Any?.toGoalCategory(): GoalCategory? {
    if (this !is Map<*, *>) return null
    val id = get("id") as? String ?: return null
    val name = get("name") as? String ?: return null
    val budget = (get("budget") as? Number)?.toDouble() ?: return null
    val saved = (get("saved") as? Number)?.toDouble() ?: 0.0
    val color = get("color") as? String ?: "#4CAF50"
    
    return GoalCategory(
        id = id,
        name = name,
        budget = budget,
        saved = saved,
        color = color
    )
}

fun DocumentSnapshot.toGoalContribution(): GoalContribution? {
    if (!exists()) return null
    val goalId = getString("goalId") ?: return null
    val categoryId = getString("categoryId") ?: return null
    val uid = getString("uid") ?: return null
    val amount = getNumber("amount")?.toDouble() ?: return null
    val date = getString("date") ?: return null
    
    return GoalContribution(
        id = id,
        goalId = goalId,
        categoryId = categoryId,
        uid = uid,
        amount = amount,
        date = date,
        note = getString("note") ?: "",
        createdAt = getTimestamp("createdAt").toEpochMilli()
    )
}

fun CollaborativeGoal.toFirestorePayload(isNew: Boolean): Map<String, Any?> =
    hashMapOf<String, Any?>(
        "name" to name,
        "description" to description,
        "createdBy" to createdBy,
        "participants" to participants,
        "totalBudget" to totalBudget,
        "currentSaved" to currentSaved,
        "categories" to categories.map { it.toFirestorePayload(isNew = false) },
        "deadline" to deadline,
        "status" to status.name,
        "updatedAt" to FieldValue.serverTimestamp()
    ).apply {
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
    }

fun GoalCategory.toFirestorePayload(isNew: Boolean): Map<String, Any?> =
    hashMapOf<String, Any?>(
        "id" to id,
        "name" to name,
        "budget" to budget,
        "saved" to saved,
        "color" to color
    ).apply {
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
    }

fun GoalContribution.toFirestorePayload(isNew: Boolean): Map<String, Any?> =
    hashMapOf<String, Any?>(
        "goalId" to goalId,
        "categoryId" to categoryId,
        "uid" to uid,
        "amount" to amount,
        "date" to date,
        "note" to note
    ).apply {
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
    }

private fun DocumentSnapshot.getNumber(key: String): Number? = when (val value = get(key)) {
    is Number -> value
    else -> null
}

private fun Timestamp?.toEpochMilli(): Long = this?.toDate()?.time ?: System.currentTimeMillis()