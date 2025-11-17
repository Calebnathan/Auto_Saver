package com.example.auto_saver

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.example.auto_saver.data.firestore.FirestoreResult
import com.example.auto_saver.data.firestore.FirestoreUserRemoteDataSource
import com.example.auto_saver.data.firestore.UserRemoteDataSource
import com.example.auto_saver.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LoginActivity : AppCompatActivity() {
    private lateinit var db: AppDatabase
    private lateinit var userDao: UserDao
    private lateinit var auth: FirebaseAuth
    private lateinit var userPreferences: UserPreferences
    private val userRemoteDataSource: UserRemoteDataSource = FirestoreUserRemoteDataSource()

    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        userPreferences = UserPreferences(this)
        auth = FirebaseAuth.getInstance()

        if (userPreferences.isLoggedIn()) {
            navigateToMain()
            return
        }

        setContentView(R.layout.activity_login)

        btnLogin = findViewById(R.id.btnLogin)
        db = AppDatabase.getDatabase(this)
        userDao = db.userDao()
        val btnSignUp = findViewById<Button>(R.id.btnSignUp)

        btnLogin.setOnClickListener {
            val email = findViewById<EditText>(R.id.etLoginId).text.toString().trim()
            val password = findViewById<EditText>(R.id.etPassword).text.toString()
            attemptLogin(email, password)
        }

        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }

    private fun attemptLogin(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(this, "Enter credentials", Toast.LENGTH_SHORT).show()
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            setLoading(true)
            val loginResult = runCatching {
                val authResult = auth.signInWithEmailAndPassword(email, password).await()
                val uid = authResult.user?.uid ?: throw IllegalStateException("Missing user id from Firebase")
                val userProfile: UserProfile = when (val profileResult = userRemoteDataSource.fetchUser(uid)) {
                    is FirestoreResult.Success -> profileResult.data
                    is FirestoreResult.Error -> {
                        val cause = profileResult.throwable
                        if (cause is NoSuchElementException) {
                            val fallback = UserProfile(
                                uid = uid,
                                fullName = email.substringBefore("@"),
                                contact = ""
                            )
                            when (val createResult = userRemoteDataSource.createOrUpdateUser(fallback, true)) {
                                is FirestoreResult.Success -> createResult.data
                                is FirestoreResult.Error -> throw createResult.throwable
                            }
                        } else {
                            throw IllegalStateException("Unable to load your profile. Please try again.", cause)
                        }
                    }
                }

                val localUserId = cacheLocalUser(email, userProfile)
                cacheSession(uid, userProfile.fullName.ifBlank { email }, localUserId)
            }

            setLoading(false)

            loginResult.onSuccess {
                Toast.makeText(this@LoginActivity, "Welcome back!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }.onFailure { error ->
                Toast.makeText(this@LoginActivity, error.localizedMessage ?: "Login failed", Toast.LENGTH_LONG).show()
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

    private fun cacheSession(uid: String, userName: String, legacyUserId: Int) {
        userPreferences.setCurrentUserUid(uid)
        userPreferences.setCurrentUserId(legacyUserId)
        userPreferences.setUserName(userName)
    }

    private fun setLoading(isLoading: Boolean) {
        btnLogin.isEnabled = !isLoading
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
