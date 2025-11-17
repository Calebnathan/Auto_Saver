package com.example.auto_saver.ui.home

import android.content.Intent
import android.os.Bundle
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
import com.example.auto_saver.ui.components.GraphMetric
import com.example.auto_saver.ui.components.GraphUiState
import com.example.auto_saver.ui.components.SpendingGraphView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView

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

        val graphView = view.findViewById<SpendingGraphView>(R.id.view_spending_graph)
        val graphSubtitle = view.findViewById<TextView>(R.id.tv_spending_graph_subtitle)
        val metricToggle = view.findViewById<MaterialButtonToggleGroup>(R.id.metric_toggle)
        val totalSpentText = view.findViewById<TextView>(R.id.tv_total_spent_value)
        val expenseCountText = view.findViewById<TextView>(R.id.tv_expense_count_value)
        val goalProgressText = view.findViewById<TextView>(R.id.tv_goal_progress_value)

        val graphCard = view.findViewById<MaterialCardView>(R.id.card_spending_graph)
        val btnViewDetails = view.findViewById<MaterialButton>(R.id.btn_view_details)
        val rangeToggle = view.findViewById<MaterialButtonToggleGroup>(R.id.range_toggle)
        val btnManageGoals = view.findViewById<MaterialButton>(R.id.btn_manage_goals)
        val btnViewAnalytics = view.findViewById<MaterialButton>(R.id.btn_view_analytics)
        val btnQuickAddExpense = view.findViewById<MaterialButton>(R.id.btn_quick_add_expense)

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

        metricToggle.check(R.id.btn_metric_daily)
        metricToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val metric = when (checkedId) {
                R.id.btn_metric_daily -> GraphMetric.DAILY
                R.id.btn_metric_weekly -> GraphMetric.WEEKLY
                R.id.btn_metric_monthly -> GraphMetric.MONTHLY
                else -> GraphMetric.DAILY
            }
            dashboardViewModel.setGraphMetric(metric)
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

        dashboardViewModel.graphState.collectWithLifecycle(viewLifecycleOwner) { state ->
            graphView.setState(state)
            graphSubtitle.text = when (state) {
                is GraphUiState.Success -> state.data.rangeLabel
                GraphUiState.Empty -> getString(R.string.graph_empty_state)
                is GraphUiState.Error -> getString(R.string.graph_error_state)
                GraphUiState.Loading -> getString(R.string.spending_graph_subtitle)
            }
        }
    }
}
