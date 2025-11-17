package com.example.auto_saver

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.auto_saver.data.firestore.*
import com.example.auto_saver.data.repository.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
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

        lateinit var friendRepository: UnifiedFriendRepository
            private set
            
        lateinit var userRemoteDataSource: UserRemoteDataSource
            private set

        private lateinit var categoryRemoteDataSource: CategoryRemoteDataSource
        private lateinit var expenseRemoteDataSource: ExpenseRemoteDataSource
        private lateinit var goalRemoteDataSource: GoalRemoteDataSource
        private lateinit var friendRemoteDataSource: FriendRemoteDataSource

        fun cleanupFirestoreListeners() {
            if (::categoryRemoteDataSource.isInitialized) {
                (categoryRemoteDataSource as? FirestoreCategoryRemoteDataSource)?.cleanup()
            }
            if (::expenseRemoteDataSource.isInitialized) {
                (expenseRemoteDataSource as? FirestoreExpenseRemoteDataSource)?.cleanup()
            }
            if (::goalRemoteDataSource.isInitialized) {
                (goalRemoteDataSource as? FirestoreGoalRemoteDataSource)?.cleanup()
            }
            if (::friendRemoteDataSource.isInitialized) {
                (friendRemoteDataSource as? FirestoreFriendRemoteDataSource)?.cleanup()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        
        // Enable Firestore offline persistence
        val firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = firestoreSettings
        
        // Initialize Room database once for the entire app
        database = AppDatabase.getDatabase(this)
        
        // Initialize user preferences
        userPreferences = UserPreferences(this, FirebaseAuth.getInstance())
        
        // Initialize remote data sources
        categoryRemoteDataSource = FirestoreCategoryRemoteDataSource()
        expenseRemoteDataSource = FirestoreExpenseRemoteDataSource()
        goalRemoteDataSource = FirestoreGoalRemoteDataSource()
        friendRemoteDataSource = FirestoreFriendRemoteDataSource()
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

        friendRepository = FirestoreFriendRepository(friendRemoteDataSource)

        // Apply saved theme preference on app start
        val nightMode = if (userPreferences.isDarkModeEnabled()) {
            AppCompatDelegate.MODE_NIGHT_YES
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
