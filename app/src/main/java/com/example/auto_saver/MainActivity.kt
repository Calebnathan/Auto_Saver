package com.example.auto_saver

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.auto_saver.adapters.GroupedExpenseAdapter
import com.example.auto_saver.models.ExpenseListItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var fabMenu: FloatingActionButton
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvUserName: TextView
    private lateinit var rvExpenses: RecyclerView
    private lateinit var tvTotalSpent: TextView
    private lateinit var tvExpenseCount: TextView
    private lateinit var emptyStateLayout: LinearLayout

    // Goal progress views
    private lateinit var btnManageGoals: MaterialButton
    private lateinit var progressGoal: ProgressBar
    private lateinit var tvMinGoal: TextView
    private lateinit var tvMaxGoal: TextView
    private lateinit var tvCurrentSpent: TextView
    private lateinit var tvGoalStatus: TextView

    // Filter button
    private lateinit var btnFilter: MaterialButton

    private lateinit var database: AppDatabase
    private lateinit var userPrefs: UserPreferences
    private lateinit var groupedExpenseAdapter: GroupedExpenseAdapter

    private val categoryCache = mutableMapOf<Int, String>()
    private val expandedCategories = mutableSetOf<Int>()

    // Date filter state
    private var filterStartDate: String? = null
    private var filterEndDate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = AppDatabase.getDatabase(this)
        userPrefs = UserPreferences(this)

        initializeViews()
        setupMenu()
        setupFabs()
        setupRecyclerViews()
        setupGoalProgress()
        setupFilterButton()
        loadCategories()
    }

    override fun onResume() {
        super.onResume()
        loadCategories()
        loadExpenses()
        loadGoalProgress()
    }

    private fun initializeViews() {
        fabMenu = findViewById(R.id.fab_menu)
        fabAdd = findViewById(R.id.fab_add)
        tvUserName = findViewById(R.id.tv_user_name)
        rvExpenses = findViewById(R.id.rv_expenses)
        tvTotalSpent = findViewById(R.id.tv_total_spent)
        tvExpenseCount = findViewById(R.id.tv_expense_count)
        emptyStateLayout = findViewById(R.id.empty_state_layout)

        // Goal progress views
        btnManageGoals = findViewById(R.id.btn_manage_goals)
        progressGoal = findViewById(R.id.progress_goal)
        tvMinGoal = findViewById(R.id.tv_min_goal)
        tvMaxGoal = findViewById(R.id.tv_max_goal)
        tvCurrentSpent = findViewById(R.id.tv_current_spent)
        tvGoalStatus = findViewById(R.id.tv_goal_status)

        // Filter button
        btnFilter = findViewById(R.id.btn_filter)

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = userPrefs.getCurrentUserId()
        if (userId != -1) {
            lifecycleScope.launch {
                val user = database.userDao().getUserById(userId)
                user?.let {
                    // Display full name in profile card
                    tvUserName.text = it.fullName ?: "User"
                }
            }
        } else {
            tvUserName.text = "User"
        }
    }

    private fun setupMenu() {
        fabMenu.setOnClickListener {
            val popup = PopupMenu(this, fabMenu)
            popup.menuInflater.inflate(R.menu.popup_menu, popup.menu)

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.action_theme_toggle -> {
                        toggleTheme()
                        true
                    }
                    R.id.action_settings -> {
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.action_profile -> {
                        val intent = Intent(this, ProfileActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    R.id.action_reset -> {
                        showResetDatabaseDialog()
                        true
                    }
                    R.id.action_logout -> {
                        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()
                        userPrefs.clearSession()
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
    }

    private fun toggleTheme() {
        val isDarkMode = userPrefs.isDarkModeEnabled()
        userPrefs.setDarkMode(!isDarkMode)

        // Apply the theme change
        AppCompatDelegate.setDefaultNightMode(
            if (!isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // The activity will automatically recreate with the new theme
    }

    private fun showResetDatabaseDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset Database")
            .setMessage("Are you sure you want to reset the database? This will delete ALL data including all users, expenses, categories, and goals. This cannot be undone!")
            .setPositiveButton("Reset") { _, _ ->
                resetDatabase()
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun resetDatabase() {
        lifecycleScope.launch {
            try {
                // Show loading toast on main thread
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Resetting database...",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                withContext(Dispatchers.IO) {
                    // Clear all tables on background thread (already in coroutine)
                    database.clearAllTables()
                }

                // Clear user preferences
                userPrefs.clearSession()

                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Database reset successfully",
                        Toast.LENGTH_LONG
                    ).show()

                    // Navigate back to login
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Error resetting database: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun setupFabs() {
        fabAdd.setOnClickListener {
            val userId = userPrefs.getCurrentUserId()
            if (userId == -1) {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Show popup menu with add options (matching menu modal design)
            showAddPopupMenu()
        }
    }

    private fun setupRecyclerViews() {
        // Grouped expense adapter
        groupedExpenseAdapter = GroupedExpenseAdapter(
            onExpenseClick = { expenseId ->
                Toast.makeText(this, "Expense ID: $expenseId", Toast.LENGTH_SHORT).show()
            },
            onCategoryHeaderClick = { categoryHeader ->
                toggleCategoryExpansion(categoryHeader)
            }
        )
        rvExpenses.adapter = groupedExpenseAdapter
    }

    private fun toggleCategoryExpansion(categoryHeader: ExpenseListItem.CategoryHeader) {
        val categoryId = categoryHeader.category.id
        if (expandedCategories.contains(categoryId)) {
            expandedCategories.remove(categoryId)
        } else {
            expandedCategories.add(categoryId)
        }
        loadExpenses() // Refresh the list
    }

    private fun setupGoalProgress() {
        btnManageGoals.setOnClickListener {
            val intent = Intent(this, GoalsActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupFilterButton() {
        btnFilter.setOnClickListener {
            showFilterOptions()
        }
    }

    private fun showFilterOptions() {
        val options = arrayOf("All Time", "This Month", "Last Month", "Custom Range", "Clear Filter")

        AlertDialog.Builder(this)
            .setTitle("Filter Expenses")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> clearFilter()
                    1 -> filterThisMonth()
                    2 -> filterLastMonth()
                    3 -> showCustomDateRange()
                    4 -> clearFilter()
                }
            }
            .show()
    }

    private fun clearFilter() {
        filterStartDate = null
        filterEndDate = null
        btnFilter.text = "Filter"
        loadExpenses()
    }

    private fun filterThisMonth() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        filterStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        filterEndDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        btnFilter.text = "This Month"
        loadExpenses()
    }

    private fun filterLastMonth() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, -1)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        filterStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        filterEndDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

        btnFilter.text = "Last Month"
        loadExpenses()
    }

    private fun showCustomDateRange() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            filterStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            DatePickerDialog(this, { _, endYear, endMonth, endDay ->
                calendar.set(endYear, endMonth, endDay)
                filterEndDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                btnFilter.text = "Custom"
                loadExpenses()
            }, year, month, day).show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showAddPopupMenu() {
        val popup = PopupMenu(this, fabAdd)
        popup.menuInflater.inflate(R.menu.add_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add_expense -> {
                    showAddExpenseDialog()
                    true
                }
                R.id.action_create_category -> {
                    showAddCategoryDialog()
                    true
                }
                R.id.action_remove_categories -> {
                    showRemoveCategoriesDialog()
                    true
                }
                R.id.action_analytics -> {
                    val intent = Intent(this, CategoryAnalyticsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showAddCategoryDialog() {
        val input = EditText(this)
        input.hint = "Category name"
        input.setPadding(60, 40, 60, 40)

        AlertDialog.Builder(this)
            .setTitle("Create Category")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val categoryName = input.text.toString().trim()
                if (categoryName.isEmpty()) {
                    Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
                } else {
                    createCategory(categoryName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createCategory(categoryName: String) {
        lifecycleScope.launch {
            val userId = userPrefs.getCurrentUserId()
            if (userId == -1) {
                Toast.makeText(this@MainActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                return@launch
            }

            // Check if category already exists
            val existingCategories = database.categoryDao().getCategoriesByUser(userId)
            if (existingCategories.any { it.name.equals(categoryName, ignoreCase = true) }) {
                Toast.makeText(
                    this@MainActivity,
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
                this@MainActivity,
                "Category created successfully",
                Toast.LENGTH_SHORT
            ).show()

            // Refresh the list
            loadCategories()
        }
    }

    private fun showAddExpenseDialog() {
        lifecycleScope.launch {
            val userId = userPrefs.getCurrentUserId()
            val categories = database.categoryDao().getCategoriesByUser(userId)

            if (categories.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "No categories found. Please create a category first.",
                    Toast.LENGTH_LONG
                ).show()
                return@launch
            }

            // Create a custom dialog view
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_expense, null)
            val etAmount = dialogView.findViewById<EditText>(R.id.et_amount)
            val etDescription = dialogView.findViewById<EditText>(R.id.et_description)
            val spinnerCategory = dialogView.findViewById<Spinner>(R.id.spinner_category)

            // Setup category spinner
            val categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, categoryNames)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = adapter

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Add Expense")
                .setView(dialogView)
                .setPositiveButton("Save") { _, _ ->
                    val amount = etAmount.text.toString().toDoubleOrNull()
                    val description = etDescription.text.toString().trim()
                    val selectedCategoryPos = spinnerCategory.selectedItemPosition

                    if (amount == null || amount <= 0) {
                        Toast.makeText(this@MainActivity, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    } else if (description.isEmpty()) {
                        Toast.makeText(this@MainActivity, "Please enter a description", Toast.LENGTH_SHORT).show()
                    } else {
                        saveExpense(amount, description, categories[selectedCategoryPos].id)
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun saveExpense(amount: Double, description: String, categoryId: Int) {
        lifecycleScope.launch {
            val userId = userPrefs.getCurrentUserId()
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val expense = Expense(
                userId = userId,
                categoryId = categoryId,
                amount = amount,
                description = description,
                date = currentDate
            )

            database.expenseDao().insertExpense(expense)

            Toast.makeText(
                this@MainActivity,
                "Expense saved successfully",
                Toast.LENGTH_SHORT
            ).show()

            // Refresh the list
            loadExpenses()
        }
    }

    private fun showRemoveCategoriesDialog() {
        lifecycleScope.launch {
            val userId = userPrefs.getCurrentUserId()
            if (userId == -1) {
                Toast.makeText(this@MainActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val categories = database.categoryDao().getCategoriesByUser(userId)

            if (categories.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "No categories to remove",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            // Get expense counts for each category
            val expenses = database.expenseDao().getExpensesByUser(userId)
            val categoryExpenseCounts = categories.map { category ->
                val count = expenses.count { it.categoryId == category.id }
                Pair(category, count)
            }

            val categoryNames = categoryExpenseCounts.map { (category, count) ->
                if (count > 0) {
                    "${category.name} ($count expenses)"
                } else {
                    category.name
                }
            }.toTypedArray()

            AlertDialog.Builder(this@MainActivity)
                .setTitle("Remove Category")
                .setItems(categoryNames) { _, which ->
                    val (selectedCategory, expenseCount) = categoryExpenseCounts[which]
                    confirmCategoryDeletion(selectedCategory, expenseCount)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun confirmCategoryDeletion(category: Category, expenseCount: Int) {
        val message = if (expenseCount > 0) {
            "Are you sure you want to delete '${category.name}'?\n\n⚠️ WARNING: This category has $expenseCount expense(s) associated with it. All these expenses will also be permanently deleted!\n\nThis action cannot be undone."
        } else {
            "Are you sure you want to delete '${category.name}'?\n\nThis action cannot be undone."
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory(category, expenseCount)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteCategory(category: Category, expenseCount: Int) {
        lifecycleScope.launch {
            try {
                // Delete all expenses associated with this category first
                if (expenseCount > 0) {
                    val expenses = database.expenseDao().getExpensesByUser(userPrefs.getCurrentUserId())
                    val categoryExpenses = expenses.filter { it.categoryId == category.id }
                    categoryExpenses.forEach { expense ->
                        database.expenseDao().deleteExpense(expense)
                    }
                }

                // Delete the category
                database.categoryDao().deleteCategory(category)

                runOnUiThread {
                    val message = if (expenseCount > 0) {
                        "Category '${category.name}' and $expenseCount expense(s) deleted successfully"
                    } else {
                        "Category '${category.name}' deleted successfully"
                    }

                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()

                    // Refresh the lists
                    loadCategories()
                    loadExpenses()
                    loadGoalProgress()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Error deleting category: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            val userId = userPrefs.getCurrentUserId()
            if (userId == -1) {
                Toast.makeText(this@MainActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                return@launch
            }

            val categories = database.categoryDao().getCategoriesByUser(userId)

            // If user has no categories, create default ones
            if (categories.isEmpty()) {
                createDefaultCategories(userId)
                val newCategories = database.categoryDao().getCategoriesByUser(userId)
                categoryCache.clear()
                newCategories.forEach { category ->
                    categoryCache[category.id] = category.name
                }
            } else {
                categoryCache.clear()
                categories.forEach { category ->
                    categoryCache[category.id] = category.name
                }
            }

            loadExpenses()
        }
    }

    private suspend fun createDefaultCategories(userId: Int) {
        val defaultCategories = listOf(
            Category(userId = userId, name = "Bills"),
            Category(userId = userId, name = "Food"),
            Category(userId = userId, name = "Pets")
        )

        defaultCategories.forEach { category ->
            database.categoryDao().insertCategory(category)
        }
    }

    private fun loadExpenses() {
        lifecycleScope.launch {
            val userId = userPrefs.getCurrentUserId()
            if (userId == -1) return@launch

            val expenses = database.expenseDao().getExpensesByUser(userId)
            val categories = database.categoryDao().getCategoriesByUser(userId)

            // Apply date range filter if set
            val filteredExpenses = if (filterStartDate != null && filterEndDate != null) {
                expenses.filter { it.date >= filterStartDate!! && it.date <= filterEndDate!! }
            } else {
                expenses
            }

            if (filteredExpenses.isEmpty() && categories.isEmpty()) {
                emptyStateLayout.visibility = View.VISIBLE
                rvExpenses.visibility = View.GONE
                tvTotalSpent.text = "$0.00"
                tvExpenseCount.text = "0 expenses"
            } else {
                emptyStateLayout.visibility = View.GONE
                rvExpenses.visibility = View.VISIBLE

                // Group expenses by category
                val expensesByCategory = filteredExpenses.groupBy { it.categoryId }
                val listItems = mutableListOf<ExpenseListItem>()

                // Process each category - now showing all categories regardless of expense count
                categories.forEach { category ->
                    val categoryExpenses = expensesByCategory[category.id] ?: emptyList()

                    // Always show category header with expense count
                    val isExpanded = expandedCategories.contains(category.id)
                    listItems.add(
                        ExpenseListItem.CategoryHeader(
                            category = category,
                            expenses = categoryExpenses,
                            isExpanded = isExpanded
                        )
                    )

                    // If expanded, show all expenses under this category
                    if (isExpanded) {
                        categoryExpenses.forEach { expense ->
                            listItems.add(
                                ExpenseListItem.ExpenseItem(
                                    expense = expense,
                                    categoryName = category.name
                                )
                            )
                        }
                    }
                }

                // Add expenses without categories (orphaned expenses) at the bottom
                val orphanedExpenses = filteredExpenses.filter { expense ->
                    categories.none { it.id == expense.categoryId }
                }

                orphanedExpenses.forEach { expense ->
                    listItems.add(
                        ExpenseListItem.ExpenseItem(
                            expense = expense,
                            categoryName = "No Category"
                        )
                    )
                }

                groupedExpenseAdapter.submitList(listItems)

                // Update summary
                val total = filteredExpenses.sumOf { it.amount }
                tvTotalSpent.text = getString(R.string.currency_format, total)

                val count = filteredExpenses.size
                tvExpenseCount.text = if (count == 1) "1 expense" else "$count expenses"
            }
        }
    }

    private fun loadGoalProgress() {
        lifecycleScope.launch {
            val userId = userPrefs.getCurrentUserId()
            if (userId == -1) return@launch

            val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val goal = database.goalDao().getGoalForMonth(userId, currentMonth)

            // Calculate current month spending
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            val expenses = database.expenseDao().getExpensesByUser(userId)
            val monthlyExpenses = expenses.filter { it.date >= startDate && it.date <= endDate }
            val currentSpent = monthlyExpenses.sumOf { it.amount }

            tvCurrentSpent.text = String.format("$%.2f", currentSpent)

            if (goal != null) {
                tvMinGoal.text = String.format("$%.2f", goal.minGoal)
                tvMaxGoal.text = String.format("$%.2f", goal.maxGoal)

                // Calculate progress percentage
                val progress = if (goal.maxGoal > 0) {
                    ((currentSpent / goal.maxGoal) * 100).toInt()
                } else 0

                progressGoal.progress = progress.coerceIn(0, 100)

                // Update status message
                tvGoalStatus.text = when {
                    currentSpent > goal.maxGoal -> "⚠️ Over budget by $${String.format("%.2f", currentSpent - goal.maxGoal)}"
                    currentSpent < goal.minGoal -> "Below minimum goal"
                    else -> "✓ On track! Keep it up"
                }

                // Update status color
                tvGoalStatus.setTextColor(
                    getColor(when {
                        currentSpent > goal.maxGoal -> R.color.error_red
                        currentSpent < goal.minGoal -> R.color.goal_warning
                        else -> R.color.goal_success
                    })
                )
            } else {
                tvMinGoal.text = "$0.00"
                tvMaxGoal.text = "$0.00"
                progressGoal.progress = 0
                tvGoalStatus.text = "Set your monthly goals to track progress"
            }
        }
    }
}
