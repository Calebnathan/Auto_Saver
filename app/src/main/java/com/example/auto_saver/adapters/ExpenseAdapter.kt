package com.example.auto_saver.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.auto_saver.Expense
import com.example.auto_saver.R

/**
 * RecyclerView adapter for displaying expenses
 */
class ExpenseAdapter(
    private val onExpenseClick: (Expense) -> Unit,
    private val getCategoryName: (Int) -> String
) : ListAdapter<Expense, ExpenseAdapter.ExpenseViewHolder>(ExpenseDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense, parent, false)
        return ExpenseViewHolder(view, onExpenseClick, getCategoryName)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ExpenseViewHolder(
        itemView: View,
        private val onExpenseClick: (Expense) -> Unit,
        private val getCategoryName: (Int) -> String
    ) : RecyclerView.ViewHolder(itemView) {

        private val tvDescription: TextView = itemView.findViewById(R.id.tv_expense_description)
        private val tvAmount: TextView = itemView.findViewById(R.id.tv_expense_amount)
        private val tvCategory: TextView = itemView.findViewById(R.id.tv_expense_category)
        private val tvDate: TextView = itemView.findViewById(R.id.tv_expense_date)

        fun bind(expense: Expense) {
            tvDescription.text = expense.description ?: "No description"
            tvAmount.text = itemView.context.getString(R.string.currency_format, expense.amount)
            tvCategory.text = getCategoryName(expense.categoryId)
            tvDate.text = expense.date

            itemView.setOnClickListener {
                onExpenseClick(expense)
            }
        }
    }

    class ExpenseDiffCallback : DiffUtil.ItemCallback<Expense>() {
        override fun areItemsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Expense, newItem: Expense): Boolean {
            return oldItem == newItem
        }
    }
}

