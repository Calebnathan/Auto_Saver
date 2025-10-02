package com.example.auto_saver

import android.content.Intent
import android.view.MenuItem
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val backButton: Button = findViewById(R.id.btn_back)

        backButton.setOnClickListener {
            // Go back to the previous activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Handle back button click
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed() // 🔥 Correct way in modern Android
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}