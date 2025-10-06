package com.example.auto_saver

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MyApplication : Application() {

    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize Room database once for the entire app
        database = AppDatabase.getDatabase(this)

        // Apply saved theme preference on app start
        val userPrefs = UserPreferences(this)
        val nightMode = if (userPrefs.isDarkModeEnabled()) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}