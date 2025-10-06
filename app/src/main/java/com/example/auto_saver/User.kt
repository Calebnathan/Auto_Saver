package com.example.auto_saver

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_table")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "login_id")
    val loginId: String,

    @ColumnInfo(name = "password")
    val password: String,

    @ColumnInfo(name = "full_name")
    val fullName: String? = null,

    @ColumnInfo(name = "contact")
    val contact: String? = null,

    @ColumnInfo(name = "profile_photo_path")
    val profilePhotoPath: String? = null
)