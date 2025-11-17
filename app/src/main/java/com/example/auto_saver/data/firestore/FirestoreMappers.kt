package com.example.auto_saver.data.firestore

import com.example.auto_saver.data.model.CategoryRecord
import com.example.auto_saver.data.model.ExpenseRecord
import com.example.auto_saver.data.model.GoalRecord
import com.example.auto_saver.data.model.UserProfile
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

fun DocumentSnapshot.toUserProfile(): UserProfile? {
    if (!exists()) return null
    val fullName = getString("fullName") ?: ""
    val contact = getString("contact") ?: ""
    val profilePhotoUrl = getString("profilePhotoUrl")
    return UserProfile(
        uid = id,
        fullName = fullName,
        contact = contact,
        profilePhotoUrl = profilePhotoUrl,
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
        photoUrl = getString("photoUrl"),
        createdAt = getTimestamp("createdAt").toEpochMilli(),
        updatedAt = getTimestamp("updatedAt").toEpochMilli()
    )
}

fun UserProfile.toFirestorePayload(isNew: Boolean): Map<String, Any?> =
    hashMapOf<String, Any?>(
        "fullName" to fullName,
        "contact" to contact,
        "profilePhotoUrl" to profilePhotoUrl,
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
        "photoUrl" to photoUrl,
        "updatedAt" to FieldValue.serverTimestamp()
    ).apply {
        if (isNew) put("createdAt", FieldValue.serverTimestamp())
    }

private fun DocumentSnapshot.getNumber(key: String): Number? = when (val value = get(key)) {
    is Number -> value
    else -> null
}

private fun Timestamp?.toEpochMilli(): Long = this?.toDate()?.time ?: 0L
