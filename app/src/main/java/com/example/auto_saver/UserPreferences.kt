package com.example.auto_saver

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages user preferences including theme settings and current user session
 */
class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_CURRENT_USER_ID = "current_user_id"
        private const val KEY_USER_NAME = "user_name"
    }

    fun isDarkModeEnabled(): Boolean =
        prefs.getBoolean(KEY_DARK_MODE, false)

    fun setDarkMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply()
    }

    fun getCurrentUserId(): Int =
        prefs.getInt(KEY_CURRENT_USER_ID, -1)

    fun setCurrentUserId(userId: Int) {
        prefs.edit().putInt(KEY_CURRENT_USER_ID, userId).apply()
    }

    fun getUserName(): String? =
        prefs.getString(KEY_USER_NAME, null)

    fun setUserName(name: String) {
        prefs.edit().putString(KEY_USER_NAME, name).apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(KEY_CURRENT_USER_ID)
            .remove(KEY_USER_NAME)
            .apply()
    }

    fun isLoggedIn(): Boolean = getCurrentUserId() != -1
}

