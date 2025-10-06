package com.example.auto_saver

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GoalsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var tvCurrentMonth: TextView
    private lateinit var etMinGoal: TextInputEditText
    private lateinit var etMaxGoal: TextInputEditText
    private lateinit var tvCurrentSpending: TextView
    private lateinit var btnSaveGoal: MaterialButton

    private lateinit var database: AppDatabase
    private lateinit var userPrefs: UserPreferences
    private var currentMonth: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        database = AppDatabase.getDatabase(this)
        userPrefs = UserPreferences(this)

        initializeViews()
        setupToolbar()
        loadCurrentMonth()
        loadExistingGoal()
        loadCurrentSpending()
        setupSaveButton()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        tvCurrentMonth = findViewById(R.id.tv_current_month)
        etMinGoal = findViewById(R.id.et_min_goal)
        etMaxGoal = findViewById(R.id.et_max_goal)
        tvCurrentSpending = findViewById(R.id.tv_current_spending)
        btnSaveGoal = findViewById(R.id.btn_save_goal)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadCurrentMonth() {
        val calendar = Calendar.getInstance()
        currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)
        val displayMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time)
        tvCurrentMonth.text = displayMonth
    }

    private fun loadExistingGoal() {
        lifecycleScope.launch {
            val userId = userPrefs.getCurrentUserId()
            if (userId == -1) return@launch

            val goal = database.goalDao().getGoalForMonth(userId, currentMonth)
            goal?.let {
                etMinGoal.setText(it.minGoal.toString())
                etMaxGoal.setText(it.maxGoal.toString())
            }
        }
    }

    private fun loadCurrentSpending() {
        lifecycleScope.launch {
            val userId = userPrefs.getCurrentUserId()
            if (userId == -1) return@launch

            val calendar = Calendar.getInstance()
            val startDate = SimpleDateFormat("yyyy-MM-01", Locale.getDefault()).format(calendar.time)

            // Get last day of month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val expenses = database.expenseDao().getExpensesByUser(userId)
            val monthlyExpenses = expenses.filter { it.date >= startDate && it.date <= endDate }
            val total = monthlyExpenses.sumOf { it.amount }

            tvCurrentSpending.text = String.format(Locale.getDefault(), "$%.2f", total)
        }
    }

    private fun setupSaveButton() {
        btnSaveGoal.setOnClickListener {
            saveGoal()
        }
    }

    private fun saveGoal() {
        val minGoalStr = etMinGoal.text.toString()
        val maxGoalStr = etMaxGoal.text.toString()

        if (minGoalStr.isEmpty() || maxGoalStr.isEmpty()) {
            Toast.makeText(this, "Please enter both minimum and maximum goals", Toast.LENGTH_SHORT).show()
            return
        }

        val minGoal = minGoalStr.toDoubleOrNull()
        val maxGoal = maxGoalStr.toDoubleOrNull()

        if (minGoal == null || maxGoal == null) {
            Toast.makeText(this, "Please enter valid amounts", Toast.LENGTH_SHORT).show()
            return
        }

        if (minGoal < 0 || maxGoal < 0) {
            Toast.makeText(this, "Goals must be positive values", Toast.LENGTH_SHORT).show()
            return
        }

        if (minGoal > maxGoal) {
            Toast.makeText(this, "Minimum goal cannot be greater than maximum goal", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val userId = userPrefs.getCurrentUserId()
            if (userId == -1) return@launch

            val goal = Goal(
                userId = userId,
                month = currentMonth,
                minGoal = minGoal,
                maxGoal = maxGoal
            )

            database.goalDao().insertGoal(goal)

            Toast.makeText(this@GoalsActivity, "Goal saved successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
