package com.example.auto_saver.data.mappers

import com.example.auto_saver.Category
import com.example.auto_saver.Expense
import com.example.auto_saver.Goal
import com.example.auto_saver.data.model.CategoryRecord
import com.example.auto_saver.data.model.ExpenseRecord
import com.example.auto_saver.data.model.GoalRecord

fun Category.toCloudRecord(
    uid: String, 
    cloudId: String? = null,
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis()
): CategoryRecord {
    return CategoryRecord(
        id = cloudId ?: "",
        uid = uid,
        name = name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun CategoryRecord.toRoomEntity(legacyUserId: Int, roomId: Int = 0): Category {
    return Category(
        id = roomId,
        userId = legacyUserId,
        name = name
    )
}

fun Expense.toCloudRecord(
    uid: String, 
    cloudCategoryId: String, 
    cloudId: String? = null, 
    photoPath: String? = null,
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis()
): ExpenseRecord {
    return ExpenseRecord(
        id = cloudId ?: "",
        uid = uid,
        categoryId = cloudCategoryId,
        date = date,
        amount = amount,
        description = description,
        startTime = startTime,
        endTime = endTime,
        photoPath = photoPath ?: this.photoPath,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun ExpenseRecord.toRoomEntity(legacyUserId: Int, localCategoryId: Int, roomId: Int = 0): Expense {
    return Expense(
        id = roomId,
        userId = legacyUserId,
        categoryId = localCategoryId,
        date = date,
        amount = amount,
        description = description,
        startTime = startTime,
        endTime = endTime,
        photoPath = photoPath
    )
}

fun Goal.toCloudRecord(
    uid: String, 
    cloudId: String? = null,
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis()
): GoalRecord {
    return GoalRecord(
        id = cloudId ?: month,
        uid = uid,
        month = month,
        minGoal = minGoal,
        maxGoal = maxGoal,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun GoalRecord.toRoomEntity(legacyUserId: Int, roomId: Int = 0): Goal {
    return Goal(
        id = roomId,
        userId = legacyUserId,
        month = month,
        minGoal = minGoal,
        maxGoal = maxGoal
    )
}
