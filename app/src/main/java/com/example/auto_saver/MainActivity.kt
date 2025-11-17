package com.example.auto_saver

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.auto_saver.adapters.GroupedExpenseAdapter
import com.example.auto_saver.data.model.CategoryRecord
import com.example.auto_saver.data.model.ExpenseRecord
import com.example.auto_saver.models.ExpenseListItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var fabMenu: FloatingActionButton
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvUserName: TextView
    private lateinit var ivProfileIcon: ImageView
    private lateinit var rvExpenses: RecyclerView
    private lateinit var tvTotalSpent: TextView
    private lateinit var tvExpenseCount: TextView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var headerScroll: NestedScrollView
    private lateinit var categoriesToolbar: LinearLayout
    private lateinit var expensesContainer: FrameLayout

    // Goal progress views
    private lateinit var btnManageGoals: MaterialButton
    private lateinit var progressGoal: ProgressBar
    private lateinit var tvMinGoal: TextView
    private lateinit var tvMaxGoal: TextView
    private lateinit var tvCurrentSpent: TextView
    private lateinit var tvGoalStatus: TextView

    // Filter button
    private lateinit var btnFilter: MaterialButton
    private lateinit var viewToggleGroup: MaterialButtonToggleGroup

    private lateinit var database: AppDatabase
    private lateinit var userPrefs: UserPreferences
    private lateinit var groupedExpenseAdapter: GroupedExpenseAdapter
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    // ViewModel using unified repositories
    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            MyApplication.expenseRepository,
            MyApplication.categoryRepository,
            MyApplication.goalRepository,
            MyApplication.userPreferences
        )
    }

    private val categoryCache = mutableMapOf<String, CategoryRecord>()
    private val expandedCategories = mutableSetOf<String>()

    // Date filter state
    private var filterStartDate: String? = null
    private var filterEndDate: String? = null
    private var currentViewMode = ViewMode.DASHBOARD

    private enum class ViewMode {
        DASHBOARD,
        CATEGORIES
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_view)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        database = MyApplication.database
        userPrefs = MyApplication.userPreferences

        initializeViews()
        setupMenu()
        setupFabs()
        setupRecyclerViews()
        setupGoalProgress()
        setupFilterButton()
        setupViewToggle()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun initializeViews() {
        fabMenu = findViewById(R.id.fab_menu)
        fabAdd = findViewById(R.id.fab_add)
        tvUserName = findViewById(R.id.tv_user_name)
        ivProfileIcon = findViewById(R.id.iv_profile_icon)
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
        viewToggleGroup = findViewById(R.id.view_toggle_group)
        headerScroll = findViewById(R.id.header_scroll)
        categoriesToolbar = findViewById(R.id.categories_toolbar)
        expensesContainer = findViewById(R.id.expenses_container)

        loadUserProfile()
    }

    private fun loadUserProfile() {
        val userId = userPrefs.getCurrentUserId()
        if (userId != -1) {
            lifecycleScope.launch {
                val user = database.userDao().getUserById(userId)
                user?.let {
                    tvUserName.text = it.fullName ?: "User"

                    if (it.profilePhotoPath != null) {
                        val file = File(it.profilePhotoPath)
                        if (file.exists()) {
                            val uri: Uri = FileProvider.getUriForFile(
                                this@MainActivity,
                                "${packageName}.fileprovider",
                                file
                            )
                            ivProfileIcon.setImageURI(uri)
                            ivProfileIcon.imageTintList = null
                            ivProfileIcon.scaleType = ImageView.ScaleType.CENTER_CROP
                        } else {
                            ivProfileIcon.setImageResource(R.drawable.ic_profile_icon)
                            ivProfileIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                        }
                    } else {
                        ivProfileIcon.setImageResource(R.drawable.ic_profile_icon)
                        ivProfileIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
                    }
                }
            }
        } else {
            tvUserName.text = viewModel.getUserName()
            ivProfileIcon.setImageResource(R.drawable.ic_profile_icon)
            ivProfileIcon.scaleType = ImageView.ScaleType.CENTER_INSIDE
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
                        performLogout()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun performLogout() {
        Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()
        
        // Clear repository caches
        lifecycleScope.launch(Dispatchers.IO) {
            (MyApplication.categoryRepository as? com.example.auto_saver.data.repository.FirestoreFirstCategoryRepository)?.clearCache()
            (MyApplication.expenseRepository as? com.example.auto_saver.data.repository.FirestoreFirstExpenseRepository)?.clearCache()
            (MyApplication.goalRepository as? com.example.auto_saver.data.repository.FirestoreFirstGoalRepository)?.clearCache()
        }
        
        // Sign out from Firebase
        firebaseAuth.signOut()
        
        // Clear user preferences
        userPrefs.clearSession()
        
        // Navigate to login
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun toggleTheme() {
        val isDarkMode = userPrefs.isDarkModeEnabled()
        userPrefs.setDarkMode(!isDarkMode)

        AppCompatDelegate.setDefaultNightMode(
            if (!isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
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
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Resetting database...",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }

                firebaseAuth.signOut()
                userPrefs.clearSession()

                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Database reset successfully",
                        Toast.LENGTH_LONG
                    ).show()

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
            val uid = userPrefs.getCurrentUserUid()
            if (uid.isNullOrBlank()) {
                Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showAddPopupMenu()
        }
    }

    private fun setupRecyclerViews() {
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
        val categoryId = categoryHeader.category.id.toString()
        if (expandedCategories.contains(categoryId)) {
            expandedCategories.remove(categoryId)
        } else {
            expandedCategories.add(categoryId)
        }
        updateExpenseList()
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

    private fun setupViewToggle() {
        viewToggleGroup.check(R.id.btn_view_dashboard)
        applyViewMode(ViewMode.DASHBOARD)

        viewToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val mode = if (checkedId == R.id.btn_view_categories) {
                ViewMode.CATEGORIES
            } else {
                ViewMode.DASHBOARD
            }
            applyViewMode(mode)
        }
    }

    private fun applyViewMode(mode: ViewMode) {
        currentViewMode = mode
        when (mode) {
            ViewMode.DASHBOARD -> {
                headerScroll.visibility = View.VISIBLE
                categoriesToolbar.visibility = View.GONE
                expensesContainer.visibility = View.GONE
            }
            ViewMode.CATEGORIES -> {
                headerScroll.visibility = View.GONE
                categoriesToolbar.visibility = View.VISIBLE
                expensesContainer.visibility = View.VISIBLE
            }
        }
    }

    private fun observeViewModel() {
        // Observe expenses
        lifecycleScope.launch {
            viewModel.expenses.collectLatest { expenses ->
                updateExpenseList(expenses)
            }
        }

        // Observe total spent
        lifecycleScope.launch {
            viewModel.totalSpent.collectLatest { total ->
                tvTotalSpent.text = getString(R.string.currency_format, total)
            }
        }

        // Observe categories for caching
        lifecycleScope.launch {
            viewModel.categories.collectLatest { categories ->
                categoryCache.clear()
                categories.forEach { categoryCache[it.id] = it }
            }
        }

        // Observe spending summary for goal progress
        lifecycleScope.launch {
            viewModel.spendingSummary.collectLatest { summary ->
                summary?.let { updateGoalProgress(it) }
            }
        }
    }

    private fun updateExpenseList(expenses: List<ExpenseRecord> = viewModel.expenses.value) {
        val categories = viewModel.categories.value

        if (expenses.isEmpty() && categories.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            rvExpenses.visibility = View.GONE
            tvExpenseCount.text = "0 expenses"
        } else {
            emptyStateLayout.visibility = View.GONE
            rvExpenses.visibility = View.VISIBLE

            // Group expenses by category
            val expensesByCategory = expenses.groupBy { it.categoryId }
            val listItems = mutableListOf<ExpenseListItem>()

            // Process each category
            categories.forEach { category ->
                val categoryExpenses = expensesByCategory[category.id] ?: emptyList()

                val isExpanded = expandedCategories.contains(category.id)
                listItems.add(
                    ExpenseListItem.CategoryHeader(
                        category = Category(
                            id = 0,
                            userId = 0,
                            name = category.name
                        ),
                        expenses = categoryExpenses.map { exp ->
                            Expense(
                                id = 0,
                                userId = 0,
                                categoryId = 0,
                                amount = exp.amount,
                                description = exp.description,
                                date = exp.date,
                                startTime = exp.startTime,
                                endTime = exp.endTime,
                                photoPath = exp.photoPath
                            )
                        },
                        isExpanded = isExpanded
                    )
                )

                if (isExpanded) {
                    categoryExpenses.forEach { expense ->
                        listItems.add(
                            ExpenseListItem.ExpenseItem(
                                expense = Expense(
                                    id = 0,
                                    userId = 0,
                                    categoryId = 0,
                                    amount = expense.amount,
                                    description = expense.description,
                                    date = expense.date,
                                    startTime = expense.startTime,
                                    endTime = expense.endTime,
                                    photoPath = expense.photoPath
                                ),
                                categoryName = category.name
                            )
                        )
                    }
                }
            }

            groupedExpenseAdapter.submitList(listItems)

            val count = expenses.size
            tvExpenseCount.text = if (count == 1) "1 expense" else "$count expenses"
        }
    }

    private fun updateGoalProgress(summary: SpendingSummary) {
        tvCurrentSpent.text = String.format("$%.2f", summary.totalSpent)

        if (summary.goalMax != null && summary.goalMin != null) {
            tvMinGoal.text = String.format("$%.2f", summary.goalMin)
            tvMaxGoal.text = String.format("$%.2f", summary.goalMax)

            val progress = summary.percentageOfGoal.toInt().coerceIn(0, 100)
            progressGoal.progress = progress

            tvGoalStatus.text = when {
                summary.isOverBudget -> "⚠️ Over budget by $${String.format("%.2f", summary.totalSpent - summary.goalMax)}"
                summary.totalSpent < summary.goalMin -> "Below minimum goal"
                else -> "✓ On track! Keep it up"
            }

            tvGoalStatus.setTextColor(
                getColor(when {
                    summary.isOverBudget -> R.color.error_red
                    summary.totalSpent < summary.goalMin -> R.color.goal_warning
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

    private fun showFilterOptions() {
        val options = arrayOf("All Time", "This Month", "Last Month", "Custom Range", "Clear Filter")

        AlertDialog.Builder(this)
            .setTitle("Filter Expenses")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        viewModel.updateDateRange("1900-01-01", "2100-12-31")
                        btnFilter.text = "All Time"
                    }
                    1 -> {
                        viewModel.setThisMonth()
                        btnFilter.text = "This Month"
                    }
                    2 -> {
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.MONTH, -1)
                        calendar.set(Calendar.DAY_OF_MONTH, 1)
                        val start = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                        
                        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                        val end = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                        
                        viewModel.updateDateRange(start, end)
                        btnFilter.text = "Last Month"
                    }
                    3 -> showCustomDateRange()
                    4 -> {
                        viewModel.setThisMonth()
                        btnFilter.text = "Filter"
                    }
                }
            }
            .show()
    }

    private fun showCustomDateRange() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(this, { _, year, month, day ->
            calendar.set(year, month, day)
            val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

            DatePickerDialog(this, { _, endYear, endMonth, endDay ->
                calendar.set(endYear, endMonth, endDay)
                val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                viewModel.updateDateRange(startDate, endDate)
                btnFilter.text = "Custom"
            }, year, month, day).show()

        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun showAddPopupMenu() {
        val popup = PopupMenu(this, fabAdd)
        popup.menuInflater.inflate(R.menu.add_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add_expense -> {
                    val intent = Intent(this, AddExpenseActivity::class.java)
                    startActivity(intent)
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
            val uid = try {
                userPrefs.requireUserUid()
            } catch (e: IllegalStateException) {
                Toast.makeText(this@MainActivity, "Please log in first", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val result = MyApplication.categoryRepository.createCategory(uid, categoryName)

            result.onSuccess {
                Toast.makeText(
                    this@MainActivity,
                    "Category created successfully",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure { error ->
                val message = when {
                    error.message?.contains("already exists", ignoreCase = true) == true ->
                        "Category '$categoryName' already exists"
                    else -> "Failed to create category: ${error.message}"
                }
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRemoveCategoriesDialog() {
        lifecycleScope.launch {
            val categories = viewModel.categories.value
            val expenses = viewModel.expenses.value

            if (categories.isEmpty()) {
                Toast.makeText(
                    this@MainActivity,
                    "No categories to remove",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

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

    private fun confirmCategoryDeletion(category: CategoryRecord, expenseCount: Int) {
        val message = if (expenseCount > 0) {
            "Are you sure you want to delete '${category.name}'?\n\n⚠️ WARNING: This category has $expenseCount expense(s) associated with it. All these expenses will also be permanently deleted!\n\nThis action cannot be undone."
        } else {
            "Are you sure you want to delete '${category.name}'?\n\nThis action cannot be undone."
        }

        AlertDialog.Builder(this)
            .setTitle("Confirm Deletion")
            .setMessage(message)
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun deleteCategory(category: CategoryRecord) {
        lifecycleScope.launch {
            try {
                val uid = userPrefs.requireUserUid()
                
                // Delete category through repository
                val result = MyApplication.categoryRepository.deleteCategory(uid, category.id)
                
                result.onSuccess {
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Category '${category.name}' deleted successfully",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }.onFailure { error ->
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Error deleting category: ${error.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
}
