package com.example.auto_saver

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.example.auto_saver.data.model.ExpenseRecord
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etAmount: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var etDate: TextInputEditText
    private lateinit var etStartTime: TextInputEditText
    private lateinit var etEndTime: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnTakePhoto: MaterialButton
    private lateinit var btnChoosePhoto: MaterialButton
    private lateinit var cardPhotoPreview: MaterialCardView
    private lateinit var ivPhotoPreview: ImageView
    private lateinit var fabRemovePhoto: FloatingActionButton

    private lateinit var userPrefs: UserPreferences
    private val categoryRepository by lazy { MyApplication.categoryRepository }
    private val expenseRepository by lazy { MyApplication.expenseRepository }

    private val categoryMap = mutableMapOf<String, String>()
    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private var selectedStartTime: String? = null
    private var selectedEndTime: String? = null
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
        setContentView(R.layout.activity_add_expense)

        userPrefs = MyApplication.userPreferences

        initializeViews()
        setupToolbar()
        loadCategories()
        setupDatePicker()
        setupTimePickers()
        setupPhotoButtons()
        setupSaveButton()
    }

    private fun initializeViews() {
        etAmount = findViewById(R.id.et_amount)
        etDescription = findViewById(R.id.et_description)
        actvCategory = findViewById(R.id.actv_category)
        etDate = findViewById(R.id.et_date)
        etStartTime = findViewById(R.id.et_start_time)
        etEndTime = findViewById(R.id.et_end_time)
        btnSave = findViewById(R.id.btn_save)
        toolbar = findViewById(R.id.toolbar)
        btnTakePhoto = findViewById(R.id.btn_take_photo)
        btnChoosePhoto = findViewById(R.id.btn_choose_photo)
        cardPhotoPreview = findViewById(R.id.card_photo_preview)
        ivPhotoPreview = findViewById(R.id.iv_photo_preview)
        fabRemovePhoto = findViewById(R.id.fab_remove_photo)

        etDate.setText(selectedDate)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val uid = try {
                userPrefs.requireUserUid()
            } catch (e: IllegalStateException) {
                Toast.makeText(this@AddExpenseActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            try {
                val categories = categoryRepository.observeCategories(uid).first()

                if (categories.isEmpty()) {
                    Toast.makeText(
                        this@AddExpenseActivity,
                        "No categories found. Please add categories first.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                categoryMap.clear()
                categories.forEach { category ->
                    categoryMap[category.name] = category.id
                }

                val categoryNames = categories.map { it.name }
                val adapter = ArrayAdapter(
                    this@AddExpenseActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    categoryNames
                )
                actvCategory.setAdapter(adapter)
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddExpenseActivity,
                    "Error loading categories: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun setupDatePicker() {
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                etDate.setText(selectedDate)
            }, year, month, day).show()
        }
    }

    private fun setupTimePickers() {
        etStartTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                selectedStartTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                etStartTime.setText(selectedStartTime)
            }, hour, minute, true).show()
        }

        etEndTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { _, selectedHour, selectedMinute ->
                selectedEndTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                etEndTime.setText(selectedEndTime)
            }, hour, minute, true).show()
        }
    }

    private fun setupPhotoButtons() {
        btnTakePhoto.setOnClickListener {
            takePhoto()
        }

        btnChoosePhoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        fabRemovePhoto.setOnClickListener {
            removePhoto()
        }
    }

    private fun takePhoto() {
        val photoFile = File(filesDir, "expense_${System.currentTimeMillis()}.jpg")
        tempPhotoUri = FileProvider.getUriForFile(
            this,
            "${packageName}.fileprovider",
            photoFile
        )
        tempPhotoUri?.let { takePictureLauncher.launch(it) }
    }

    private fun displayPhoto(uri: Uri) {
        ivPhotoPreview.setImageURI(uri)
        cardPhotoPreview.visibility = View.VISIBLE
    }

    private fun removePhoto() {
        photoUri = null
        tempPhotoUri = null
        cardPhotoPreview.visibility = View.GONE
        ivPhotoPreview.setImageURI(null)
    }

    private fun setupSaveButton() {
        btnSave.setOnClickListener {
            saveExpense()
        }
    }

    private fun saveExpense() {
        val amountStr = etAmount.text.toString()
        val descriptionText = etDescription.text.toString()
        val categoryName = actvCategory.text.toString()

        // Validation
        if (amountStr.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_amount, Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoryName.isEmpty()) {
            Toast.makeText(this, R.string.error_select_category, Toast.LENGTH_SHORT).show()
            return
        }

        val categoryId = categoryMap[categoryName]
        if (categoryId == null) {
            Toast.makeText(this, "Invalid category selected", Toast.LENGTH_SHORT).show()
            return
        }

        // Save expense using repository
        lifecycleScope.launch {
            val uid = try {
                userPrefs.requireUserUid()
            } catch (e: IllegalStateException) {
                Toast.makeText(this@AddExpenseActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val expense = ExpenseRecord(
                id = "",
                uid = uid,
                categoryId = categoryId,
                date = selectedDate,
                amount = amount,
                description = descriptionText.takeIf { it.isNotBlank() },
                startTime = selectedStartTime,
                endTime = selectedEndTime,
                photoPath = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            // Create expense with optional photo
            val result = expenseRepository.createExpense(uid, expense, photoUri)

            result.onSuccess {
                Toast.makeText(
                    this@AddExpenseActivity,
                    R.string.expense_saved,
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }.onFailure { error ->
                Toast.makeText(
                    this@AddExpenseActivity,
                    "Failed to save expense: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
