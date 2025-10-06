package com.example.auto_saver

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/**
 * Sign Up Activity - Allows new users to create an account
 * After successful registration, navigates back to Login screen
 */
class SignUpActivity : AppCompatActivity() {

    private lateinit var editFullName: EditText
    private lateinit var editContact: EditText
    private lateinit var editLoginId: EditText
    private lateinit var editPassword: EditText
    private lateinit var editConfirmPassword: EditText
    private lateinit var btnSignUp: Button
    private lateinit var btnBackToLogin: Button

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        db = AppDatabase.getDatabase(this)

        editFullName = findViewById(R.id.editFullName)
        editContact = findViewById(R.id.editContact)
        editLoginId = findViewById(R.id.editLoginId)
        editPassword = findViewById(R.id.editPassword)
        editConfirmPassword = findViewById(R.id.editConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)

        btnSignUp.setOnClickListener {
            val fullName = editFullName.text.toString().trim()
            val contact = editContact.text.toString().trim()
            val loginId = editLoginId.text.toString().trim()
            val password = editPassword.text.toString().trim()
            val confirmPassword = editConfirmPassword.text.toString().trim()

            when {
                fullName.isEmpty() -> {
                    editFullName.error = "Full name is required"
                    editFullName.requestFocus()
                }
                contact.isEmpty() -> {
                    editContact.error = "Contact is required"
                    editContact.requestFocus()
                }
                loginId.isEmpty() -> {
                    editLoginId.error = "Username is required"
                    editLoginId.requestFocus()
                }
                password.isEmpty() -> {
                    editPassword.error = "Password is required"
                    editPassword.requestFocus()
                }
                password.length < 4 -> {
                    editPassword.error = "Password must be at least 4 characters"
                    editPassword.requestFocus()
                }
                confirmPassword.isEmpty() -> {
                    editConfirmPassword.error = "Please confirm password"
                    editConfirmPassword.requestFocus()
                }
                password != confirmPassword -> {
                    editConfirmPassword.error = "Passwords do not match"
                    editConfirmPassword.requestFocus()
                }
                else -> {
                    createAccount(fullName, contact, loginId, password)
                }
            }
        }

        btnBackToLogin.setOnClickListener {
            finish() // Go back to login screen
        }
    }

    private fun createAccount(fullName: String, contact: String, loginId: String, password: String) {
        lifecycleScope.launch {
            // Check if username already exists
            val existingUser = db.userDao().getUserByLoginId(loginId)

            if (existingUser != null) {
                runOnUiThread {
                    editLoginId.error = "Username already taken"
                    editLoginId.requestFocus()
                    Toast.makeText(this@SignUpActivity, "Username already exists. Please choose another.", Toast.LENGTH_LONG).show()
                }
                return@launch
            }

            // Create new user
            val user = User(
                fullName = fullName,
                contact = contact,
                loginId = loginId,
                password = password
            )

            val userId = db.userDao().insert(user).toInt()

            // Create default categories for the new user
            createDefaultCategories(userId)

            runOnUiThread {
                Toast.makeText(this@SignUpActivity, "Account created successfully! Please log in.", Toast.LENGTH_LONG).show()

                // Navigate back to login
                finish()
            }
        }
    }

    private suspend fun createDefaultCategories(userId: Int) {
        val defaultCategories = listOf(
            Category(userId = userId, name = "Bills"),
            Category(userId = userId, name = "Food"),
            Category(userId = userId, name = "Pets")
        )

        defaultCategories.forEach { category ->
            db.categoryDao().insertCategory(category)
        }
    }
}
