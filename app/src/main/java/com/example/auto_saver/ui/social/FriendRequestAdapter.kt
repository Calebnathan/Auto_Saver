package com.example.auto_saver.ui.social

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.auto_saver.R
import com.example.auto_saver.data.model.FriendRequest
import com.google.android.material.button.MaterialButton

class FriendRequestAdapter(
    private val onAccept: (FriendRequest) -> Unit,
    private val onDecline: (FriendRequest) -> Unit
) : ListAdapter<FriendRequest, FriendRequestAdapter.RequestViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<FriendRequest>() {
        override fun areItemsTheSame(oldItem: FriendRequest, newItem: FriendRequest): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: FriendRequest, newItem: FriendRequest): Boolean =
            oldItem == newItem
    }

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val emailText: TextView = itemView.findViewById(R.id.text_request_email)
        private val messageText: TextView = itemView.findViewById(R.id.text_request_message)
        private val acceptButton: MaterialButton = itemView.findViewById(R.id.button_accept)
        private val declineButton: MaterialButton = itemView.findViewById(R.id.button_decline)

        fun bind(request: FriendRequest) {
            emailText.text = request.fromEmail
            messageText.text = itemView.context.getString(R.string.pending_request_message_from, request.fromEmail)
            acceptButton.setOnClickListener { onAccept(request) }
            declineButton.setOnClickListener { onDecline(request) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
