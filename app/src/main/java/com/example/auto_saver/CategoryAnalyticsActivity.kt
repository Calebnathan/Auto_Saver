package com.example.auto_saver

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.auto_saver.adapters.CategoryAnalyticsAdapter
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CategoryAnalyticsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var btnSelectPeriod: MaterialButton
    private lateinit var tvDateRange: TextView
    private lateinit var tvTotalAmount: TextView
    private lateinit var rvCategoryAnalytics: RecyclerView
    private lateinit var emptyState: LinearLayout

    private lateinit var database: AppDatabase
    private lateinit var userPrefs: UserPreferences

    private var startDate: String = ""
    private var endDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_analytics)

        database = AppDatabase.getDatabase(this)
        userPrefs = UserPreferences(this)

        initializeViews()
        setupToolbar()
        setDefaultDateRange()
        setupPeriodSelector()
        loadAnalytics()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        btnSelectPeriod = findViewById(R.id.btn_select_period)
        tvDateRange = findViewById(R.id.tv_date_range)
        tvTotalAmount = findViewById(R.id.tv_total_amount)
        rvCategoryAnalytics = findViewById(R.id.rv_category_analytics)
        emptyState = findViewById(R.id.empty_state)

        rvCategoryAnalytics.layoutManager = LinearLayoutManager(this)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setDefaultDateRange() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        updateDateRangeDisplay()
    }

    private fun setupPeriodSelector() {
        btnSelectPeriod.setOnClickListener {
            showPeriodOptions()
        }
    }

    private fun showPeriodOptions() {
        val options = arrayOf("This Month", "Last Month", "This Year", "Custom Range")

        AlertDialog.Builder(this)
            .setTitle("Select Period")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> setThisMonth()
                    1 -> setLastMonth()
                    2 -> setThisYear()
                    3 -> showCustomDateRange()
                }
            }
            .show()
    }

    private fun setThisMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        btnSelectPeriod.text = "This Month"
        updateDateRangeDisplay()
        loadAnalytics()
    }

    private fun setLastMonth() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        btnSelectPeriod.text = "Last Month"
        updateDateRangeDisplay()
        loadAnalytics()
    }

    private fun setThisYear() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, 0)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.set(Calendar.MONTH, 11)
        calendar.set(Calendar.DAY_OF_MONTH, 31)
        endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        btnSelectPeriod.text = "This Year"
        updateDateRangeDisplay()
        loadAnalytics()
    }

    private fun showCustomDateRange() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            // Show end date picker
            DatePickerDialog(this, { _, endYear, endMonth, endDay ->
                calendar.set(endYear, endMonth, endDay)
                endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                btnSelectPeriod.text = "Custom Range"
                updateDateRangeDisplay()
                loadAnalytics()
            }, year, month, day).show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun updateDateRangeDisplay() {
        tvDateRange.text = "$startDate to $endDate"
    }

    private fun loadAnalytics() {
        lifecycleScope.launch {
            val userId = userPrefs.getCurrentUserId()
            if (userId == -1) return@launch

            // Get all expenses in date range
            val expenses = database.expenseDao().getExpensesByUser(userId)
                .filter { it.date >= startDate && it.date <= endDate }

            if (expenses.isEmpty()) {
                showEmptyState()
                return@launch
            }

            // Calculate total
            val total = expenses.sumOf { it.amount }
            tvTotalAmount.text = String.format(Locale.getDefault(), "$%.2f", total)

            // Group by category and calculate percentages
            val categoryMap = mutableMapOf<Int, MutableList<Expense>>()
            expenses.forEach { expense ->
                categoryMap.getOrPut(expense.categoryId) { mutableListOf() }.add(expense)
            }

            val categorySpendingList = mutableListOf<CategorySpending>()

            for ((categoryId, expenseList) in categoryMap) {
                val category = database.categoryDao().getCategoryById(categoryId)
                val categoryTotal = expenseList.sumOf { it.amount }
                val percentage = if (total > 0) ((categoryTotal / total) * 100).toFloat() else 0f

                categorySpendingList.add(
                    CategorySpending(
                        categoryId = categoryId,
                        categoryName = category?.name ?: "Unknown",
                        totalAmount = categoryTotal,
                        percentage = percentage,
                        expenseCount = expenseList.size
                    )
                )
            }

            // Sort by amount descending
            categorySpendingList.sortByDescending { it.totalAmount }

            // Update RecyclerView
            rvCategoryAnalytics.adapter = CategoryAnalyticsAdapter(categorySpendingList)
            hideEmptyState()
        }
    }

    private fun showEmptyState() {
        emptyState.visibility = View.VISIBLE
        rvCategoryAnalytics.visibility = View.GONE
        tvTotalAmount.text = "$0.00"
    }

    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        rvCategoryAnalytics.visibility = View.VISIBLE
    }
}
