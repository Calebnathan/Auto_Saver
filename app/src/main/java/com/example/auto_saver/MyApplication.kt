package com.example.auto_saver

import android.app.Application

class MyApplication : Application() {

    companion object {
        lateinit var database: AppDatabase
            private set
    }

    override fun onCreate() {
        super.onCreate()
        // Initialize Room database once for the entire app
        database = AppDatabase.getDatabase(this)
    }
}