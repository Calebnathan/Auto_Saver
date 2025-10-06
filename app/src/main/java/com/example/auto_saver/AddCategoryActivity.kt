package com.example.auto_saver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var etCategoryName: TextInputEditText
    private lateinit var btnSaveCategory: MaterialButton
    private lateinit var toolbar: MaterialToolbar

    private lateinit var database: AppDatabase
    private lateinit var userPrefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_category)

        database = AppDatabase.getDatabase(this)
        userPrefs = UserPreferences(this)

        initializeViews()
        setupToolbar()
        setupSaveButton()
    }

    private fun initializeViews() {
        etCategoryName = findViewById(R.id.et_category_name)
        btnSaveCategory = findViewById(R.id.btn_save_category)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSaveButton() {
        btnSaveCategory.setOnClickListener {
            saveCategory()
        }
    }

    private fun saveCategory() {
        val categoryName = etCategoryName.text.toString().trim()

        // Validation
        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            return
        }

        // Save category
        lifecycleScope.launch {
            val userId = userPrefs.getCurrentUserId()
            if (userId == -1) {
                Toast.makeText(this@AddCategoryActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            // Check if category already exists for this user
            val existingCategories = database.categoryDao().getCategoriesByUser(userId)
            if (existingCategories.any { it.name.equals(categoryName, ignoreCase = true) }) {
                Toast.makeText(
                    this@AddCategoryActivity,
                    "Category '$categoryName' already exists",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            val category = Category(
                userId = userId,
                name = categoryName
            )

            database.categoryDao().insertCategory(category)

            Toast.makeText(
                this@AddCategoryActivity,
                "Category created successfully",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}

