package com.example.auto_saver.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.auto_saver.R
import com.example.auto_saver.models.ExpenseListItem

/**
 * Adapter for displaying grouped expenses by category with expand/collapse functionality
 */
class GroupedExpenseAdapter(
    private val onExpenseClick: (Int) -> Unit,
    private val onCategoryHeaderClick: (ExpenseListItem.CategoryHeader) -> Unit
) : ListAdapter<ExpenseListItem, RecyclerView.ViewHolder>(ExpenseListItemDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_CATEGORY_HEADER = 0
        private const val VIEW_TYPE_EXPENSE = 1
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ExpenseListItem.CategoryHeader -> VIEW_TYPE_CATEGORY_HEADER
            is ExpenseListItem.ExpenseItem -> VIEW_TYPE_EXPENSE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_CATEGORY_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_category_header, parent, false)
                CategoryHeaderViewHolder(view, onCategoryHeaderClick)
            }
            VIEW_TYPE_EXPENSE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_expense, parent, false)
                ExpenseItemViewHolder(view, onExpenseClick)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ExpenseListItem.CategoryHeader -> (holder as CategoryHeaderViewHolder).bind(item)
            is ExpenseListItem.ExpenseItem -> (holder as ExpenseItemViewHolder).bind(item)
        }
    }

    class CategoryHeaderViewHolder(
        itemView: View,
        private val onCategoryHeaderClick: (ExpenseListItem.CategoryHeader) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvCategoryHeader: TextView = itemView.findViewById(R.id.tv_category_header)
        private val tvCategoryCount: TextView = itemView.findViewById(R.id.tv_category_count)
        private val ivExpandIcon: ImageView = itemView.findViewById(R.id.iv_expand_icon)

        fun bind(item: ExpenseListItem.CategoryHeader) {
            tvCategoryHeader.text = item.category.name
            val count = item.expenses.size
            tvCategoryCount.text = if (count == 1) "1 item" else "$count items"

            // Set expand/collapse icon
            if (item.isExpanded) {
                ivExpandIcon.rotation = 180f
            } else {
                ivExpandIcon.rotation = 0f
            }

            itemView.setOnClickListener {
                onCategoryHeaderClick(item)
            }
        }
    }

    class ExpenseItemViewHolder(
        itemView: View,
        private val onExpenseClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvDescription: TextView = itemView.findViewById(R.id.tv_expense_description)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_expense_amount)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_expense_category)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_expense_date)

        fun bind(item: ExpenseListItem.ExpenseItem) {
            val expense = item.expense
            tvDescription.text = expense.description ?: "No description"
            tvAmount.text = itemView.context.getString(R.string.currency_format, expense.amount)
            tvCategory.text = item.categoryName
            tvDate.text = expense.date

            itemView.setOnClickListener {
                onExpenseClick(expense.id)
            }
        }
    }

    class ExpenseListItemDiffCallback : DiffUtil.ItemCallback<ExpenseListItem>() {
        override fun areItemsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem): Boolean {
            return when {
                oldItem is ExpenseListItem.CategoryHeader && newItem is ExpenseListItem.CategoryHeader -> {
                    oldItem.category.id == newItem.category.id
                }
                oldItem is ExpenseListItem.ExpenseItem && newItem is ExpenseListItem.ExpenseItem -> {
                    oldItem.expense.id == newItem.expense.id
                }
                else -> false
            }
        }

        override fun areContentsTheSame(oldItem: ExpenseListItem, newItem: ExpenseListItem): Boolean {
            return oldItem == newItem
        }
    }
}

