package com.example.auto_saver.ui.race

import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CreateChallengeBottomSheet : BottomSheetDialogFragment() {

    private val raceViewModel: RaceViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    ) {
        RaceViewModelFactory(
            raceRepository = MyApplication.raceRepository,
            userPreferences = MyApplication.userPreferences,
            firebaseAuth = FirebaseAuth.getInstance()
        )
    }

    private lateinit var tilChallengeName: TextInputLayout
    private lateinit var etChallengeName: TextInputEditText
    private lateinit var tilBudget: TextInputLayout
    private lateinit var etBudget: TextInputEditText
    private lateinit var tilStartDate: TextInputLayout
    private lateinit var etStartDate: TextInputEditText
    private lateinit var tilEndDate: TextInputLayout
    private lateinit var etEndDate: TextInputEditText
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnCreate: MaterialButton
    private lateinit var loadingOverlay: FrameLayout

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    
    private var startDateMillis: Long = 0
    private var endDateMillis: Long = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_create_challenge, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupClickListeners()
        setupValidation()
        observeViewModel()
    }

    private fun initializeViews(view: View) {
        tilChallengeName = view.findViewById(R.id.til_challenge_name)
        etChallengeName = view.findViewById(R.id.et_challenge_name)
        tilBudget = view.findViewById(R.id.til_budget)
        etBudget = view.findViewById(R.id.et_budget)
        tilStartDate = view.findViewById(R.id.til_start_date)
        etStartDate = view.findViewById(R.id.et_start_date)
        tilEndDate = view.findViewById(R.id.til_end_date)
        etEndDate = view.findViewById(R.id.et_end_date)
        btnCancel = view.findViewById(R.id.btn_cancel)
        btnCreate = view.findViewById(R.id.btn_create)
        loadingOverlay = view.findViewById(R.id.loading_overlay)
    }

    private fun setupClickListeners() {
        btnCancel.setOnClickListener {
            dismiss()
        }

        btnCreate.setOnClickListener {
            if (validateInput()) {
                createChallenge()
            }
        }

        etStartDate.setOnClickListener {
            showDatePicker(isStartDate = true)
        }

        tilStartDate.setEndIconOnClickListener {
            showDatePicker(isStartDate = true)
        }

        etEndDate.setOnClickListener {
            showDatePicker(isStartDate = false)
        }

        tilEndDate.setEndIconOnClickListener {
            showDatePicker(isStartDate = false)
        }
    }

    private fun setupValidation() {
        etChallengeName.addTextChangedListener {
            tilChallengeName.error = null
        }

        etBudget.addTextChangedListener {
            tilBudget.error = null
        }
    }

    private fun observeViewModel() {
        raceViewModel.uiEvents.collectWithLifecycle(viewLifecycleOwner) { event ->
            when (event) {
                is RaceUiEvent.Message -> {
                    // Challenge created successfully
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

    private fun showDatePicker(isStartDate: Boolean) {
        val calendar = Calendar.getInstance()
        
        // Set minimum date to today
        val minDate = Calendar.getInstance().timeInMillis
        
        // If selecting end date and start date is set, use start date as minimum
        val actualMinDate = if (!isStartDate && startDateMillis > 0) {
            startDateMillis
        } else {
            minDate
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val dateMillis = calendar.timeInMillis
                val dateString = dateFormat.format(calendar.time)
                val displayString = displayFormat.format(calendar.time)

                if (isStartDate) {
                    startDateMillis = dateMillis
                    etStartDate.setText(displayString)
                    tilStartDate.error = null
                    
                    // Clear end date if it's before new start date
                    if (endDateMillis > 0 && endDateMillis < startDateMillis) {
                        endDateMillis = 0
                        etEndDate.setText("")
                    }
                } else {
                    endDateMillis = dateMillis
                    etEndDate.setText(displayString)
                    tilEndDate.error = null
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate = actualMinDate
        datePickerDialog.show()
    }

    private fun validateInput(): Boolean {
        var isValid = true

        // Validate challenge name
        val name = etChallengeName.text.toString().trim()
        if (name.isEmpty()) {
            tilChallengeName.error = getString(R.string.error_challenge_name_required)
            isValid = false
        }

        // Validate budget
        val budgetText = etBudget.text.toString().trim()
        if (budgetText.isEmpty()) {
            tilBudget.error = getString(R.string.error_budget_invalid)
            isValid = false
        } else {
            val budget = budgetText.toDoubleOrNull()
            if (budget == null || budget <= 0) {
                tilBudget.error = getString(R.string.error_budget_invalid)
                isValid = false
            }
        }

        // Validate start date
        if (startDateMillis == 0L) {
            tilStartDate.error = "Start date is required"
            isValid = false
        }

        // Validate end date
        if (endDateMillis == 0L) {
            tilEndDate.error = "End date is required"
            isValid = false
        } else if (endDateMillis <= startDateMillis) {
            tilEndDate.error = getString(R.string.error_date_invalid)
            isValid = false
        }

        return isValid
    }

    private fun createChallenge() {
        loadingOverlay.visibility = View.VISIBLE

        val name = etChallengeName.text.toString().trim()
        val budget = etBudget.text.toString().trim().toDouble()
        val startDate = dateFormat.format(startDateMillis)
        val endDate = dateFormat.format(endDateMillis)

        raceViewModel.createChallenge(name, budget, startDate, endDate)
    }
}