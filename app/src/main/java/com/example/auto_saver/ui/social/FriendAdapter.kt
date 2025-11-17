package com.example.auto_saver.ui.social

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.auto_saver.R
import com.example.auto_saver.data.model.FriendProfile
import com.google.android.material.button.MaterialButton
import android.widget.ImageView

class FriendAdapter(
    private val onRemove: (FriendProfile) -> Unit
) : ListAdapter<FriendProfile, FriendAdapter.FriendViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<FriendProfile>() {
        override fun areItemsTheSame(oldItem: FriendProfile, newItem: FriendProfile): Boolean =
            oldItem.uid == newItem.uid

        override fun areContentsTheSame(oldItem: FriendProfile, newItem: FriendProfile): Boolean =
            oldItem == newItem
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val name: TextView = itemView.findViewById(R.id.text_name)
        private val email: TextView = itemView.findViewById(R.id.text_email)
        private val removeButton: MaterialButton = itemView.findViewById(R.id.button_remove)
        private val avatar: ImageView = itemView.findViewById(R.id.image_avatar)

        fun bind(friend: FriendProfile) {
            name.text = friend.displayName?.takeIf { it.isNotBlank() } ?: friend.email
            email.text = friend.email
            removeButton.setOnClickListener { onRemove(friend) }
            avatar.setImageResource(R.drawable.ic_social_people)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
