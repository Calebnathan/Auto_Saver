package com.example.auto_saver

import android.content.Intent
import android.view.MenuItem
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = AppDatabase.getDatabase(this)
        userDao = db.userDao()

        val btnLogin = findViewById<Button>(R.id.btnLogin)
        btnLogin.setOnClickListener {
            val loginId = findViewById<EditText>(R.id.etLoginId).text.toString()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()
            attemptLogin(loginId, password)
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
                // success
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                finish()
            } else {
                Toast.makeText(this@LoginActivity, "Invalid credentials", Toast.LENGTH_SHORT).show()
            }
        }
    }
}