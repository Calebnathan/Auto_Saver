package com.example.auto_saver

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.widget.doAfterTextChanged
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.auto_saver.data.firestore.FirestoreResult
import com.example.auto_saver.data.firestore.FirestoreUserRemoteDataSource
import com.example.auto_saver.data.firestore.UserRemoteDataSource
import com.example.auto_saver.data.model.UserProfile
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.android.material.progressindicator.LinearProgressIndicator
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

    private lateinit var layoutFullName: TextInputLayout
    private lateinit var layoutContact: TextInputLayout
    private lateinit var layoutEmail: TextInputLayout
    private lateinit var layoutPassword: TextInputLayout
    private lateinit var layoutConfirmPassword: TextInputLayout
    private lateinit var passwordStrengthIndicator: LinearProgressIndicator
    private lateinit var tvPasswordStrength: TextView

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
        setupValidationWatchers()
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
        layoutFullName = findViewById(R.id.layoutFullName)
        layoutContact = findViewById(R.id.layoutContact)
        layoutEmail = findViewById(R.id.layoutEmail)
        layoutPassword = findViewById(R.id.layoutPassword)
        layoutConfirmPassword = findViewById(R.id.layoutConfirmPassword)
        passwordStrengthIndicator = findViewById(R.id.passwordStrengthIndicator)
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength)
        btnSignUp.isEnabled = false
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

    private fun setupValidationWatchers() {
        editFullName.doAfterTextChanged {
            validateFullName()
            updateSignUpButtonState()
        }
        editContact.doAfterTextChanged {
            validateContact()
            updateSignUpButtonState()
        }
        editEmail.doAfterTextChanged {
            validateEmailInput()
            updateSignUpButtonState()
        }
        editPassword.doAfterTextChanged {
            validatePassword()
            validateConfirmPassword()
            updateSignUpButtonState()
        }
        editConfirmPassword.doAfterTextChanged {
            validateConfirmPassword()
            updateSignUpButtonState()
        }
        updatePasswordStrengthUI("")
    }

    private fun signUp() {
        val fullName = editFullName.text.toString().trim()
        val contact = editContact.text.toString().trim()
        val email = editEmail.text.toString().trim()
        val password = editPassword.text.toString().trim()
        val confirmPassword = editConfirmPassword.text.toString().trim()

        val isValid = listOf(
            validateFullName(),
            validateContact(),
            validateEmailInput(),
            validatePassword(),
            validateConfirmPassword()
        ).all { it }

        if (!isValid) {
            Toast.makeText(this, "Please resolve the highlighted fields.", Toast.LENGTH_SHORT).show()
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
        if (isLoading) {
            btnSignUp.isEnabled = false
        } else {
            updateSignUpButtonState()
        }
    }

    private fun validateFullName(): Boolean {
        val fullName = editFullName.text?.toString()?.trim().orEmpty()
        return when {
            fullName.isEmpty() -> {
                layoutFullName.showError("Full name is required")
                false
            }
            fullName.split("\\s+".toRegex()).size < 2 -> {
                layoutFullName.showError("Include at least first and last name")
                false
            }
            else -> {
                layoutFullName.showSuccess("Hi ${fullName.substringBefore(" ")} 👋")
                true
            }
        }
    }

    private fun validateContact(): Boolean {
        val contact = editContact.text?.toString()?.trim().orEmpty()
        val normalized = contact.replace("[^0-9+]".toRegex(), "")
        return when {
            normalized.isEmpty() -> {
                layoutContact.showError("Contact number is required")
                false
            }
            !normalized.matches(Regex("^\\+?\\d{7,15}$")) -> {
                layoutContact.showError("Use 7-15 digits (country code optional)")
                false
            }
            else -> {
                layoutContact.showSuccess("We'll only use this for security alerts")
                true
            }
        }
    }

    private fun validateEmailInput(): Boolean {
        val email = editEmail.text?.toString()?.trim().orEmpty()
        return when {
            email.isEmpty() -> {
                layoutEmail.showError("Email is required")
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                layoutEmail.showError("Please enter a valid email")
                false
            }
            else -> {
                layoutEmail.showSuccess("Looks great!")
                true
            }
        }
    }

    private fun validatePassword(): Boolean {
        val password = editPassword.text?.toString().orEmpty()
        updatePasswordStrengthUI(password)

        if (password.isEmpty()) {
            layoutPassword.showError("Password is required")
            return false
        }

        val score = passwordScore(password)
        return if (score < 3) {
            layoutPassword.showError("Mix letters, numbers & symbols for strength")
            false
        } else {
            layoutPassword.showSuccess("Password strength is solid")
            true
        }
    }

    private fun validateConfirmPassword(): Boolean {
        val password = editPassword.text?.toString().orEmpty()
        val confirm = editConfirmPassword.text?.toString().orEmpty()
        return when {
            confirm.isEmpty() -> {
                layoutConfirmPassword.showError("Please confirm your password")
                false
            }
            confirm != password -> {
                layoutConfirmPassword.showError("Passwords do not match")
                false
            }
            else -> {
                layoutConfirmPassword.showSuccess("Passwords match ✔")
                true
            }
        }
    }

    private fun updatePasswordStrengthUI(password: String) {
        val score = passwordScore(password)
        val strength = when (score) {
            0 -> PasswordStrengthInfo("Add a password", R.color.password_strength_base, 0)
            1 -> PasswordStrengthInfo("Too weak", R.color.password_strength_weak, 25)
            2 -> PasswordStrengthInfo("Getting there", R.color.password_strength_medium, 50)
            3 -> PasswordStrengthInfo("Strong", R.color.password_strength_good, 75)
            else -> PasswordStrengthInfo("Vault grade", R.color.password_strength_strong, 100)
        }
        val color = ContextCompat.getColor(this, strength.colorRes)
        passwordStrengthIndicator.setIndicatorColor(color)
        passwordStrengthIndicator.progress = strength.progress
        tvPasswordStrength.text = strength.label
        tvPasswordStrength.setTextColor(color)
    }

    private fun passwordScore(password: String): Int {
        if (password.isEmpty()) return 0
        var score = 0
        if (password.length >= 8) score++
        if (password.any(Char::isLowerCase) && password.any(Char::isUpperCase)) score++
        if (password.any(Char::isDigit)) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        return score
    }

    private fun TextInputLayout.showError(message: String) {
        error = message
        helperText = null
    }

    private fun TextInputLayout.showSuccess(message: String) {
        error = null
        helperText = message
    }

    private fun updateSignUpButtonState() {
        btnSignUp.isEnabled = editFullName.text?.isNotBlank() == true &&
            editContact.text?.isNotBlank() == true &&
            editEmail.text?.isNotBlank() == true &&
            editPassword.text?.isNotBlank() == true &&
            editConfirmPassword.text?.isNotBlank() == true
    }

    private data class PasswordStrengthInfo(
        val label: String,
        val colorRes: Int,
        val progress: Int
    )
}
