package com.example.auto_saver

import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.auto_saver.data.firestore.UserRemoteDataSource
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var ivProfilePhoto: ImageView
    private lateinit var btnTakePhoto: MaterialButton
    private lateinit var btnChoosePhoto: MaterialButton
    private lateinit var btnRemovePhoto: MaterialButton
    private lateinit var etFullName: TextInputEditText
    private lateinit var etContact: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnSave: MaterialButton

    private lateinit var userPrefs: UserPreferences
    private val userRemoteDataSource: UserRemoteDataSource by lazy { MyApplication.userRemoteDataSource }
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private var photoUri: Uri? = null
    private var tempPhotoUri: Uri? = null
    private var currentFullName: String = ""
    private var currentContact: String = ""
    private var currentProfilePhotoPath: String? = null

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
        setContentView(R.layout.activity_profile)

        userPrefs = MyApplication.userPreferences

        initializeViews()
        setupToolbar()
        loadUserData()
        setupPhotoButtons()
        setupSaveButton()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        ivProfilePhoto = findViewById(R.id.iv_profile_photo)
        btnTakePhoto = findViewById(R.id.btn_take_photo)
        btnChoosePhoto = findViewById(R.id.btn_choose_photo)
        btnRemovePhoto = findViewById(R.id.btn_remove_photo)
        etFullName = findViewById(R.id.et_full_name)
        etContact = findViewById(R.id.et_contact)
        etPassword = findViewById(R.id.et_password)
        btnSave = findViewById(R.id.btn_save)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val uid = try {
                userPrefs.requireUserUid()
            } catch (e: IllegalStateException) {
                Toast.makeText(this@ProfileActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            try {
                val result = userRemoteDataSource.fetchUser(uid)
                when (result) {
                    is com.example.auto_saver.data.firestore.FirestoreResult.Success -> {
                        val userProfile = result.data
                        currentFullName = userProfile.fullName
                        currentContact = userProfile.contact
                        currentProfilePhotoPath = userProfile.profilePhotoPath

                        etFullName.setText(userProfile.fullName)
                        etContact.setText(userProfile.contact)

                        // Load profile photo if exists
                        userProfile.profilePhotoPath?.let { path ->
                            val file = File(path)
                            if (file.exists()) {
                                val uri = FileProvider.getUriForFile(
                                    this@ProfileActivity,
                                    "${packageName}.fileprovider",
                                    file
                                )
                                ivProfilePhoto.setImageURI(uri)
                                ivProfilePhoto.imageTintList = null
                            }
                        }
                    }
                    is com.example.auto_saver.data.firestore.FirestoreResult.Error -> {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Error loading profile: ${result.throwable.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun setupPhotoButtons() {
        btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        btnChoosePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        btnRemovePhoto.setOnClickListener {
            removePhoto()
        }
    }

    private fun takePhoto() {
        val photoFile = File(filesDir, "profile_${System.currentTimeMillis()}.jpg")
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
    }

    private fun removePhoto() {
        photoUri = null
        ivProfilePhoto.setImageResource(R.drawable.ic_profile_icon)
        ivProfilePhoto.imageTintList = getColorStateList(R.color.red_primary)
        Toast.makeText(this, "Photo will be removed on save", Toast.LENGTH_SHORT).show()
    }

    private fun setupSaveButton() {
        btnSave.setOnClickListener {
            saveProfile()
        }
    }

    private fun saveProfile() {
        val fullName = etFullName.text.toString().trim()
        val contact = etContact.text.toString().trim()
        val newPassword = etPassword.text.toString().trim()

        if (fullName.isEmpty()) {
            etFullName.error = "Full name is required"
            return
        }

        if (contact.isEmpty()) {
            etContact.error = "Contact is required"
            return
        }

        lifecycleScope.launch {
            val uid = try {
                userPrefs.requireUserUid()
            } catch (e: IllegalStateException) {
                Toast.makeText(this@ProfileActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                return@launch
            }

            try {
                // Save photo if changed
                val photoPath = photoUri?.let { uri ->
                    val photoFile = File(filesDir, "profile_${uid}_${System.currentTimeMillis()}.jpg")
                    contentResolver.openInputStream(uri)?.use { input ->
                        photoFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    photoFile.absolutePath
                } ?: if (photoUri == null && currentProfilePhotoPath != null) {
                    currentProfilePhotoPath // Keep existing photo
                } else {
                    null // Photo was removed or never existed
                }

                // Update user profile in Firestore
                val updatedProfile = com.example.auto_saver.data.model.UserProfile(
                    uid = uid,
                    fullName = fullName,
                    contact = contact,
                    profilePhotoPath = photoPath,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                val result = userRemoteDataSource.createOrUpdateUser(updatedProfile, isNew = false)

                when (result) {
                    is com.example.auto_saver.data.firestore.FirestoreResult.Success -> {
                        // Update password in Firebase Auth if changed
                        if (newPassword.isNotEmpty()) {
                            firebaseAuth.currentUser?.updatePassword(newPassword)
                                ?.addOnSuccessListener {
                                    Toast.makeText(
                                        this@ProfileActivity,
                                        "Profile and password updated successfully",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                }
                                ?.addOnFailureListener { error ->
                                    Toast.makeText(
                                        this@ProfileActivity,
                                        "Profile updated but password change failed: ${error.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    finish()
                                }
                        } else {
                            Toast.makeText(
                                this@ProfileActivity,
                                "Profile updated successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }
                    is com.example.auto_saver.data.firestore.FirestoreResult.Error -> {
                        Toast.makeText(
                            this@ProfileActivity,
                            "Failed to update profile: ${result.throwable.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@ProfileActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
