package com.example.auto_saver

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.auto_saver.data.firestore.FirestoreResult
import com.example.auto_saver.data.firestore.FirestoreUserRemoteDataSource
import com.example.auto_saver.data.firestore.UserRemoteDataSource
import com.example.auto_saver.data.model.UserProfile
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File

class SignUpActivity : AppCompatActivity() {

    private lateinit var ivProfilePhoto: ImageView
    private lateinit var btnTakePhoto: MaterialButton
    private lateinit var btnChoosePhoto: MaterialButton
    private lateinit var editFullName: TextInputEditText
    private lateinit var editContact: TextInputEditText
    private lateinit var editEmail: TextInputEditText
    private lateinit var editPassword: TextInputEditText
    private lateinit var editConfirmPassword: TextInputEditText
    private lateinit var btnSignUp: MaterialButton
    private lateinit var btnBackToLogin: MaterialButton

    private lateinit var userPreferences: UserPreferences
    private lateinit var database: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var auth: FirebaseAuth
    private val userRemoteDataSource: UserRemoteDataSource = FirestoreUserRemoteDataSource()

    private var photoUri: Uri? = null
    private var tempPhotoUri: Uri? = null

    private val takePictureLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && tempPhotoUri != null) {
                photoUri = tempPhotoUri
                displayPhoto(photoUri!!)
            }
        }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                photoUri = it
                displayPhoto(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        auth = FirebaseAuth.getInstance()
        userPreferences = UserPreferences(this)
        database = AppDatabase.getDatabase(this)
        userDao = database.userDao()

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
        editEmail = findViewById(R.id.editLoginId)
        editPassword = findViewById(R.id.editPassword)
        editConfirmPassword = findViewById(R.id.editConfirmPassword)
        btnSignUp = findViewById(R.id.btnSignUp)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)
    }

    private fun setupPhotoButtons() {
        btnTakePhoto.setOnClickListener { takePhoto() }
        btnChoosePhoto.setOnClickListener { pickImageLauncher.launch("image/*") }
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
        ivProfilePhoto.imageTintList = null
        ivProfilePhoto.scaleType = ImageView.ScaleType.CENTER_CROP
    }

    private fun setupButtons() {
        btnSignUp.setOnClickListener { signUp() }
        btnBackToLogin.setOnClickListener { finish() }
    }

    private fun signUp() {
        val fullName = editFullName.text.toString().trim()
        val contact = editContact.text.toString().trim()
        val email = editEmail.text.toString().trim()
        val password = editPassword.text.toString().trim()
        val confirmPassword = editConfirmPassword.text.toString().trim()

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

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editEmail.error = "Valid email is required"
            editEmail.requestFocus()
            return
        }

        if (password.isEmpty()) {
            editPassword.error = "Password is required"
            editPassword.requestFocus()
            return
        }

        if (password.length < 6) {
            editPassword.error = "Password must be at least 6 characters"
            editPassword.requestFocus()
            return
        }

        if (password != confirmPassword) {
            editConfirmPassword.error = "Passwords do not match"
            editConfirmPassword.requestFocus()
            return
        }

        lifecycleScope.launch {
            setLoading(true)

            val result = runCatching {
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: throw IllegalStateException("Unable to create user")

                val photoUrl = photoUri?.let { uploadProfilePhoto(uid, it) }
                val profile = UserProfile(
                    uid = uid,
                    fullName = fullName,
                    contact = contact,
                    profilePhotoUrl = photoUrl
                )

                val localUserId = when (val createResult = userRemoteDataSource.createOrUpdateUser(profile, true)) {
                    is FirestoreResult.Success -> cacheLocalUser(email, createResult.data)
                    is FirestoreResult.Error -> throw createResult.throwable
                }
                cacheSession(uid, profile.fullName, localUserId)
            }

            setLoading(false)

            result.onSuccess {
                Toast.makeText(this@SignUpActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this@SignUpActivity, MainActivity::class.java))
                finish()
            }.onFailure { error ->
                Toast.makeText(
                    this@SignUpActivity,
                    error.localizedMessage ?: "Sign up failed",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun uploadProfilePhoto(uid: String, uri: Uri): String? {
        return when (val result = userRemoteDataSource.uploadProfilePhoto(uid, uri)) {
            is FirestoreResult.Success -> result.data
            is FirestoreResult.Error -> {
                Toast.makeText(this, "Profile photo upload failed, continuing without photo.", Toast.LENGTH_SHORT).show()
                null
            }
        }
    }

    private suspend fun cacheLocalUser(email: String, profile: UserProfile): Int {
        val existingUser = userDao.getUserByLoginId(email)
        return if (existingUser == null) {
            userDao.insert(
                User(
                    loginId = email,
                    password = "",
                    fullName = profile.fullName,
                    contact = profile.contact,
                    profilePhotoPath = profile.profilePhotoUrl
                )
            ).toInt()
        } else {
            userDao.update(
                existingUser.copy(
                    fullName = profile.fullName,
                    contact = profile.contact,
                    profilePhotoPath = profile.profilePhotoUrl ?: existingUser.profilePhotoPath
                )
            )
            existingUser.id
        }
    }

    private fun cacheSession(uid: String, fullName: String, legacyUserId: Int) {
        userPreferences.setCurrentUserUid(uid)
        userPreferences.setCurrentUserId(legacyUserId)
        userPreferences.setUserName(fullName)
    }

    private fun setLoading(isLoading: Boolean) {
        btnSignUp.isEnabled = !isLoading
    }
}
