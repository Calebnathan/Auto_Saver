package com.example.auto_saver.ui.race

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.auto_saver.MyApplication
import com.example.auto_saver.R
import com.example.auto_saver.collectWithLifecycle
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class RaceFragment : Fragment() {

    private val raceViewModel: RaceViewModel by viewModels {
        RaceViewModelFactory(
            raceRepository = MyApplication.raceRepository,
            userPreferences = MyApplication.userPreferences,
            firebaseAuth = FirebaseAuth.getInstance()
        )
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutLoading: LinearLayout
    private lateinit var cardEmptyState: MaterialCardView
    private lateinit var cardErrorState: MaterialCardView
    private lateinit var tvErrorMessage: TextView
    private lateinit var btnRetry: MaterialButton
    private lateinit var layoutActiveSection: LinearLayout
    private lateinit var layoutCompletedSection: LinearLayout
    private lateinit var rvActiveChallenges: RecyclerView
    private lateinit var rvCompletedChallenges: RecyclerView
    private lateinit var fabCreateChallenge: ExtendedFloatingActionButton
    private lateinit var fabJoinChallenge: com.google.android.material.floatingactionbutton.FloatingActionButton

    private lateinit var activeChallengeAdapter: ChallengeAdapter
    private lateinit var completedChallengeAdapter: ChallengeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_race, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        setupRecyclerViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun initializeViews(view: View) {
        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        layoutLoading = view.findViewById(R.id.layout_loading)
        cardEmptyState = view.findViewById(R.id.card_empty_state)
        cardErrorState = view.findViewById(R.id.card_error_state)
        tvErrorMessage = view.findViewById(R.id.tv_error_message)
        btnRetry = view.findViewById(R.id.btn_retry)
        layoutActiveSection = view.findViewById(R.id.layout_active_section)
        layoutCompletedSection = view.findViewById(R.id.layout_completed_section)
        rvActiveChallenges = view.findViewById(R.id.rv_active_challenges)
        rvCompletedChallenges = view.findViewById(R.id.rv_completed_challenges)
        fabCreateChallenge = view.findViewById(R.id.fab_create_challenge)
        fabJoinChallenge = view.findViewById(R.id.fab_join_challenge)
    }

    private fun setupRecyclerViews() {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid

        activeChallengeAdapter = ChallengeAdapter(
            onChallengeClick = { challenge ->
                navigateToChallengeDetail(challenge.id)
            },
            currentUserUid = currentUid
        )

        completedChallengeAdapter = ChallengeAdapter(
            onChallengeClick = { challenge ->
                navigateToChallengeDetail(challenge.id)
            },
            currentUserUid = currentUid
        )

        rvActiveChallenges.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = activeChallengeAdapter
        }

        rvCompletedChallenges.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = completedChallengeAdapter
        }
    }

    private fun setupClickListeners() {
        swipeRefresh.setOnRefreshListener {
            raceViewModel.loadChallenges()
            swipeRefresh.isRefreshing = false
        }

        fabCreateChallenge.setOnClickListener {
            if (raceViewModel.sessionActive) {
                showCreateChallengeDialog()
            } else {
                showError("Please log in to create challenges")
            }
        }

        fabJoinChallenge.setOnClickListener {
            if (raceViewModel.sessionActive) {
                showJoinChallengeDialog()
            } else {
                showError("Please log in to join challenges")
            }
        }

        btnRetry.setOnClickListener {
            raceViewModel.loadChallenges()
        }
    }

    private fun observeViewModel() {
        raceViewModel.uiState.collectWithLifecycle(viewLifecycleOwner) { state ->
            updateUI(state)
        }

        raceViewModel.uiEvents.collectWithLifecycle(viewLifecycleOwner) { event ->
            handleUiEvent(event)
        }
    }

    private fun updateUI(state: RaceUiState) {
        // Hide all states first
        layoutLoading.isVisible = false
        cardEmptyState.isVisible = false
        cardErrorState.isVisible = false
        layoutActiveSection.isVisible = false
        layoutCompletedSection.isVisible = false

        when (state) {
            is RaceUiState.Loading -> {
                layoutLoading.isVisible = true
            }
            is RaceUiState.Empty -> {
                cardEmptyState.isVisible = true
            }
            is RaceUiState.Error -> {
                cardErrorState.isVisible = true
                tvErrorMessage.text = state.message
            }
            is RaceUiState.Success -> {
                // Show active challenges if any
                if (state.activeChallenges.isNotEmpty()) {
                    layoutActiveSection.isVisible = true
                    activeChallengeAdapter.submitList(state.activeChallenges)
                }

                // Show completed challenges if any
                if (state.completedChallenges.isNotEmpty()) {
                    layoutCompletedSection.isVisible = true
                    completedChallengeAdapter.submitList(state.completedChallenges)
                }

                // If neither section has content, show empty state
                if (state.activeChallenges.isEmpty() && state.completedChallenges.isEmpty()) {
                    cardEmptyState.isVisible = true
                }
            }
        }
    }

    private fun handleUiEvent(event: RaceUiEvent) {
        when (event) {
            is RaceUiEvent.Error -> {
                showError(event.text)
            }
            is RaceUiEvent.Message -> {
                showMessage(event.text)
            }
            is RaceUiEvent.NavigateToDetail -> {
                navigateToChallengeDetail(event.challengeId)
            }
        }
    }

    private fun showCreateChallengeDialog() {
        val dialog = CreateChallengeBottomSheet()
        dialog.show(childFragmentManager, "CreateChallenge")
    }

    private fun showJoinChallengeDialog() {
        val dialog = JoinChallengeBottomSheet()
        dialog.show(childFragmentManager, "JoinChallenge")
    }

    private fun navigateToChallengeDetail(challengeId: String) {
        val intent = Intent(requireContext(), ChallengeDetailActivity::class.java).apply {
            putExtra("CHALLENGE_ID", challengeId)
        }
        startActivity(intent)
    }

    private fun showError(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).apply {
                setBackgroundTint(ContextCompat.getColor(requireContext(), com.google.android.material.R.color.design_default_color_error))
            }.show()
        }
    }

    private fun showMessage(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh challenges when returning to fragment
        raceViewModel.loadChallenges()
    }
}