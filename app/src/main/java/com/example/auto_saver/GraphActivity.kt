package com.example.auto_saver

import android.app.DatePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.auto_saver.DateRange
import com.example.auto_saver.ui.components.GraphMetric
import com.example.auto_saver.ui.components.GraphRenderData
import com.example.auto_saver.ui.components.GraphUiState
import com.example.auto_saver.ui.components.SpendingGraphView
import com.example.auto_saver.ui.graph.GraphViewModel
import com.example.auto_saver.ui.graph.GraphViewModelFactory
import com.example.auto_saver.utils.CSVExportHelper
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class GraphActivity : AppCompatActivity() {

    private val graphViewModel: GraphViewModel by viewModels {
        GraphViewModelFactory(
            expenseRepository = MyApplication.expenseRepository,
            goalRepository = MyApplication.goalRepository,
            userPreferences = MyApplication.userPreferences
        )
    }

    private lateinit var startDateButton: MaterialButton
    private lateinit var endDateButton: MaterialButton
    private lateinit var selectedRangeText: TextView
    private lateinit var metricToggle: MaterialButtonToggleGroup
    private lateinit var graphRangeLabel: TextView
    private lateinit var comparisonSummary: TextView
    private lateinit var shareButton: MaterialButton
    private lateinit var exportCsvButton: MaterialButton
    private lateinit var graphView: SpendingGraphView
    private lateinit var legendCurrentIndicator: View
    private lateinit var legendPreviousIndicator: View

    private var lastRenderData: GraphRenderData? = null
    private var latestRange: DateRange? = null
    private var latestExpenses: List<com.example.auto_saver.data.model.ExpenseRecord> = emptyList()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_graph)

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        startDateButton = findViewById(R.id.startDateButton)
        endDateButton = findViewById(R.id.endDateButton)
        selectedRangeText = findViewById(R.id.tv_selected_range)
        metricToggle = findViewById(R.id.metric_toggle_full)
        graphRangeLabel = findViewById(R.id.tv_graph_range_label)
        comparisonSummary = findViewById(R.id.tv_comparison_summary)
        shareButton = findViewById(R.id.btn_share_graph)
        exportCsvButton = findViewById(R.id.btn_export_csv)
        graphView = findViewById(R.id.view_full_graph)
        legendCurrentIndicator = findViewById(R.id.legend_current_indicator)
        legendPreviousIndicator = findViewById(R.id.legend_previous_indicator)

        startDateButton.setOnClickListener { pickDate(isStart = true) }
        endDateButton.setOnClickListener { pickDate(isStart = false) }

        metricToggle.check(R.id.btn_metric_daily_full)
        metricToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val metric = when (checkedId) {
                R.id.btn_metric_daily_full -> GraphMetric.DAILY
                R.id.btn_metric_weekly_full -> GraphMetric.WEEKLY
                R.id.btn_metric_monthly_full -> GraphMetric.MONTHLY
                R.id.btn_metric_yearly_full -> GraphMetric.YEARLY
                else -> GraphMetric.DAILY
            }
            graphViewModel.setMetric(metric)
        }

        shareButton.setOnClickListener { shareSnapshot() }
        exportCsvButton.setOnClickListener { exportToCSV() }

        tintLegendIndicators()
        observeViewModel()
    }

    private fun tintLegendIndicators() {
        val currentColor = resolveThemeColor(androidx.appcompat.R.attr.colorPrimary)
        val previousColor = resolveThemeColor(androidx.appcompat.R.attr.colorAccent)
        ViewCompat.setBackgroundTintList(legendCurrentIndicator, ColorStateList.valueOf(currentColor))
        ViewCompat.setBackgroundTintList(legendPreviousIndicator, ColorStateList.valueOf(previousColor))
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                graphViewModel.expenses.collect { expenses ->
                    latestExpenses = expenses
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                graphViewModel.dateRange.collect { range ->
                    latestRange = range
                    startDateButton.text = range.start
                    endDateButton.text = range.end
                    if (lastRenderData == null) {
                        selectedRangeText.text = getString(
                            R.string.graph_range_short_format,
                            range.start,
                            range.end
                        )
                    }
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                graphViewModel.graphState.collect { state ->
                    graphView.setState(state)
                    when (state) {
                        is GraphUiState.Success -> {
                            lastRenderData = state.data
                            val formattedRange = state.data.rangeLabel
                            selectedRangeText.text = formattedRange
                            graphRangeLabel.text = formattedRange
                            val current = getString(R.string.currency_format, state.data.totalSpent)
                            val previous = getString(R.string.currency_format, state.data.comparisonTotal)
                            comparisonSummary.text = getString(R.string.graph_comparison_format, current, previous)
                            shareButton.isEnabled = true
                            exportCsvButton.isEnabled = true
                        }
                        GraphUiState.Empty -> {
                            lastRenderData = null
                            graphRangeLabel.text = getString(R.string.graph_empty_state)
                            comparisonSummary.text = getString(R.string.graph_no_data_message)
                            shareButton.isEnabled = false
                            exportCsvButton.isEnabled = false
                            latestRange?.let {
                                selectedRangeText.text = getString(
                                    R.string.graph_range_short_format,
                                    it.start,
                                    it.end
                                )
                            }
                        }
                        is GraphUiState.Error -> {
                            lastRenderData = null
                            graphRangeLabel.text = getString(R.string.graph_error_state)
                            comparisonSummary.text = getString(R.string.graph_error_state)
                            shareButton.isEnabled = false
                            exportCsvButton.isEnabled = false
                            Toast.makeText(this@GraphActivity, state.message, Toast.LENGTH_SHORT).show()
                            latestRange?.let {
                                selectedRangeText.text = getString(
                                    R.string.graph_range_short_format,
                                    it.start,
                                    it.end
                                )
                            }
                        }
                        GraphUiState.Loading -> {
                            graphRangeLabel.text = getString(R.string.spending_graph_subtitle)
                            shareButton.isEnabled = false
                            exportCsvButton.isEnabled = false
                            comparisonSummary.text = getString(R.string.graph_loading_message)
                            latestRange?.let {
                                selectedRangeText.text = getString(
                                    R.string.graph_range_short_format,
                                    it.start,
                                    it.end
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun pickDate(isStart: Boolean) {
        val calendar = Calendar.getInstance()
        val dateString = if (isStart) startDateButton.text?.toString() else endDateButton.text?.toString()
        if (!dateString.isNullOrEmpty()) {
            val parsed = runCatching { dateFormat.parse(dateString) }.getOrNull()
            if (parsed != null) {
                calendar.time = parsed
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val pickedDate = Calendar.getInstance()
            pickedDate.set(selectedYear, selectedMonth, selectedDay)
            val formattedDate = dateFormat.format(pickedDate.time)
            if (isStart) {
                val end = latestRange?.end ?: formattedDate
                graphViewModel.setDateRange(formattedDate, end)
            } else {
                val start = latestRange?.start ?: formattedDate
                graphViewModel.setDateRange(start, formattedDate)
            }
        }, year, month, day)

        datePicker.show()
    }

    private fun shareSnapshot() {
        val data = lastRenderData ?: run {
            Toast.makeText(this, R.string.graph_share_no_data, Toast.LENGTH_SHORT).show()
            return
        }

        val body = getString(
            R.string.graph_share_body,
            data.rangeLabel,
            getString(R.string.currency_format, data.totalSpent),
            getString(R.string.currency_format, data.comparisonTotal)
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, getString(R.string.graph_share_subject))
            putExtra(Intent.EXTRA_TEXT, body)
        }

        startActivity(Intent.createChooser(intent, getString(R.string.graph_share_subject)))
    }

    private fun exportToCSV() {
        val data = lastRenderData ?: run {
            Toast.makeText(this, R.string.graph_share_no_data, Toast.LENGTH_SHORT).show()
            return
        }

        val intent = CSVExportHelper.exportGraphDataToCSV(
            context = this,
            data = data,
            expenses = latestExpenses
        )

        if (intent != null) {
            startActivity(Intent.createChooser(intent, "Export CSV"))
        } else {
            Toast.makeText(this, "Failed to export CSV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveThemeColor(attr: Int): Int {
        val typedArray = obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()
        return color
    }
}