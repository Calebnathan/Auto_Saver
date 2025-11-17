package com.example.auto_saver

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.auto_saver.ui.home.HomeFragment
import com.example.auto_saver.ui.race.RaceFragment
import com.example.auto_saver.ui.social.SocialFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var ivProfileIcon: ImageView
    private lateinit var btnSettings: ImageView
    
    private lateinit var database: AppDatabase
    private lateinit var userPrefs: UserPreferences
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        database = MyApplication.database
        userPrefs = MyApplication.userPreferences

        // Handle window insets for edge-to-edge display
        setupWindowInsets()
        
        initializeViews()
        setupBottomNavigation()
        setupToolbarActions()
        
        // Load initial fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private fun setupWindowInsets() {
        val mainView = findViewById<androidx.constraintlayout.widget.ConstraintLayout>(R.id.main_view)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply padding to prevent content from being hidden by system bars
            view.setPadding(
                insets.left,
                insets.top,
                insets.right,
                insets.bottom
            )
            
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun initializeViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation)
        ivProfileIcon = findViewById(R.id.iv_profile_icon)
        btnSettings = findViewById(R.id.btn_settings)
        
        loadUserProfile()
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_race -> {
                    loadFragment(RaceFragment())
                    true
                }
                R.id.nav_social -> {
                    loadFragment(SocialFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun setupToolbarActions() {
        // Profile picture click - navigate to ProfileActivity
        ivProfileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Settings button click - show menu
        btnSettings.setOnClickListener { view ->
            showSettingsMenu(view as ImageView)
        }
    }

    private fun showSettingsMenu(anchor: ImageView) {
        val popup = PopupMenu(this, anchor)
        popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_theme_toggle -> {
                    toggleTheme()
                    true
                }
                R.id.action_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.action_profile -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.action_reset -> {
                    showResetDatabaseDialog()
                    true
                }
                R.id.action_logout -> {
                    performLogout()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun loadUserProfile() {
        val userId = userPrefs.getCurrentUserId()
        if (userId != -1) {
            lifecycleScope.launch {
                val user = database.userDao().getUserById(userId)
                user?.let {
                    if (it.profilePhotoPath != null) {
                        val file = File(it.profilePhotoPath)
                        if (file.exists()) {
                            val uri: Uri = FileProvider.getUriForFile(
                                this@MainActivity,
                                "${packageName}.fileprovider",
                                file
                            )
                            ivProfileIcon.setImageURI(uri)
                            ivProfileIcon.imageTintList = null
                            ivProfileIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                        } else {
                            setDefaultProfileIcon()
                        }
                    } else {
                        setDefaultProfileIcon()
                    }
                }
            }
        } else {
            setDefaultProfileIcon()
        }
    }

    private fun setDefaultProfileIcon() {
        ivProfileIcon.setImageResource(R.drawable.ic_profile_icon)
        ivProfileIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
    }

    private fun toggleTheme() {
        val isDarkMode = userPrefs.isDarkModeEnabled()
        userPrefs.setDarkMode(!isDarkMode)

        AppCompatDelegate.setDefaultNightMode(
            if (!isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun showResetDatabaseDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset Database")
            .setMessage("Are you sure you want to reset the database? This will delete ALL data including all users, expenses, categories, and goals. This cannot be undone!")
            .setPositiveButton("Reset") { _, _ ->
                resetDatabase()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun resetDatabase() {
        lifecycleScope.launch {
            try {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Resetting database...",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }

                firebaseAuth.signOut()
                userPrefs.clearSession()

                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Database reset successfully",
                        Toast.LENGTH_LONG
                    ).show()

                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Error resetting database: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun performLogout() {
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()
        
        // Clear repository caches and cleanup Firestore listeners
        lifecycleScope.launch(Dispatchers.IO) {
            // Clear repository caches
            (MyApplication.categoryRepository as? com.example.auto_saver.data.repository.FirestoreFirstCategoryRepository)?.clearCache()
            (MyApplication.expenseRepository as? com.example.auto_saver.data.repository.FirestoreFirstExpenseRepository)?.clearCache()
            (MyApplication.goalRepository as? com.example.auto_saver.data.repository.FirestoreFirstGoalRepository)?.clearCache()
            
            // Cleanup Firestore listeners to prevent memory leaks
            MyApplication.cleanupFirestoreListeners()
        }
        
        // Sign out from Firebase
        firebaseAuth.signOut()
        
        // Clear user preferences
        userPrefs.clearSession()
        
        // Navigate to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
