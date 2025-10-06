package com.example.auto_saver

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.io.File

class SignUpActivity : AppCompatActivity() {

    private lateinit var ivProfilePhoto: ImageView
    private lateinit var btnTakePhoto: MaterialButton
    private lateinit var btnChoosePhoto: MaterialButton
    private lateinit var editFullName: TextInputEditText
    private lateinit var editContact: TextInputEditText
    private lateinit var editLoginId: TextInputEditText
    private lateinit var editPassword: TextInputEditText
    private lateinit var editConfirmPassword: TextInputEditText
    private lateinit var btnSignUp: MaterialButton
    private lateinit var btnBackToLogin: MaterialButton

    private lateinit var database: AppDatabase

    private var photoUri: Uri? = null
    private var tempPhotoUri: Uri? = null

    // Activity result launchers
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoUri != null) {
            photoUri = tempPhotoUri
            displayPhoto(photoUri!!)
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            photoUri = it
            displayPhoto(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        database = AppDatabase.getDatabase(this)

        initializeViews()
        setupPhotoButtons()
        setupButtons()
    }

    private fun initializeViews() {
        ivProfilePhoto = findViewById(R.id.iv_profile_photo)
        btnTakePhoto = findViewById(R.id.btn_take_photo)
        btnChoosePhoto = findViewById(R.id.btn_choose_photo)
        editFullName = findViewById(R.id.editFullName)
        editContact = findViewById(R.id.editContact)
        editLoginId = findViewById(R.id.editLoginId)
        editPassword = findViewById(R.id.editPassword)
        editConfirmPassword = findViewById(R.id.editConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
    }

    private fun setupPhotoButtons() {
        btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        btnChoosePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun takePhoto() {
        val photoFile = File(filesDir, "signup_temp_${System.currentTimeMillis()}.jpg")
        tempPhotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        tempPhotoUri?.let { takePictureLauncher.launch(it) }
    }

    private fun displayPhoto(uri: Uri) {
        ivProfilePhoto.setImageURI(uri)
        ivProfilePhoto.imageTintList = null // Remove tint to show actual photo
        ivProfilePhoto.scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private fun setupButtons() {
        btnSignUp.setOnClickListener {
            signUp()
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun signUp() {
        val fullName = editFullName.text.toString().trim()
        val contact = editContact.text.toString().trim()
        val loginId = editLoginId.text.toString().trim()
        val password = editPassword.text.toString().trim()
        val confirmPassword = editConfirmPassword.text.toString().trim()

        // Validation
        if (fullName.isEmpty()) {
            editFullName.error = "Full name is required"
            editFullName.requestFocus()
            return
        }

        if (contact.isEmpty()) {
            editContact.error = "Contact is required"
            editContact.requestFocus()
            return
        }

        if (loginId.isEmpty()) {
            editLoginId.error = "Username is required"
            editLoginId.requestFocus()
            return
        }

        if (password.isEmpty()) {
            editPassword.error = "Password is required"
            editPassword.requestFocus()
            return
        }

        if (password.length < 4) {
            editPassword.error = "Password must be at least 4 characters"
            editPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            editConfirmPassword.error = "Passwords do not match"
            editConfirmPassword.requestFocus()
            return
        }

        lifecycleScope.launch {
            // Check if username already exists
            val existingUser = database.userDao().getUserByLoginId(loginId)
            if (existingUser != null) {
                runOnUiThread {
                    editLoginId.error = "Username already exists"
                    editLoginId.requestFocus()
                    Toast.makeText(
                        this@SignUpActivity,
                        "Username already exists. Please choose a different username.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }

            // Save photo if one was selected
            val photoPath = photoUri?.let { uri ->
                val photoFile = File(filesDir, "profile_new_${System.currentTimeMillis()}.jpg")
                contentResolver.openInputStream(uri)?.use { input ->
                    photoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                photoFile.absolutePath
            }

            // Create new user
            val newUser = User(
                loginId = loginId,
                password = password,
                fullName = fullName,
                contact = contact,
                profilePhotoPath = photoPath
            )

            database.userDao().insert(newUser)

            runOnUiThread {
                Toast.makeText(
                    this@SignUpActivity,
                    "Account created successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                // Navigate back to login
                val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(intent)
                finish()
            }
        }
    }
}

