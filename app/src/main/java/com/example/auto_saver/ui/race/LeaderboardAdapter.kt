package com.example.auto_saver.ui.race

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.auto_saver.R
import com.example.auto_saver.data.model.RaceParticipant
import com.google.android.material.card.MaterialCardView

class LeaderboardAdapter(
    private val budget: Double,
    private val currentUserUid: String?
) : ListAdapter<RaceParticipant, LeaderboardAdapter.ParticipantViewHolder>(ParticipantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_leaderboard_participant, parent, false)
        return ParticipantViewHolder(view, budget, currentUserUid)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ParticipantViewHolder(
        itemView: View,
        private val budget: Double,
        private val currentUserUid: String?
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardParticipant: MaterialCardView = itemView.findViewById(R.id.card_participant)
        private val tvRankBadge: TextView = itemView.findViewById(R.id.tv_rank_badge)
        private val tvTrophy: TextView = itemView.findViewById(R.id.tv_trophy)
        private val tvParticipantName: TextView = itemView.findViewById(R.id.tv_participant_name)
        private val tvParticipantEmail: TextView = itemView.findViewById(R.id.tv_participant_email)
        private val tvAmountSpent: TextView = itemView.findViewById(R.id.tv_amount_spent)
        private val tvBudgetStatus: TextView = itemView.findViewById(R.id.tv_budget_status)
        private val ivStatusIcon: ImageView = itemView.findViewById(R.id.iv_status_icon)

        fun bind(participant: RaceParticipant) {
            val context = itemView.context
            
            // Rank badge
            tvRankBadge.text = participant.rank.toString()
            
            // Trophy for top 3
            when (participant.rank) {
                1 -> {
                    tvTrophy.text = "🏆"
                    tvTrophy.visibility = View.VISIBLE
                    tvRankBadge.visibility = View.GONE
                }
                2 -> {
                    tvTrophy.text = "🥈"
                    tvTrophy.visibility = View.VISIBLE
                    tvRankBadge.visibility = View.GONE
                }
                3 -> {
                    tvTrophy.text = "🥉"
                    tvTrophy.visibility = View.VISIBLE
                    tvRankBadge.visibility = View.GONE
                }
                else -> {
                    tvTrophy.visibility = View.GONE
                    tvRankBadge.visibility = View.VISIBLE
                }
            }
            
            // Name and email
            if (!participant.displayName.isNullOrBlank()) {
                tvParticipantName.text = participant.displayName
                tvParticipantEmail.text = participant.email
            } else {
                tvParticipantName.text = participant.email
                tvParticipantEmail.visibility = View.GONE
            }
            
            // Amount spent
            tvAmountSpent.text = context.getString(R.string.currency_format, participant.totalSpent)
            
            // Budget status
            val isUnderBudget = participant.totalSpent <= budget
            if (isUnderBudget) {
                tvBudgetStatus.text = context.getString(R.string.under_budget)
                tvBudgetStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark))
                ivStatusIcon.setImageResource(android.R.drawable.checkbox_on_background)
                ivStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            } else {
                tvBudgetStatus.text = context.getString(R.string.over_budget)
                tvBudgetStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
                ivStatusIcon.setImageResource(android.R.drawable.ic_delete)
                ivStatusIcon.setColorFilter(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            }
            
            // Highlight current user's row
            if (participant.uid == currentUserUid) {
                cardParticipant.strokeWidth = 3
                cardParticipant.strokeColor = ContextCompat.getColor(context, R.color.red_primary)
            } else {
                cardParticipant.strokeWidth = 1
                cardParticipant.strokeColor = ContextCompat.getColor(context, R.color.gray_300)
            }
            
            // Special styling for top 3
            when (participant.rank) {
                1 -> {
                    tvRankBadge.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_orange_light)
                }
                2 -> {
                    tvRankBadge.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.darker_gray)
                }
                3 -> {
                    tvRankBadge.backgroundTintList = ContextCompat.getColorStateList(context, android.R.color.holo_orange_dark)
                }
                else -> {
                    tvRankBadge.backgroundTintList = null
                }
            }
        }
    }

    private class ParticipantDiffCallback : DiffUtil.ItemCallback<RaceParticipant>() {
        override fun areItemsTheSame(oldItem: RaceParticipant, newItem: RaceParticipant): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: RaceParticipant, newItem: RaceParticipant): Boolean {
            return oldItem == newItem
        }
    }
}