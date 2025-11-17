package com.example.auto_saver.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.auto_saver.data.model.ExpenseRecord
import com.example.auto_saver.ui.components.GraphMetric
import com.example.auto_saver.ui.components.GraphRenderData
import java.io.File
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Utility for exporting spending data to CSV format
 */
object CSVExportHelper {

    /**
     * Export graph data to CSV and return a shareable URI
     */
    fun exportGraphDataToCSV(
        context: Context,
        data: GraphRenderData,
        expenses: List<ExpenseRecord>
    ): Intent? {
        try {
            val fileName = "spending_export_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            
            FileWriter(file).use { writer ->
                // Write header
                writer.append("Date,Amount,Category,Description\n")
                
                // Write expense data
                expenses.sortedBy { it.date }.forEach { expense ->
                    writer.append("${expense.date},")
                    writer.append("${expense.amount},")
                    writer.append("${expense.categoryId},")
                    writer.append("\"${expense.description?.replace("\"", "\"\"") ?: ""}\"\n")
                }
                
                // Add summary section
                writer.append("\n")
                writer.append("Summary\n")
                writer.append("Date Range,${data.rangeLabel}\n")
                writer.append("Total Spent,${data.totalSpent}\n")
                writer.append("Previous Period,${data.comparisonTotal}\n")
                writer.append("Metric Type,${data.metric.name}\n")
                if (data.goalMax != null) {
                    writer.append("Budget Goal,${data.goalMax}\n")
                    writer.append("Over Budget,${if (data.isOverBudget) "Yes" else "No"}\n")
                }
            }
            
            // Create share intent
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            return Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Spending Report")
                putExtra(Intent.EXTRA_TEXT, "Spending data from ${data.rangeLabel}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Export aggregated spending data to CSV
     */
    fun exportAggregatedDataToCSV(
        context: Context,
        aggregatedData: Map<String, Double>,
        metric: GraphMetric,
        dateRange: String
    ): Intent? {
        try {
            val fileName = "aggregated_spending_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            
            FileWriter(file).use { writer ->
                // Write header
                writer.append("Period,Amount\n")
                
                // Write aggregated data
                aggregatedData.forEach { (period, amount) ->
                    writer.append("$period,$amount\n")
                }
                
                // Add summary
                writer.append("\n")
                writer.append("Summary\n")
                writer.append("Date Range,$dateRange\n")
                writer.append("Metric Type,${metric.name}\n")
                writer.append("Total,${aggregatedData.values.sum()}\n")
                writer.append("Average,${aggregatedData.values.average()}\n")
                writer.append("Max,${aggregatedData.values.maxOrNull() ?: 0.0}\n")
                writer.append("Min,${aggregatedData.values.minOrNull() ?: 0.0}\n")
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            return Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Spending Report - $dateRange")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Export category breakdown to CSV
     */
    fun exportCategoryBreakdownToCSV(
        context: Context,
        categoryTotals: Map<String, SpendingAggregator.CategoryTotal>,
        dateRange: String
    ): Intent? {
        try {
            val fileName = "category_breakdown_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            
            FileWriter(file).use { writer ->
                // Write header
                writer.append("Category ID,Total Amount,Expense Count,Average per Expense\n")
                
                // Write category data
                categoryTotals.values.sortedByDescending { it.total }.forEach { category ->
                    val avgPerExpense = if (category.expenseCount > 0) {
                        category.total / category.expenseCount
                    } else 0.0
                    
                    writer.append("${category.categoryId},")
                    writer.append("${category.total},")
                    writer.append("${category.expenseCount},")
                    writer.append("$avgPerExpense\n")
                }
                
                // Add summary
                writer.append("\n")
                writer.append("Summary\n")
                writer.append("Date Range,$dateRange\n")
                writer.append("Total Categories,${categoryTotals.size}\n")
                writer.append("Grand Total,${categoryTotals.values.sumOf { it.total }}\n")
                writer.append("Total Expenses,${categoryTotals.values.sumOf { it.expenseCount }}\n")
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            return Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Category Breakdown - $dateRange")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Format date for CSV export
     */
    private fun formatDate(dateString: String): String {
        return try {
            val date = LocalDate.parse(dateString)
            date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            dateString
        }
    }
}