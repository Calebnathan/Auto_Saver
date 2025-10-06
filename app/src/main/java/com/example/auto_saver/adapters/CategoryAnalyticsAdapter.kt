package com.example.auto_saver.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.auto_saver.CategorySpending
import com.example.auto_saver.R
import java.util.Locale

class CategoryAnalyticsAdapter(
    private val categories: List<CategorySpending>
) : RecyclerView.Adapter<CategoryAnalyticsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCategoryName: TextView = view.findViewById(R.id.tv_category_name)
        val tvCategoryAmount: TextView = view.findViewById(R.id.tv_category_amount)
        val tvCategoryPercentage: TextView = view.findViewById(R.id.tv_category_percentage)
        val progressCategory: ProgressBar = view.findViewById(R.id.progress_category)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_analytics, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]

        holder.tvCategoryName.text = category.categoryName
        holder.tvCategoryAmount.text = String.format(Locale.getDefault(), "$%.2f", category.totalAmount)
        holder.tvCategoryPercentage.text = String.format(Locale.getDefault(), "%.1f%% of total • %d expenses",
            category.percentage, category.expenseCount)
        holder.progressCategory.progress = category.percentage.toInt()
    }

    override fun getItemCount() = categories.size
}
