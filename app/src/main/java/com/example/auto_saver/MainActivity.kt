package com.example.auto_saver

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
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

    private lateinit var database: AppDatabase
    private lateinit var userPrefs: UserPreferences
    private lateinit var groupedExpenseAdapter: GroupedExpenseAdapter

    private val categoryCache = mutableMapOf<Int, String>()
    private val expandedCategories = mutableSetOf<Int>() // Track which categories are expanded

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
        loadCategories()
    }

    override fun onResume() {
        super.onResume()
        // Refresh when returning from add screens
        loadCategories()
        loadExpenses()
    }

    private fun initializeViews() {
        fabMenu = findViewById(R.id.fab_menu)
        fabAdd = findViewById(R.id.fab_add)
        tvUserName = findViewById(R.id.tv_user_name)
        rvExpenses = findViewById(R.id.rv_expenses)
        tvTotalSpent = findViewById(R.id.tv_total_spent)
        tvExpenseCount = findViewById(R.id.tv_expense_count)
        emptyStateLayout = findViewById(R.id.empty_state_layout)

        // Load user's name in profile card
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

    private fun showAddPopupMenu() {
        val popup = PopupMenu(this, fabAdd)
        popup.menuInflater.inflate(R.menu.add_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_add_expense -> {
                    // Show Add Expense Dialog
                    showAddExpenseDialog()
                    true
                }
                R.id.action_create_category -> {
                    // Show Create Category Dialog
                    showAddCategoryDialog()
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

    private fun showDeleteCategoryDialog(category: Category) {
        AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete '${category.name}'?\n\nNote: This will only delete the category if it's not being used by any expenses.")
            .setPositiveButton("Delete") { _, _ ->
                deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCategory(category: Category) {
        lifecycleScope.launch {
            // Check if category is being used by any expenses
            val expenses = database.expenseDao().getExpensesByUser(userPrefs.getCurrentUserId())
            val isUsed = expenses.any { it.categoryId == category.id }

            if (isUsed) {
                runOnUiThread {
                    Toast.makeText(
                        this@MainActivity,
                        "Cannot delete category '${category.name}' - it's being used by expenses",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return@launch
            }

            database.categoryDao().deleteCategory(category)
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Category deleted", Toast.LENGTH_SHORT).show()
                loadCategories() // Refresh the list
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

            if (expenses.isEmpty() && categories.isEmpty()) {
                emptyStateLayout.visibility = View.VISIBLE
                rvExpenses.visibility = View.GONE
                tvTotalSpent.text = "$0.00"
                tvExpenseCount.text = "0 expenses"
            } else {
                emptyStateLayout.visibility = View.GONE
                rvExpenses.visibility = View.VISIBLE

                // Group expenses by category
                val expensesByCategory = expenses.groupBy { it.categoryId }
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
                val orphanedExpenses = expenses.filter { expense ->
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
                val total = expenses.sumOf { it.amount }
                tvTotalSpent.text = getString(R.string.currency_format, total)

                val count = expenses.size
                tvExpenseCount.text = if (count == 1) "1 expense" else "$count expenses"
            }
        }
    }
}
