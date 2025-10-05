package com.example.auto_saver

import android.content.Intent
import android.os.Bundle
import android.service.autofill.OnClickAction
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.Button
import android.widget.PopupMenu
import android.widget.Toast


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
            //comment Example\
        }
            val button_menu: Button = findViewById(R.id.button_menu)
            val button_add: Button = findViewById(R.id.button_add)

            button_menu.setOnClickListener {
                val popup = PopupMenu(this, button_menu)
                popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.action_settings -> {
                            // Example: open SettingsActivity
                            val intent = Intent(this, SettingsActivity::class.java)
                            startActivity(intent)
                            true
                        }
                        R.id.action_profile -> {
                            // Example: open ProfileActivity
                            val intent = Intent(this, ProfileActivity::class.java)
                            startActivity(intent)
                            true
                        }
                        R.id.action_logout -> {
                            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags =
                                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)

                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }

            button_add.setOnClickListener {
                startActivity(Intent(this, AddUserActivity::class.java))
            }
        }
    }


