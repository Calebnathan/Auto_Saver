package com.example.auto_saver.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.core.graphics.ColorUtils
import com.example.auto_saver.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.progressindicator.CircularProgressIndicator

class SpendingGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val chart: LineChart
    private val loadingIndicator: CircularProgressIndicator
    private val messageView: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.view_spending_graph, this, true)
        chart = findViewById(R.id.graph_chart)
        loadingIndicator = findViewById(R.id.graph_loading)
        messageView = findViewById(R.id.graph_message)
        configureChart()
    }

    fun setState(state: GraphUiState) {
        when (state) {
            GraphUiState.Loading -> showLoading()
            GraphUiState.Empty -> showMessage(resources.getString(R.string.graph_empty_state))
            is GraphUiState.Error -> showMessage(state.message)
            is GraphUiState.Success -> renderData(state.data)
        }
    }

    private fun renderData(renderData: GraphRenderData) {
        loadingIndicator.visibility = View.GONE
        messageView.visibility = View.GONE
        chart.visibility = View.VISIBLE

        val lineData = LineData().apply {
            addDataSet(createPrimaryDataSet(renderData))
            createComparisonDataSet(renderData)?.let { addDataSet(it) }
        }

        chart.data = lineData
        chart.xAxis.valueFormatter = object : ValueFormatter() {
            override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                val index = value.toInt()
                return if (index in renderData.labels.indices) {
                    renderData.labels[index]
                } else {
                    ""
                }
            }
        }

        chart.axisLeft.removeAllLimitLines()
        renderData.goalMax?.let { maxGoal ->
            val limitLine = LimitLine(maxGoal.toFloat(), resources.getString(R.string.graph_goal_limit))
            limitLine.lineColor = resolveThemeColor(androidx.appcompat.R.attr.colorAccent)
            limitLine.lineWidth = 1.5f
            limitLine.textColor = resolveThemeColor(android.R.attr.textColorSecondary)
            chart.axisLeft.addLimitLine(limitLine)
        }

        chart.invalidate()
    }

    private fun showLoading() {
        chart.visibility = View.INVISIBLE
        messageView.visibility = View.GONE
        loadingIndicator.visibility = View.VISIBLE
    }

    private fun showMessage(message: String) {
        loadingIndicator.visibility = View.GONE
        chart.visibility = View.INVISIBLE
        messageView.visibility = View.VISIBLE
        messageView.text = message
    }

    private fun createPrimaryDataSet(renderData: GraphRenderData): LineDataSet {
        val primaryColor = resolveThemeColor(androidx.appcompat.R.attr.colorPrimary)
        return LineDataSet(renderData.entries, resources.getString(R.string.graph_current_period_label)).apply {
            color = primaryColor
            setCircleColor(primaryColor)
            circleRadius = 3f
            lineWidth = 2f
            setDrawCircleHole(false)
            setDrawValues(false)
            setDrawFilled(true)
            fillColor = ColorUtils.setAlphaComponent(primaryColor, 80)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
    }

    private fun createComparisonDataSet(renderData: GraphRenderData): LineDataSet? {
        if (renderData.comparisonEntries.isEmpty() || renderData.comparisonEntries.all { it.y == 0f }) {
            return null
        }
        val secondaryColor = resolveThemeColor(androidx.appcompat.R.attr.colorAccent)
        return LineDataSet(renderData.comparisonEntries, resources.getString(R.string.graph_previous_period_label)).apply {
            color = secondaryColor
            setCircleColor(secondaryColor)
            lineWidth = 1.5f
            circleRadius = 2.5f
            enableDashedLine(6f, 6f, 0f)
            setDrawValues(false)
            setDrawFilled(false)
        }
    }

    private fun configureChart() {
        chart.description.isEnabled = false
        chart.legend.isEnabled = false
        chart.axisRight.isEnabled = false
        chart.setTouchEnabled(true)
        chart.setPinchZoom(true)
        chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        chart.xAxis.granularity = 1f
        chart.axisLeft.setDrawGridLines(true)
        chart.axisLeft.axisMinimum = 0f
        chart.xAxis.setDrawGridLines(false)
        chart.setNoDataText(context.getString(R.string.graph_empty_state))
    }

    private fun resolveThemeColor(@AttrRes attr: Int): Int {
        val typedArray = context.obtainStyledAttributes(intArrayOf(attr))
        val color = typedArray.getColor(0, 0)
        typedArray.recycle()
        return color
    }
}
