package com.example.auto_saver.ui.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.auto_saver.MyApplication
import com.example.auto_saver.R
import com.example.auto_saver.collectWithLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class SocialFragment : Fragment() {

    private val socialViewModel: SocialViewModel by viewModels {
        SocialViewModelFactory(
            friendRepository = MyApplication.friendRepository,
            userPreferences = MyApplication.userPreferences,
            firebaseAuth = FirebaseAuth.getInstance()
        )
    }

    private lateinit var friendAdapter: FriendAdapter
    private lateinit var requestAdapter: FriendRequestAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_social, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val summaryTitle = view.findViewById<TextView>(R.id.text_friend_summary)
        val summaryBody = view.findViewById<TextView>(R.id.text_friend_subtitle)
        val addFriendButton = view.findViewById<MaterialButton>(R.id.button_add_friend)
        val pendingEmpty = view.findViewById<TextView>(R.id.text_pending_empty)
        val friendsEmpty = view.findViewById<TextView>(R.id.text_friends_empty)
        val requestList = view.findViewById<RecyclerView>(R.id.list_friend_requests)
        val friendsList = view.findViewById<RecyclerView>(R.id.list_friends)

        friendAdapter = FriendAdapter { friend ->
            socialViewModel.removeFriend(friend.uid)
        }
        requestAdapter = FriendRequestAdapter(
            onAccept = { socialViewModel.acceptFriend(it.id) },
            onDecline = { socialViewModel.declineFriend(it.id) }
        )

        requestList.layoutManager = LinearLayoutManager(requireContext())
        requestList.adapter = requestAdapter

        friendsList.layoutManager = LinearLayoutManager(requireContext())
        friendsList.adapter = friendAdapter

        addFriendButton.isEnabled = socialViewModel.sessionActive
        addFriendButton.setOnClickListener {
            if (socialViewModel.sessionActive) {
                AddFriendBottomSheet().show(childFragmentManager, "AddFriend")
            }
        }

        socialViewModel.friends.collectWithLifecycle(viewLifecycleOwner) { friends ->
            friendAdapter.submitList(friends)
            friendsEmpty.isVisible = friends.isEmpty()
            val summaryText = resources.getQuantityString(
                R.plurals.friend_count,
                friends.size,
                friends.size
            )
            summaryTitle.text = summaryText
            summaryBody.text = if (friends.isEmpty()) {
                getString(R.string.friend_summary_body)
            } else {
                getString(R.string.friend_summary_connected)
            }
        }

        socialViewModel.friendRequests.collectWithLifecycle(viewLifecycleOwner) { requests ->
            requestAdapter.submitList(requests)
            pendingEmpty.isVisible = requests.isEmpty()
        }

        socialViewModel.uiEvents.collectWithLifecycle(viewLifecycleOwner) { event ->
            val message = when (event) {
                is SocialViewModel.UiEvent.Error -> event.text
                is SocialViewModel.UiEvent.Message -> event.text
            }
            Snackbar.make(view, message, Snackbar.LENGTH_LONG).apply {
                if (event is SocialViewModel.UiEvent.Error) {
                    val color = ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_error)
                    setBackgroundTint(color)
                }
            }.show()
        }
    }
}
