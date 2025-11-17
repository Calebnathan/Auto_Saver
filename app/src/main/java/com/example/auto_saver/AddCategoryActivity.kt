package com.example.auto_saver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.auto_saver.data.firestore.CategoryRemoteDataSource
import com.example.auto_saver.data.firestore.FirestoreCategoryRemoteDataSource
import com.example.auto_saver.data.repository.FirestoreFirstCategoryRepository
import com.example.auto_saver.data.repository.UnifiedCategoryRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddCategoryActivity : AppCompatActivity() {

    private lateinit var etCategoryName: TextInputEditText
    private lateinit var btnSaveCategory: MaterialButton
    private lateinit var toolbar: MaterialToolbar

    private lateinit var categoryRepository: UnifiedCategoryRepository
    private lateinit var userPrefs: UserPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_category)

        userPrefs = UserPreferences(this)
        val database = AppDatabase.getDatabase(this)
        val remoteDataSource: CategoryRemoteDataSource = FirestoreCategoryRemoteDataSource()
        categoryRepository = FirestoreFirstCategoryRepository(
            remoteDataSource = remoteDataSource,
            categoryDao = database.categoryDao(),
            userPreferences = userPrefs
        )

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

        if (categoryName.isEmpty()) {
            Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val uid = try {
                userPrefs.requireUserUid()
            } catch (e: IllegalStateException) {
                Toast.makeText(this@AddCategoryActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            val result = categoryRepository.createCategory(uid, categoryName)

            result.onSuccess {
                Toast.makeText(
                    this@AddCategoryActivity,
                    "Category created successfully",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }.onFailure { error ->
                val message = when {
                    error.message?.contains("already exists", ignoreCase = true) == true ->
                        "Category '$categoryName' already exists"
                    else -> "Failed to create category: ${error.message}"
                }
                Toast.makeText(this@AddCategoryActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
