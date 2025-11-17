package com.example.auto_saver.ui.social

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.auto_saver.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import androidx.fragment.app.viewModels

class AddFriendBottomSheet : BottomSheetDialogFragment() {

    private val socialViewModel: SocialViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_add_friend, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val emailLayout = view.findViewById<TextInputLayout>(R.id.input_layout_email)
        val emailField = view.findViewById<TextInputEditText>(R.id.input_friend_email)
        val sendButton = view.findViewById<MaterialButton>(R.id.button_send_invite)

        sendButton.setOnClickListener {
            val email = emailField.text?.toString().orEmpty()
            if (email.isBlank()) {
                emailLayout.error = getString(R.string.add_friend_error_empty)
                return@setOnClickListener
            }
            emailLayout.error = null
            socialViewModel.sendFriendInvite(email)
            dismiss()
        }
    }
}
