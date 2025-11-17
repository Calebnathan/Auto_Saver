package com.example.auto_saver.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.auto_saver.AddCategoryActivity
import com.example.auto_saver.AddExpenseActivity
import com.example.auto_saver.MyApplication
import com.example.auto_saver.R
import com.example.auto_saver.adapters.GroupedExpenseAdapter
import com.example.auto_saver.collectWithLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CategoriesFragment : Fragment() {

    private val viewModel: CategoriesViewModel by viewModels {
        CategoriesViewModelFactory(
            expenseRepository = MyApplication.expenseRepository,
            categoryRepository = MyApplication.categoryRepository,
            userPreferences = MyApplication.userPreferences
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_categories, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_categories)
        val emptyState = view.findViewById<LinearLayout>(R.id.empty_state)
        val fabAdd = view.findViewById<FloatingActionButton>(R.id.fab_add)
        val btnFilter = view.findViewById<ImageButton>(R.id.btn_filter)

        val adapter = GroupedExpenseAdapter(
            onExpenseClick = { _ -> /* TODO: open expense details when implemented */ },
            onCategoryHeaderClick = { header ->
                viewModel.toggleCategory(header.category.id.toString())
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewModel.items.collectWithLifecycle(viewLifecycleOwner) { items ->
            adapter.submitList(items)
        }

        viewModel.hasContent.collectWithLifecycle(viewLifecycleOwner) { hasContent ->
            if (hasContent) {
                recyclerView.visibility = View.VISIBLE
                emptyState.visibility = View.GONE
            } else {
                recyclerView.visibility = View.GONE
                emptyState.visibility = View.VISIBLE
            }
        }

        fabAdd.setOnClickListener {
            showAddActions()
        }

        btnFilter.setOnClickListener {
            showFilterPlaceholder()
        }
    }

    private fun showAddActions() {
        val context = requireContext()
        val options = arrayOf(
            getString(R.string.add_expense),
            getString(R.string.categories) // Add Category
        )

        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.quick_actions)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> startActivity(Intent(context, AddExpenseActivity::class.java))
                    1 -> startActivity(Intent(context, AddCategoryActivity::class.java))
                }
            }
            .show()
    }

    private fun showFilterPlaceholder() {
        val context = requireContext()
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.filter)
            .setMessage(R.string.coming_soon)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
