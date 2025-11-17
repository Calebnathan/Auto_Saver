package com.example.auto_saver.ui.race

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.auto_saver.MyApplication
import com.example.auto_saver.R
import com.example.auto_saver.collectWithLifecycle
import com.example.auto_saver.data.model.ChallengeStatus
import com.example.auto_saver.data.model.RaceChallenge
import com.example.auto_saver.data.model.RaceParticipant
import com.example.auto_saver.data.repository.UnifiedRaceRepository
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ChallengeDetailActivity : AppCompatActivity() {

    private val viewModel: ChallengeDetailViewModel by viewModels {
        ChallengeDetailViewModelFactory(
            challengeId = intent.getStringExtra("CHALLENGE_ID") ?: "",
            raceRepository = MyApplication.raceRepository,
            firebaseAuth = FirebaseAuth.getInstance()
        )
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var tvChallengeName: TextView
    private lateinit var tvDateRange: TextView
    private lateinit var chipStatus: Chip
    private lateinit var tvBudget: TextView
    private lateinit var tvParticipants: TextView
    private lateinit var layoutInviteCode: LinearLayout
    private lateinit var tvInviteCode: TextView
    private lateinit var btnCopyCode: MaterialButton
    private lateinit var cardYourProgress: MaterialCardView
    private lateinit var tvYourRankBadge: TextView
    private lateinit var tvYourSpent: TextView
    private lateinit var tvYourRemaining: TextView
    private lateinit var progressBudget: LinearProgressIndicator
    private lateinit var tvBudgetPercentage: TextView
    private lateinit var btnSyncSpending: MaterialButton
    private lateinit var tvLeaderboardEmpty: TextView
    private lateinit var rvLeaderboard: RecyclerView
    private lateinit var layoutActionButtons: LinearLayout
    private lateinit var btnStartChallenge: MaterialButton
    private lateinit var btnEndChallenge: MaterialButton
    private lateinit var btnLeaveChallenge: MaterialButton
    private lateinit var btnDeleteChallenge: MaterialButton
    private lateinit var loadingOverlay: FrameLayout

    private lateinit var leaderboardAdapter: LeaderboardAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_challenge_detail)

        setupWindowInsets()
        initializeViews()
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(insets.left, 0, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        swipeRefresh = findViewById(R.id.swipe_refresh)
        tvChallengeName = findViewById(R.id.tv_challenge_name)
        tvDateRange = findViewById(R.id.tv_date_range)
        chipStatus = findViewById(R.id.chip_status)
        tvBudget = findViewById(R.id.tv_budget)
        tvParticipants = findViewById(R.id.tv_participants)
        layoutInviteCode = findViewById(R.id.layout_invite_code)
        tvInviteCode = findViewById(R.id.tv_invite_code)
        btnCopyCode = findViewById(R.id.btn_copy_code)
        cardYourProgress = findViewById(R.id.card_your_progress)
        tvYourRankBadge = findViewById(R.id.tv_your_rank_badge)
        tvYourSpent = findViewById(R.id.tv_your_spent)
        tvYourRemaining = findViewById(R.id.tv_your_remaining)
        progressBudget = findViewById(R.id.progress_budget)
        tvBudgetPercentage = findViewById(R.id.tv_budget_percentage)
        btnSyncSpending = findViewById(R.id.btn_sync_spending)
        tvLeaderboardEmpty = findViewById(R.id.tv_leaderboard_empty)
        rvLeaderboard = findViewById(R.id.rv_leaderboard)
        layoutActionButtons = findViewById(R.id.layout_action_buttons)
        btnStartChallenge = findViewById(R.id.btn_start_challenge)
        btnEndChallenge = findViewById(R.id.btn_end_challenge)
        btnLeaveChallenge = findViewById(R.id.btn_leave_challenge)
        btnDeleteChallenge = findViewById(R.id.btn_delete_challenge)
        loadingOverlay = findViewById(R.id.loading_overlay)
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        leaderboardAdapter = LeaderboardAdapter(
            budget = 0.0,
            currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        )
        rvLeaderboard.layoutManager = LinearLayoutManager(this)
        rvLeaderboard.adapter = leaderboardAdapter
    }

    private fun setupClickListeners() {
        swipeRefresh.setOnRefreshListener {
            viewModel.syncSpending()
            swipeRefresh.isRefreshing = false
        }

        btnCopyCode.setOnClickListener {
            copyInviteCode()
        }

        btnSyncSpending.setOnClickListener {
            viewModel.syncSpending()
        }

        btnStartChallenge.setOnClickListener {
            showConfirmationDialog(
                title = "Start Challenge",
                message = "Are you sure you want to start this challenge? Participants will begin tracking their spending.",
                onConfirm = { viewModel.startChallenge() }
            )
        }

        btnEndChallenge.setOnClickListener {
            showConfirmationDialog(
                title = "End Challenge",
                message = "Are you sure you want to end this challenge? This will finalize the results.",
                onConfirm = { viewModel.endChallenge() }
            )
        }

        btnLeaveChallenge.setOnClickListener {
            showConfirmationDialog(
                title = "Leave Challenge",
                message = "Are you sure you want to leave this challenge? You won't be able to rejoin with the same invite code.",
                onConfirm = { viewModel.leaveChallenge() }
            )
        }

        btnDeleteChallenge.setOnClickListener {
            showConfirmationDialog(
                title = "Delete Challenge",
                message = "Are you sure you want to delete this challenge? This action cannot be undone and will remove all participant data.",
                onConfirm = { viewModel.deleteChallenge() }
            )
        }
    }

    private fun observeViewModel() {
        viewModel.challenge.collectWithLifecycle(this) { challenge ->
            challenge?.let { updateChallengeUI(it) }
        }

        viewModel.leaderboard.collectWithLifecycle(this) { participants ->
            updateLeaderboardUI(participants)
        }

        viewModel.uiEvents.collectWithLifecycle(this) { event ->
            handleUiEvent(event)
        }
    }

    private fun updateChallengeUI(challenge: RaceChallenge) {
        tvChallengeName.text = challenge.name
        tvDateRange.text = formatDateRange(challenge.startDate, challenge.endDate)
        tvBudget.text = getString(R.string.currency_format, challenge.budget)
        tvParticipants.text = challenge.participants.size.toString()
        tvInviteCode.text = challenge.inviteCode

        // Update status chip
        updateStatusChip(challenge.status)

        // Show/hide invite code section
        layoutInviteCode.isVisible = challenge.status != ChallengeStatus.COMPLETED && 
                                      challenge.status != ChallengeStatus.CANCELLED

        // Update action buttons visibility
        updateActionButtons(challenge)

        // Update leaderboard adapter budget
        leaderboardAdapter = LeaderboardAdapter(
            budget = challenge.budget,
            currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        )
        rvLeaderboard.adapter = leaderboardAdapter
    }

    private fun updateLeaderboardUI(participants: List<RaceParticipant>) {
        if (participants.isEmpty()) {
            tvLeaderboardEmpty.isVisible = true
            rvLeaderboard.isVisible = false
        } else {
            tvLeaderboardEmpty.isVisible = false
            rvLeaderboard.isVisible = true
            leaderboardAdapter.submitList(participants)

            // Update current user's progress
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid
            val currentUser = participants.find { it.uid == currentUid }
            if (currentUser != null) {
                updateUserProgressUI(currentUser)
            }
        }
    }

    private fun updateUserProgressUI(participant: RaceParticipant) {
        val challenge = viewModel.challenge.value ?: return

        tvYourRankBadge.text = participant.rank.toString()
        tvYourSpent.text = getString(R.string.currency_format, participant.totalSpent)

        val remaining = challenge.budget - participant.totalSpent
        tvYourRemaining.text = getString(R.string.currency_format, remaining)

        if (remaining < 0) {
            tvYourRemaining.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else {
            tvYourRemaining.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
        }

        // Progress bar
        val percentage = ((participant.totalSpent / challenge.budget) * 100).toInt().coerceIn(0, 100)
        progressBudget.progress = percentage
        tvBudgetPercentage.text = "$percentage% of budget used"

        // Color progress bar based on status
        if (percentage >= 100) {
            progressBudget.setIndicatorColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
        } else if (percentage >= 80) {
            progressBudget.setIndicatorColor(ContextCompat.getColor(this, android.R.color.holo_orange_light))
        } else {
            progressBudget.setIndicatorColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
        }
    }

    private fun updateStatusChip(status: ChallengeStatus) {
        when (status) {
            ChallengeStatus.PENDING -> {
                chipStatus.text = getString(R.string.challenge_status_pending)
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_orange_light)
            }
            ChallengeStatus.ACTIVE -> {
                chipStatus.text = getString(R.string.challenge_status_active)
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_green_light)
            }
            ChallengeStatus.COMPLETED -> {
                chipStatus.text = getString(R.string.challenge_status_completed)
                chipStatus.setChipBackgroundColorResource(android.R.color.darker_gray)
            }
            ChallengeStatus.CANCELLED -> {
                chipStatus.text = getString(R.string.challenge_status_cancelled)
                chipStatus.setChipBackgroundColorResource(android.R.color.holo_red_light)
            }
        }
    }

    private fun updateActionButtons(challenge: RaceChallenge) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        val isCreator = challenge.createdBy == currentUid

        btnStartChallenge.isVisible = isCreator && challenge.status == ChallengeStatus.PENDING
        btnEndChallenge.isVisible = isCreator && challenge.status == ChallengeStatus.ACTIVE
        btnLeaveChallenge.isVisible = !isCreator && challenge.status != ChallengeStatus.COMPLETED
        btnDeleteChallenge.isVisible = isCreator
    }

    private fun copyInviteCode() {
        val code = tvInviteCode.text.toString()
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Invite Code", code)
        clipboard.setPrimaryClip(clip)

        // Share intent
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "Join my budget challenge! Use code: $code")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(shareIntent, "Share invite code"))

        Snackbar.make(findViewById(android.R.id.content), "Code copied to clipboard", Snackbar.LENGTH_SHORT).show()
    }

    private fun handleUiEvent(event: ChallengeDetailUiEvent) {
        when (event) {
            is ChallengeDetailUiEvent.Message -> {
                Snackbar.make(findViewById(android.R.id.content), event.text, Snackbar.LENGTH_SHORT).show()
            }
            is ChallengeDetailUiEvent.Error -> {
                Snackbar.make(findViewById(android.R.id.content), event.text, Snackbar.LENGTH_LONG).apply {
                    setBackgroundTint(ContextCompat.getColor(this@ChallengeDetailActivity, android.R.color.holo_red_dark))
                }.show()
            }
            is ChallengeDetailUiEvent.NavigateBack -> {
                finish()
            }
            is ChallengeDetailUiEvent.Loading -> {
                loadingOverlay.isVisible = event.isLoading
            }
        }
    }

    private fun showConfirmationDialog(title: String, message: String, onConfirm: () -> Unit) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Confirm") { _, _ -> onConfirm() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun formatDateRange(startDate: String, endDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            
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

// ViewModel for Challenge Detail
sealed class ChallengeDetailUiEvent {
    data class Message(val text: String) : ChallengeDetailUiEvent()
    data class Error(val text: String) : ChallengeDetailUiEvent()
    object NavigateBack : ChallengeDetailUiEvent()
    data class Loading(val isLoading: Boolean) : ChallengeDetailUiEvent()
}

class ChallengeDetailViewModel(
    private val challengeId: String,
    private val raceRepository: UnifiedRaceRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiEvents = MutableSharedFlow<ChallengeDetailUiEvent>()
    val uiEvents: SharedFlow<ChallengeDetailUiEvent> = _uiEvents.asSharedFlow()

    private val _challenge = MutableStateFlow<RaceChallenge?>(null)
    val challenge: StateFlow<RaceChallenge?> = _challenge.asStateFlow()

    private val _leaderboard = MutableStateFlow<List<RaceParticipant>>(emptyList())
    val leaderboard: StateFlow<List<RaceParticipant>> = _leaderboard.asStateFlow()

    private val currentUid: String? = firebaseAuth.currentUser?.uid

    init {
        loadChallengeDetails()
        loadLeaderboard()
        
        // Auto-sync spending when activity starts
        syncSpending()
    }

    private fun loadChallengeDetails() {
        viewModelScope.launch {
            raceRepository.observeChallengeDetails(challengeId)
                .catch { error ->
                    _uiEvents.emit(ChallengeDetailUiEvent.Error(error.message ?: "Failed to load challenge"))
                }
                .collect { challenge ->
                    _challenge.value = challenge
                }
        }
    }

    private fun loadLeaderboard() {
        viewModelScope.launch {
            raceRepository.observeLeaderboard(challengeId)
                .catch { error ->
                    _uiEvents.emit(ChallengeDetailUiEvent.Error(error.message ?: "Failed to load leaderboard"))
                }
                .collect { participants ->
                    _leaderboard.value = participants
                }
        }
    }

    fun syncSpending() {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _uiEvents.emit(ChallengeDetailUiEvent.Loading(true))
            val result = raceRepository.syncMySpendingForActiveChallenges(uid)
            _uiEvents.emit(ChallengeDetailUiEvent.Loading(false))
            result.onSuccess {
                _uiEvents.emit(ChallengeDetailUiEvent.Message("Spending synced"))
            }.onFailure {
                _uiEvents.emit(ChallengeDetailUiEvent.Error(it.message ?: "Failed to sync spending"))
            }
        }
    }

    fun startChallenge() {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _uiEvents.emit(ChallengeDetailUiEvent.Loading(true))
            val result = raceRepository.startChallenge(challengeId, uid)
            _uiEvents.emit(ChallengeDetailUiEvent.Loading(false))
            result.onSuccess {
                _uiEvents.emit(ChallengeDetailUiEvent.Message("Challenge started!"))
            }.onFailure {
                _uiEvents.emit(ChallengeDetailUiEvent.Error(it.message ?: "Failed to start challenge"))
            }
        }
    }

    fun endChallenge() {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _uiEvents.emit(ChallengeDetailUiEvent.Loading(true))
            val result = raceRepository.endChallenge(challengeId, uid)
            _uiEvents.emit(ChallengeDetailUiEvent.Loading(false))
            result.onSuccess {
                _uiEvents.emit(ChallengeDetailUiEvent.Message("Challenge completed!"))
            }.onFailure {
                _uiEvents.emit(ChallengeDetailUiEvent.Error(it.message ?: "Failed to end challenge"))
            }
        }
    }

    fun leaveChallenge() {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _uiEvents.emit(ChallengeDetailUiEvent.Loading(true))
            val result = raceRepository.leaveChallenge(challengeId, uid)
            _uiEvents.emit(ChallengeDetailUiEvent.Loading(false))
            result.onSuccess {
                _uiEvents.emit(ChallengeDetailUiEvent.Message("Left challenge"))
                _uiEvents.emit(ChallengeDetailUiEvent.NavigateBack)
            }.onFailure {
                _uiEvents.emit(ChallengeDetailUiEvent.Error(it.message ?: "Failed to leave challenge"))
            }
        }
    }

    fun deleteChallenge() {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _uiEvents.emit(ChallengeDetailUiEvent.Loading(true))
            val result = raceRepository.deleteChallenge(challengeId, uid)
            _uiEvents.emit(ChallengeDetailUiEvent.Loading(false))
            result.onSuccess {
                _uiEvents.emit(ChallengeDetailUiEvent.Message("Challenge deleted"))
                _uiEvents.emit(ChallengeDetailUiEvent.NavigateBack)
            }.onFailure {
                _uiEvents.emit(ChallengeDetailUiEvent.Error(it.message ?: "Failed to delete challenge"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        raceRepository.cleanup()
    }
}

class ChallengeDetailViewModelFactory(
    private val challengeId: String,
    private val raceRepository: UnifiedRaceRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChallengeDetailViewModel::class.java)) {
            return ChallengeDetailViewModel(challengeId, raceRepository, firebaseAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}