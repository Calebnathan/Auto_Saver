package com.example.auto_saver

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GraphActivity : AppCompatActivity() {

    private lateinit var startDateButton: MaterialButton
    private lateinit var endDateButton: MaterialButton
    private lateinit var selectedRangeText: TextView
    private lateinit var barChart: BarChart
    private lateinit var userPrefs: UserPreferences
    
    private val expenseRepository by lazy { MyApplication.expenseRepository }
    private val goalRepository by lazy { MyApplication.goalRepository }
    private val categoryRepository by lazy { MyApplication.categoryRepository }

    private var startDate: String? = null
    private var endDate: String? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        userPrefs = MyApplication.userPreferences

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        startDateButton = findViewById(R.id.startDateButton)
        endDateButton = findViewById(R.id.endDateButton)
        selectedRangeText = findViewById(R.id.tv_selected_range)
        barChart = findViewById(R.id.barChart)

        startDateButton.setOnClickListener { pickDate(isStart = true) }
        endDateButton.setOnClickListener { pickDate(isStart = false) }
    }

    private fun pickDate(isStart: Boolean) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val pickedDate = Calendar.getInstance()
            pickedDate.set(selectedYear, selectedMonth, selectedDay)
            val formattedDate = dateFormat.format(pickedDate.time)

            if (isStart) {
                startDate = formattedDate
                startDateButton.text = formattedDate
            } else {
                endDate = formattedDate
                endDateButton.text = formattedDate
            }

            updateSelectedRangeLabel()

            if (startDate != null && endDate != null) {
                fetchDataAndDrawChart()
            }

        }, year, month, day)

        datePicker.show()
    }

    private fun fetchDataAndDrawChart() {
        val uid = try {
            userPrefs.requireUserUid()
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val start = startDate ?: return
        val end = endDate ?: return

        updateSelectedRangeLabel()

        lifecycleScope.launch {
            try {
                // Get category summaries for the date range
                val summaries = expenseRepository.getCategorySummaries(uid, start, end).first()

                if (summaries.isEmpty()) {
                    Toast.makeText(
                        this@GraphActivity,
                        "No data for selected period",
                        Toast.LENGTH_SHORT
                    ).show()
                    barChart.clear()
                    return@launch
                }

                // Get current month goal for limit lines
                val currentMonth = java.time.YearMonth.now().toString()
                val goal = goalRepository.getGoalForMonth(uid, currentMonth)

                drawBarChart(summaries.associate { it.categoryName to it.total }, goal)
            } catch (e: Exception) {
                Toast.makeText(
                    this@GraphActivity,
                    "Error fetching data: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun drawBarChart(categoryTotals: Map<String, Double>, goal: com.example.auto_saver.data.model.GoalRecord?) {
        val entries = ArrayList<BarEntry>()
        val categories = ArrayList<String>()

        var index = 0f
        for ((category, total) in categoryTotals) {
            entries.add(BarEntry(index, total.toFloat()))
            categories.add(category)
            index++
        }

        val dataSet = BarDataSet(entries, "Amount Spent by Category")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val barData = BarData(dataSet)
        barChart.data = barData

        val yAxis = barChart.axisLeft
        yAxis.removeAllLimitLines()

        // Add goal lines if goal exists
        goal?.let {
            val minGoal = LimitLine(it.minGoal.toFloat(), "Min Goal")
            minGoal.lineColor = getColor(android.R.color.holo_green_dark)
            minGoal.lineWidth = 2f

            val maxGoal = LimitLine(it.maxGoal.toFloat(), "Max Goal")
            maxGoal.lineColor = getColor(android.R.color.holo_red_dark)
            maxGoal.lineWidth = 2f

            yAxis.addLimitLine(minGoal)
            yAxis.addLimitLine(maxGoal)
        }

        barChart.axisRight.isEnabled = false
        barChart.xAxis.granularity = 1f
        barChart.description.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun updateSelectedRangeLabel() {
        val start = startDate
        val end = endDate
        if (start.isNullOrEmpty() || end.isNullOrEmpty()) {
            selectedRangeText.text = ""
        } else {
            selectedRangeText.text = getString(R.string.select_date_range, start, end)
        }
    }
}
