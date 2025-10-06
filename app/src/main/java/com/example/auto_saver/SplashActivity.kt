package com.example.auto_saver

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Splash screen that displays the app logo with animation
 * Then navigates to Login or Main screen based on login state
 */
class SplashActivity : AppCompatActivity() {

    private lateinit var userPreferences: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        userPreferences = UserPreferences(this)

        val logoImageView = findViewById<ImageView>(R.id.ivLogo)

        // Load and start animation
        val fadeIn = AnimationUtils.loadAnimation(this, R.anim.splash_fade_in)
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.splash_scale)

        logoImageView.startAnimation(fadeIn)
        logoImageView.startAnimation(scaleUp)

        // Navigate after animation
        lifecycleScope.launch {
            delay(2500) // 2.5 seconds for splash

            if (userPreferences.isLoggedIn()) {
                // User is logged in, go to main
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            } else {
                // User not logged in, go to login
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            }
            finish()
        }
    }
}

