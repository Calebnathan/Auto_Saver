package com.example.auto_saver.ui.race

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.viewModels
import com.example.auto_saver.MyApplication
import com.example.auto_saver.R
import com.example.auto_saver.collectWithLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class JoinChallengeBottomSheet : BottomSheetDialogFragment() {

    private val raceViewModel: RaceViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    ) {
        RaceViewModelFactory(
            raceRepository = MyApplication.raceRepository,
            userPreferences = MyApplication.userPreferences,
            firebaseAuth = FirebaseAuth.getInstance()
        )
    }

    private lateinit var tilInviteCode: TextInputLayout
    private lateinit var etInviteCode: TextInputEditText
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnJoin: MaterialButton
    private lateinit var loadingOverlay: FrameLayout

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_join_challenge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        setupValidation()
        observeViewModel()
    }

    private fun initializeViews(view: View) {
        tilInviteCode = view.findViewById(R.id.til_invite_code)
        etInviteCode = view.findViewById(R.id.et_invite_code)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnJoin = view.findViewById(R.id.btn_join)
        loadingOverlay = view.findViewById(R.id.loading_overlay)
    }

    private fun setupClickListeners() {
        btnCancel.setOnClickListener {
            dismiss()
        }

        btnJoin.setOnClickListener {
            if (validateInput()) {
                joinChallenge()
            }
        }
    }

    private fun setupValidation() {
        etInviteCode.addTextChangedListener {
            tilInviteCode.error = null
        }
    }

    private fun observeViewModel() {
        raceViewModel.uiEvents.collectWithLifecycle(viewLifecycleOwner) { event ->
            when (event) {
                is RaceUiEvent.Message -> {
                    // Challenge joined successfully
                    dismiss()
                }
                is RaceUiEvent.Error -> {
                    loadingOverlay.visibility = View.GONE
                    Snackbar.make(requireView(), event.text, Snackbar.LENGTH_LONG).show()
                }
                is RaceUiEvent.NavigateToDetail -> {
                    // Will be handled by parent fragment
                    dismiss()
                }
            }
        }
    }

    private fun validateInput(): Boolean {
        val code = etInviteCode.text.toString().trim()
        
        if (code.isEmpty()) {
            tilInviteCode.error = getString(R.string.error_code_required)
            return false
        }
        
        if (code.length != 6) {
            tilInviteCode.error = "Code must be 6 characters"
            return false
        }

        return true
    }

    private fun joinChallenge() {
        loadingOverlay.visibility = View.VISIBLE
        val code = etInviteCode.text.toString().trim().uppercase()
        raceViewModel.joinChallengeByCode(code)
    }
}