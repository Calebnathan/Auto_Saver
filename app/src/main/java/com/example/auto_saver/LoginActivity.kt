package com.example.auto_saver

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = AppDatabase.getDatabase(this)
        userDao = db.userDao()
        userPreferences = UserPreferences(this)

        // Check if user is already logged in
        if (userPreferences.isLoggedIn()) {
            navigateToMain()
            return
        }

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        btnLogin.setOnClickListener {
            val loginId = findViewById<EditText>(R.id.etLoginId).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()
            attemptLogin(loginId, password)
        }

        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun attemptLogin(loginId: String, password: String) {
        if (loginId.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Enter credentials", Toast.LENGTH_SHORT).show()
            return
        }

        // Using coroutine for background thread:
        lifecycleScope.launch {
            val user = userDao.getUserByLoginId(loginId)
            if (user != null && user.password == password) {
                // Save user session - Use fullName instead of username
                userPreferences.setCurrentUserId(user.id)
                userPreferences.setUserName(user.fullName ?: loginId)

                Toast.makeText(this@LoginActivity, "Welcome back, ${user.fullName}!", Toast.LENGTH_SHORT).show()

                // Navigate to main screen
                navigateToMain()
            } else {
                Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}