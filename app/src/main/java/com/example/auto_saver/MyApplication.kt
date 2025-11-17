package com.example.auto_saver

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.auto_saver.data.firestore.*
import com.example.auto_saver.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers

class MyApplication : Application() {

    companion object {
        lateinit var database: AppDatabase
            private set
        
        lateinit var userPreferences: UserPreferences
            private set
            
        lateinit var categoryRepository: UnifiedCategoryRepository
            private set
            
        lateinit var expenseRepository: UnifiedExpenseRepository
            private set
            
        lateinit var goalRepository: UnifiedGoalRepository
            private set
            
        lateinit var userRemoteDataSource: UserRemoteDataSource
            private set
    }

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Room database once for the entire app
        database = AppDatabase.getDatabase(this)
        
        // Initialize user preferences
        userPreferences = UserPreferences(this, FirebaseAuth.getInstance())
        
        // Initialize remote data sources
        val categoryRemoteDataSource: CategoryRemoteDataSource = FirestoreCategoryRemoteDataSource()
        val expenseRemoteDataSource: ExpenseRemoteDataSource = FirestoreExpenseRemoteDataSource()
        val goalRemoteDataSource: GoalRemoteDataSource = FirestoreGoalRemoteDataSource()
        userRemoteDataSource = FirestoreUserRemoteDataSource()
        
        // Initialize unified repositories
        categoryRepository = FirestoreFirstCategoryRepository(
            remoteDataSource = categoryRemoteDataSource,
            categoryDao = database.categoryDao(),
            userPreferences = userPreferences,
            dispatcher = Dispatchers.IO
        )
        
        goalRepository = FirestoreFirstGoalRepository(
            remoteDataSource = goalRemoteDataSource,
            goalDao = database.goalDao(),
            userPreferences = userPreferences,
            dispatcher = Dispatchers.IO
        )
        
        expenseRepository = FirestoreFirstExpenseRepository(
            context = this,
            remoteDataSource = expenseRemoteDataSource,
            expenseDao = database.expenseDao(),
            userPreferences = userPreferences,
            categoryRepository = categoryRepository as FirestoreFirstCategoryRepository,
            dispatcher = Dispatchers.IO
        )

        // Apply saved theme preference on app start
        val nightMode = if (userPreferences.isDarkModeEnabled()) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
