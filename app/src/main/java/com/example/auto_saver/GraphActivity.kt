package com.example.auto_saver

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Button
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color

data class ChartExpense(val amount: Double, val date: String, val categoryId: Int)

class GraphActivity : AppCompatActivity() {

    private lateinit var startDateButton: Button
    private lateinit var endDateButton: Button
    private lateinit var barChart: BarChart
    private val db = FirebaseFirestore.getInstance()

    private var startDate: Date? = null
    private var endDate: Date? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        startDateButton = findViewById(R.id.startDateButton)
        endDateButton = findViewById(R.id.endDateButton)
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
            val date = pickedDate.time
            val formattedDate = dateFormat.format(date)

            if (isStart) {
                startDate = date
                startDateButton.text = formattedDate
            } else {
                endDate = date
                endDateButton.text = formattedDate
            }

            if (startDate != null && endDate != null) {
                fetchDataAndDrawChart()
            }

        }, year, month, day)

        datePicker.show()
    }

    private fun fetchDataAndDrawChart() {
        val masterId = "xJHQxxUiDtUfOLv7NGza" // your master_table document ID
        val userId = "BWW8S82t1ChGAwPRaT" // your test user's document ID

        db.collection("master_table")
            .document(masterId)
            .collection("user_table")
            .document(userId)
            .collection("expense_table")
            .get()
            .addOnSuccessListener { result ->
                val expenseList = mutableListOf<ChartExpense>()
                for (document in result) {
                    val amount = document.getDouble("amount") ?: 0.0
                    val date = document.getString("date") ?: ""
                    val categoryId = document.getLong("category_id")?.toInt() ?: 0

                    expenseList.add(ChartExpense(amount, date, categoryId))
                }

                updateBarChart(expenseList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun drawBarChart(categoryTotals: Map<String, Double>) {
        val entries = ArrayList<BarEntry>()
        val categories = ArrayList<String>()

        var index = 0f
        for ((category, total) in categoryTotals) {
            entries.add(BarEntry(index, total.toFloat()))
            categories.add(category)
            index++
        }

        val dataSet = BarDataSet(entries, "Amount Spent")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()

        val barData = BarData(dataSet)
        barChart.data = barData

        // Add goal lines (example: 1000 min, 5000 max)
        val minGoal = LimitLine(1000f, "Min Goal")
        minGoal.lineColor = getColor(android.R.color.holo_green_dark)
        minGoal.lineWidth = 2f

        val maxGoal = LimitLine(5000f, "Max Goal")
        maxGoal.lineColor = getColor(android.R.color.holo_red_dark)
        maxGoal.lineWidth = 2f

        val yAxis = barChart.axisLeft
        yAxis.removeAllLimitLines()
        yAxis.addLimitLine(minGoal)
        yAxis.addLimitLine(maxGoal)

        barChart.axisRight.isEnabled = false
        barChart.xAxis.granularity = 1f
        barChart.description.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun updateBarChart(expenses: List<ChartExpense>) {
        if (expenses.isEmpty()) {
            Toast.makeText(this, "No data for selected period", Toast.LENGTH_SHORT).show()
            barChart.clear()
            return
        }

        val entries = expenses.mapIndexed { index, expense ->
            BarEntry(index.toFloat(), expense.amount.toFloat())
        }

        val dataSet = BarDataSet(entries, "Expenses")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
        dataSet.valueTextColor = Color.WHITE
        dataSet.valueTextSize = 12f

        val data = BarData(dataSet)
        barChart.data = data

        val yAxis = barChart.axisLeft
        yAxis.removeAllLimitLines()
        val maxGoal = LimitLine(5000f, "Max Goal")
        maxGoal.lineColor = Color.RED
        yAxis.addLimitLine(maxGoal)

        barChart.axisRight.isEnabled = false
        barChart.xAxis.isEnabled = false
        barChart.description.isEnabled = false
        barChart.animateY(1000)
        barChart.invalidate()
    }
}
