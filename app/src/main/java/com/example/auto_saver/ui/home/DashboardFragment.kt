package com.example.auto_saver.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.auto_saver.AddExpenseActivity
import com.example.auto_saver.CategoryAnalyticsActivity
import com.example.auto_saver.GoalsActivity
import com.example.auto_saver.GraphActivity
import com.example.auto_saver.MyApplication
import com.example.auto_saver.R
import com.example.auto_saver.collectWithLifecycle
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import java.time.LocalDate

class DashboardFragment : Fragment() {

    private val dashboardViewModel: DashboardViewModel by viewModels {
        // Reuse application-scoped repositories and preferences
        @Suppress("UNUSED_VARIABLE")
        val app = requireActivity().application as MyApplication
        DashboardViewModelFactory(
            expenseRepository = MyApplication.expenseRepository,
            goalRepository = MyApplication.goalRepository,
            userPreferences = MyApplication.userPreferences
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chart = view.findViewById<LineChart>(R.id.chart_spending)
        val totalSpentText = view.findViewById<TextView>(R.id.tv_total_spent_value)
        val expenseCountText = view.findViewById<TextView>(R.id.tv_expense_count_value)
        val goalProgressText = view.findViewById<TextView>(R.id.tv_goal_progress_value)

        val graphCard = view.findViewById<MaterialCardView>(R.id.card_spending_graph)
        val btnViewDetails = view.findViewById<MaterialButton>(R.id.btn_view_details)
        val rangeToggle = view.findViewById<MaterialButtonToggleGroup>(R.id.range_toggle)
        val btnManageGoals = view.findViewById<MaterialButton>(R.id.btn_manage_goals)
        val btnViewAnalytics = view.findViewById<MaterialButton>(R.id.btn_view_analytics)
        val btnQuickAddExpense = view.findViewById<MaterialButton>(R.id.btn_quick_add_expense)

        configureChartAppearance(chart)

        // Default to 30-day range to match ViewModel initial state
        rangeToggle.check(R.id.btn_range_30)

        rangeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btn_range_7 -> dashboardViewModel.setLast7Days()
                R.id.btn_range_30 -> dashboardViewModel.setLast30Days()
                R.id.btn_range_90 -> dashboardViewModel.setLast90Days()
            }
        }

        dashboardViewModel.totalSpent.collectWithLifecycle(viewLifecycleOwner) { total ->
            totalSpentText.text = getString(R.string.currency_format, total)
        }

        dashboardViewModel.expenseCount.collectWithLifecycle(viewLifecycleOwner) { count ->
            val label = resources.getQuantityString(
                R.plurals.expense_count,
                count,
                count
            )
            expenseCountText.text = label
        }

        dashboardViewModel.spendingSummary.collectWithLifecycle(viewLifecycleOwner) { summary ->
            if (summary == null) {
                goalProgressText.text = getString(R.string.no_goals_set)
            } else {
                val percentage = summary.percentageOfGoal.toInt()
                goalProgressText.text = getString(
                    R.string.goal_progress_format,
                    percentage
                )
            }
        }

        graphCard.setOnClickListener {
            startActivity(Intent(requireContext(), GraphActivity::class.java))
        }

        btnViewDetails.setOnClickListener {
            startActivity(Intent(requireContext(), GraphActivity::class.java))
        }

        btnManageGoals.setOnClickListener {
            startActivity(Intent(requireContext(), GoalsActivity::class.java))
        }

        btnViewAnalytics.setOnClickListener {
            startActivity(Intent(requireContext(), CategoryAnalyticsActivity::class.java))
        }

        btnQuickAddExpense.setOnClickListener {
            startActivity(Intent(requireContext(), AddExpenseActivity::class.java))
        }

        dashboardViewModel.dailySpending.collectWithLifecycle(viewLifecycleOwner) { points ->
            updateChart(chart, points)
        }
    }

    private fun configureChartAppearance(chart: LineChart) {
        chart.description.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.axisRight.isEnabled = false
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.setDrawGridLines(false)
        chart.axisLeft.setDrawGridLines(true)
        chart.legend.isEnabled = false
    }

    private fun updateChart(chart: LineChart, points: List<com.example.auto_saver.DailySpendingPoint>) {
        if (points.isEmpty()) {
            chart.data = null
            chart.invalidate()
            return
        }

        val entries = points.mapIndexed { index, point ->
            Entry(index.toFloat(), point.total.toFloat())
        }

        val primaryColor = resolveThemeColor(chart, androidx.appcompat.R.attr.colorPrimary)

        val dataSet = LineDataSet(entries, "").apply {
            color = primaryColor
            setCircleColor(primaryColor)
            lineWidth = 2f
            circleRadius = 3f
            setDrawValues(false)
            setDrawFilled(false)
        }

        chart.data = LineData(dataSet)

        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: com.github.mikephil.charting.components.AxisBase?): String {
                val index = value.toInt()
                if (index < 0 || index >= points.size) return ""
                val date = try {
                    LocalDate.parse(points[index].date)
                } catch (e: Exception) {
                    return ""
                }

                val size = points.size
                return when {
                    size <= 10 -> date.dayOfMonth.toString()
                    index == 0 || index == size / 2 || index == size - 1 -> date.dayOfMonth.toString()
                    else -> ""
                }
            }
        }

        chart.invalidate()
    }

    private fun resolveThemeColor(view: View, attrResId: Int): Int {
        val typedValue = TypedValue()
        val theme = view.context.theme
        theme.resolveAttribute(attrResId, typedValue, true)
        return typedValue.data
    }
}
