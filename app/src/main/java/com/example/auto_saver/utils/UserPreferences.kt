package com.example.auto_saver.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Helper class to manage user preferences and session data
 */
class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    fun getCurrentUserId(): Int = prefs.getInt(KEY_CURRENT_USER_ID, -1)

    fun setCurrentUserId(userId: Int) {
        prefs.edit().putInt(KEY_CURRENT_USER_ID, userId).apply()
    }

    fun isDarkModeEnabled(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun clearUserData() {
        prefs.edit().clear().apply()
    }
}

