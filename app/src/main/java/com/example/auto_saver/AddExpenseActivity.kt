package com.example.auto_saver

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etAmount: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var actvCategory: AutoCompleteTextView
    private lateinit var etDate: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var toolbar: MaterialToolbar

    private lateinit var database: AppDatabase
    private lateinit var userPrefs: UserPreferences

    private val categories = mutableListOf<Category>()
    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        database = AppDatabase.getDatabase(this)
        userPrefs = UserPreferences(this)

        initializeViews()
        setupToolbar()
        loadCategories()
        setupDatePicker()
        setupSaveButton()
    }

    private fun initializeViews() {
        etAmount = findViewById(R.id.et_amount)
        etDescription = findViewById(R.id.et_description)
        actvCategory = findViewById(R.id.actv_category)
        etDate = findViewById(R.id.et_date)
        btnSave = findViewById(R.id.btn_save)
        toolbar = findViewById(R.id.toolbar)

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

        // Save expense
        lifecycleScope.launch {
            val expense = Expense(
                userId = userPrefs.getCurrentUserId(),
                categoryId = selectedCategory.id,
                amount = amount,
                description = description.ifEmpty { null },
                date = selectedDate
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
