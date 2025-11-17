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
import com.example.auto_saver.data.model.ChallengeStatus
import com.example.auto_saver.data.model.RaceChallenge
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Locale

class ChallengeAdapter(
    private val onChallengeClick: (RaceChallenge) -> Unit,
    private val currentUserUid: String?
) : ListAdapter<RaceChallenge, ChallengeAdapter.ChallengeViewHolder>(ChallengeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_challenge, parent, false)
        return ChallengeViewHolder(view, onChallengeClick, currentUserUid)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChallengeViewHolder(
        itemView: View,
        private val onChallengeClick: (RaceChallenge) -> Unit,
        private val currentUserUid: String?
    ) : RecyclerView.ViewHolder(itemView) {

        private val cardChallenge: MaterialCardView = itemView.findViewById(R.id.card_challenge)
        private val ivChallengeIcon: ImageView = itemView.findViewById(R.id.iv_challenge_icon)
        private val tvChallengeName: TextView = itemView.findViewById(R.id.tv_challenge_name)
        private val chipStatus: Chip = itemView.findViewById(R.id.chip_status)
        private val tvDateRange: TextView = itemView.findViewById(R.id.tv_date_range)
        private val tvBudget: TextView = itemView.findViewById(R.id.tv_budget)
        private val tvParticipants: TextView = itemView.findViewById(R.id.tv_participants)
        private val layoutRank: LinearLayout = itemView.findViewById(R.id.layout_rank)
        private val tvRank: TextView = itemView.findViewById(R.id.tv_rank)

        fun bind(challenge: RaceChallenge) {
            tvChallengeName.text = challenge.name
            
            // Format date range
            tvDateRange.text = formatDateRange(challenge.startDate, challenge.endDate)
            
            // Format budget
            tvBudget.text = itemView.context.getString(R.string.currency_format, challenge.budget)
            
            // Participants count
            tvParticipants.text = challenge.participants.size.toString()
            
            // Status chip
            updateStatusChip(challenge.status)
            
            // Show rank only for active challenges
            if (challenge.status == ChallengeStatus.ACTIVE || challenge.status == ChallengeStatus.PENDING) {
                layoutRank.visibility = View.VISIBLE
                // Note: Rank would need to be fetched from leaderboard in real implementation
                // For now, showing placeholder
                tvRank.text = "#?"
            } else {
                layoutRank.visibility = View.GONE
            }
            
            // Click listener
            cardChallenge.setOnClickListener {
                onChallengeClick(challenge)
            }
            
            // Visual feedback for completed/cancelled challenges
            if (challenge.status == ChallengeStatus.COMPLETED || challenge.status == ChallengeStatus.CANCELLED) {
                cardChallenge.alpha = 0.7f
            } else {
                cardChallenge.alpha = 1.0f
            }
        }

        private fun updateStatusChip(status: ChallengeStatus) {
            val context = itemView.context
            when (status) {
                ChallengeStatus.PENDING -> {
                    chipStatus.text = context.getString(R.string.challenge_status_pending)
                    chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light)
                    chipStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                ChallengeStatus.ACTIVE -> {
                    chipStatus.text = context.getString(R.string.challenge_status_active)
                    chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
                    chipStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                ChallengeStatus.COMPLETED -> {
                    chipStatus.text = context.getString(R.string.challenge_status_completed)
                    chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray)
                    chipStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
                ChallengeStatus.CANCELLED -> {
                    chipStatus.text = context.getString(R.string.challenge_status_cancelled)
                    chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light)
                    chipStatus.setTextColor(ContextCompat.getColor(context, android.R.color.white))
                }
            }
        }

        private fun formatDateRange(startDate: String, endDate: String): String {
            return try {
                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val outputFormat = SimpleDateFormat("MMM d", Locale.getDefault())
                
                val start = inputFormat.parse(startDate)
                val end = inputFormat.parse(endDate)
                
                if (start != null && end != null) {
                    "${outputFormat.format(start)} - ${outputFormat.format(end)}"
                } else {
                    "$startDate - $endDate"
                }
            } catch (e: Exception) {
                "$startDate - $endDate"
            }
        }
    }

    private class ChallengeDiffCallback : DiffUtil.ItemCallback<RaceChallenge>() {
        override fun areItemsTheSame(oldItem: RaceChallenge, newItem: RaceChallenge): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: RaceChallenge, newItem: RaceChallenge): Boolean {
            return oldItem == newItem
        }
    }
}