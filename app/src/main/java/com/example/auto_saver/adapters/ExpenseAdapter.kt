package com.example.auto_saver.adapters

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.auto_saver.Expense
import com.example.auto_saver.PhotoViewerActivity
import com.example.auto_saver.R
import com.google.android.material.card.MaterialCardView
import java.io.File

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
        private val tvTime: TextView = itemView.findViewById(R.id.tv_expense_time)
        private val cardPhotoThumbnail: MaterialCardView = itemView.findViewById(R.id.card_photo_thumbnail)
        private val ivExpensePhoto: ImageView = itemView.findViewById(R.id.iv_expense_photo)

        fun bind(expense: Expense) {
            tvDescription.text = expense.description ?: "No description"
            tvAmount.text = itemView.context.getString(R.string.currency_format, expense.amount)
            tvCategory.text = getCategoryName(expense.categoryId)
            tvDate.text = expense.date

            // Show time range if available
            if (expense.startTime != null && expense.endTime != null) {
                tvTime.text = "${expense.startTime} - ${expense.endTime}"
                tvTime.visibility = View.VISIBLE
            } else if (expense.startTime != null) {
                tvTime.text = expense.startTime
                tvTime.visibility = View.VISIBLE
            } else {
                tvTime.visibility = View.GONE
            }

            // Show photo thumbnail if available
            if (expense.photoPath != null) {
                val photoFile = File(expense.photoPath)
                if (photoFile.exists()) {
                    val uri = FileProvider.getUriForFile(
                        itemView.context,
                        "${itemView.context.packageName}.fileprovider",
                        photoFile
                    )
                    ivExpensePhoto.setImageURI(uri)
                    cardPhotoThumbnail.visibility = View.VISIBLE

                    // Click photo to view full size
                    cardPhotoThumbnail.setOnClickListener {
                        val intent = Intent(itemView.context, PhotoViewerActivity::class.java)
                        intent.putExtra("PHOTO_PATH", expense.photoPath)
                        itemView.context.startActivity(intent)
                    }
                } else {
                    cardPhotoThumbnail.visibility = View.GONE
                }
            } else {
                cardPhotoThumbnail.visibility = View.GONE
            }

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
