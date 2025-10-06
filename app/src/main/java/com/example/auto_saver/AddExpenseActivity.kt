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
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
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

    private lateinit var database: AppDatabase
    private lateinit var userPrefs: UserPreferences

    private val categories = mutableListOf<Category>()
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

        database = AppDatabase.getDatabase(this)
        userPrefs = UserPreferences(this)

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

        // Set today's date
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
            val userId = userPrefs.getCurrentUserId()
            if (userId == -1) {
                Toast.makeText(this@AddExpenseActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            categories.clear()
            categories.addAll(database.categoryDao().getCategoriesByUser(userId))

            if (categories.isEmpty()) {
                Toast.makeText(this@AddExpenseActivity, "No categories found. Please add categories first.", Toast.LENGTH_LONG).show()
            }

            val categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(
                this@AddExpenseActivity,
                android.R.layout.simple_dropdown_item_1line,
                categoryNames
            )
            actvCategory.setAdapter(adapter)
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
        val description = etDescription.text.toString()
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

        val selectedCategory = categories.find { it.name == categoryName }
        if (selectedCategory == null) {
            Toast.makeText(this, "Invalid category selected", Toast.LENGTH_SHORT).show()
            return
        }

        // Save expense with all fields
        lifecycleScope.launch {
            // Copy photo to permanent storage if exists
            val photoPath = photoUri?.let { uri ->
                val photoFile = File(filesDir, "expense_photo_${System.currentTimeMillis()}.jpg")
                contentResolver.openInputStream(uri)?.use { input ->
                    photoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                photoFile.absolutePath
            }

            val expense = Expense(
                userId = userPrefs.getCurrentUserId(),
                categoryId = selectedCategory.id,
                amount = amount,
                description = description.ifEmpty { null },
                date = selectedDate,
                startTime = selectedStartTime,
                endTime = selectedEndTime,
                photoPath = photoPath
            )

            database.expenseDao().insertExpense(expense)

            Toast.makeText(
                this@AddExpenseActivity,
                R.string.expense_saved,
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }
}
