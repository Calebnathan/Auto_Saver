package com.example.auto_saver

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.auto_saver.data.model.GoalRecord
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.flow.first
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

    private lateinit var userPrefs: UserPreferences
    private val goalRepository by lazy { MyApplication.goalRepository }
    private val expenseRepository by lazy { MyApplication.expenseRepository }
    
    private var currentMonth: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        userPrefs = MyApplication.userPreferences

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
            val uid = try {
                userPrefs.requireUserUid()
            } catch (e: IllegalStateException) {
                Toast.makeText(this@GoalsActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                finish()
                return@launch
            }

            try {
                val goal = goalRepository.getGoalForMonth(uid, currentMonth)
                goal?.let {
                    etMinGoal.setText(it.minGoal.toString())
                    etMaxGoal.setText(it.maxGoal.toString())
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@GoalsActivity,
                    "Error loading goal: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun loadCurrentSpending() {
        lifecycleScope.launch {
            val uid = try {
                userPrefs.requireUserUid()
            } catch (e: IllegalStateException) {
                return@launch
            }

            try {
                val calendar = Calendar.getInstance()
                val startDate = SimpleDateFormat("yyyy-MM-01", Locale.getDefault()).format(calendar.time)

                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                val total = expenseRepository.getTotalSpent(uid, startDate, endDate).first()
                tvCurrentSpending.text = String.format(Locale.getDefault(), "$%.2f", total)
            } catch (e: Exception) {
                tvCurrentSpending.text = "$0.00"
            }
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
            val uid = try {
                userPrefs.requireUserUid()
            } catch (e: IllegalStateException) {
                Toast.makeText(this@GoalsActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val goal = GoalRecord(
                id = currentMonth,
                uid = uid,
                month = currentMonth,
                minGoal = minGoal,
                maxGoal = maxGoal,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val result = goalRepository.upsertGoal(uid, goal)
            
            result.onSuccess {
                Toast.makeText(this@GoalsActivity, "Goal saved successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }.onFailure { error ->
                Toast.makeText(
                    this@GoalsActivity,
                    "Failed to save goal: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
