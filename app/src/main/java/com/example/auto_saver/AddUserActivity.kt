package com.example.auto_saver

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class AddUserActivity : AppCompatActivity() {

    private lateinit var editFullName: EditText
    private lateinit var editContact: EditText
    private lateinit var editLoginId: EditText
    private lateinit var editPassword: EditText
    private lateinit var btnSaveUser: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_user)

        editFullName = findViewById(R.id.editFullName)
        editContact = findViewById(R.id.editContact)
        editLoginId = findViewById(R.id.editLoginId)
        editPassword = findViewById(R.id.editPassword)
        btnSaveUser = findViewById(R.id.btnSaveUser)

        btnSaveUser.setOnClickListener {
            val fullName = editFullName.text.toString().trim()
            val contact = editContact.text.toString().trim()
            val loginId = editLoginId.text.toString().trim()
            val password = editPassword.text.toString().trim()

            if (fullName.isNotEmpty() && contact.isNotEmpty() && loginId.isNotEmpty() && password.isNotEmpty()) {
                saveUser(fullName, contact, loginId, password)
            } else {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        val backButton: Button = findViewById(R.id.btn_back)

        backButton.setOnClickListener {
            // Go back to the previous activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun saveUser(fullName: String, contact: String, loginId: String, password: String) {
        val user = User(
            fullName = fullName,
            contact = contact,
            loginId = loginId,
            password = password
        )

        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(this@AddUserActivity)
            db.userDao().insert(user)

            runOnUiThread {
                Toast.makeText(this@AddUserActivity, "User saved successfully!", Toast.LENGTH_SHORT).show()
                clearFields()
            }
        }
    }

    private fun clearFields() {
        editFullName.text.clear()
        editContact.text.clear()
        editLoginId.text.clear()
        editPassword.text.clear()
    }
}