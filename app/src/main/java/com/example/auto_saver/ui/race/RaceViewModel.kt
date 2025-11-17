package com.example.auto_saver.ui.race

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auto_saver.UserPreferences
import com.example.auto_saver.data.model.ChallengeStatus
import com.example.auto_saver.data.model.RaceChallenge
import com.example.auto_saver.data.repository.UnifiedRaceRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

sealed class RaceUiState {
    object Loading : RaceUiState()
    data class Success(
        val activeChallenges: List<RaceChallenge>,
        val completedChallenges: List<RaceChallenge>
    ) : RaceUiState()
    data class Error(val message: String) : RaceUiState()
    object Empty : RaceUiState()
}

sealed class RaceUiEvent {
    data class Error(val text: String) : RaceUiEvent()
    data class Message(val text: String) : RaceUiEvent()
    data class NavigateToDetail(val challengeId: String) : RaceUiEvent()
}

class RaceViewModel(
    private val raceRepository: UnifiedRaceRepository,
    private val userPreferences: UserPreferences,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow<RaceUiState>(RaceUiState.Loading)
    val uiState: StateFlow<RaceUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<RaceUiEvent>()
    val uiEvents: SharedFlow<RaceUiEvent> = _uiEvents.asSharedFlow()

    val sessionActive: Boolean
        get() = firebaseAuth.currentUser != null

    private val currentUid: String?
        get() = firebaseAuth.currentUser?.uid

    init {
        loadChallenges()
    }

    fun loadChallenges() {
        val uid = currentUid
        if (uid == null) {
            _uiState.value = RaceUiState.Error("Not logged in")
            return
        }

        viewModelScope.launch {
            _uiState.value = RaceUiState.Loading
            
            raceRepository.observeMyChallenges(uid)
                .catch { error ->
                    _uiState.value = RaceUiState.Error(error.message ?: "Failed to load challenges")
                }
                .collect { challenges ->
                    if (challenges.isEmpty()) {
                        _uiState.value = RaceUiState.Empty
                    } else {
                        val active = challenges.filter { 
                            it.status == ChallengeStatus.ACTIVE || it.status == ChallengeStatus.PENDING 
                        }
                        val completed = challenges.filter { 
                            it.status == ChallengeStatus.COMPLETED || it.status == ChallengeStatus.CANCELLED 
                        }
                        _uiState.value = RaceUiState.Success(active, completed)
                    }
                }
        }
    }

    fun createChallenge(name: String, budget: Double, startDate: String, endDate: String) {
        val uid = currentUid
        val email = firebaseAuth.currentUser?.email
        
        if (uid == null || email == null) {
            viewModelScope.launch {
                _uiEvents.emit(RaceUiEvent.Error("Not logged in"))
            }
            return
        }

        if (name.isBlank()) {
            viewModelScope.launch {
                _uiEvents.emit(RaceUiEvent.Error("Challenge name is required"))
            }
            return
        }

        if (budget <= 0) {
            viewModelScope.launch {
                _uiEvents.emit(RaceUiEvent.Error("Budget must be greater than 0"))
            }
            return
        }

        if (startDate >= endDate) {
            viewModelScope.launch {
                _uiEvents.emit(RaceUiEvent.Error("End date must be after start date"))
            }
            return
        }

        viewModelScope.launch {
            val challenge = RaceChallenge(
                id = "",
                name = name,
                createdBy = uid,
                createdByEmail = email,
                budget = budget,
                startDate = startDate,
                endDate = endDate,
                status = ChallengeStatus.PENDING,
                participants = emptyList(),
                inviteCode = "",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            val result = raceRepository.createChallenge(challenge)
            result.onSuccess { challengeId ->
                _uiEvents.emit(RaceUiEvent.Message("Challenge created successfully!"))
                _uiEvents.emit(RaceUiEvent.NavigateToDetail(challengeId))
            }.onFailure { error ->
                _uiEvents.emit(RaceUiEvent.Error(error.message ?: "Failed to create challenge"))
            }
        }
    }

    fun joinChallengeByCode(inviteCode: String) {
        val uid = currentUid
        val email = firebaseAuth.currentUser?.email
        
        if (uid == null || email == null) {
            viewModelScope.launch {
                _uiEvents.emit(RaceUiEvent.Error("Not logged in"))
            }
            return
        }

        if (inviteCode.isBlank()) {
            viewModelScope.launch {
                _uiEvents.emit(RaceUiEvent.Error("Invite code is required"))
            }
            return
        }

        viewModelScope.launch {
            val displayName = userPreferences.getUserName()
            val result = raceRepository.joinChallengeByCode(uid, email, displayName, inviteCode)
            
            result.onSuccess { challengeId ->
                _uiEvents.emit(RaceUiEvent.Message("Joined challenge successfully!"))
                _uiEvents.emit(RaceUiEvent.NavigateToDetail(challengeId))
            }.onFailure { error ->
                _uiEvents.emit(RaceUiEvent.Error(error.message ?: "Failed to join challenge"))
            }
        }
    }

    fun leaveChallenge(challengeId: String) {
        val uid = currentUid ?: return

        viewModelScope.launch {
            val result = raceRepository.leaveChallenge(challengeId, uid)
            result.onSuccess {
                _uiEvents.emit(RaceUiEvent.Message("Left challenge"))
            }.onFailure { error ->
                _uiEvents.emit(RaceUiEvent.Error(error.message ?: "Failed to leave challenge"))
            }
        }
    }

    fun deleteChallenge(challengeId: String) {
        val uid = currentUid ?: return

        viewModelScope.launch {
            val result = raceRepository.deleteChallenge(challengeId, uid)
            result.onSuccess {
                _uiEvents.emit(RaceUiEvent.Message("Challenge deleted"))
            }.onFailure { error ->
                _uiEvents.emit(RaceUiEvent.Error(error.message ?: "Failed to delete challenge"))
            }
        }
    }

    fun syncSpending() {
        val uid = currentUid ?: return

        viewModelScope.launch {
            val result = raceRepository.syncMySpendingForActiveChallenges(uid)
            result.onSuccess {
                _uiEvents.emit(RaceUiEvent.Message("Spending synced"))
            }.onFailure { error ->
                _uiEvents.emit(RaceUiEvent.Error(error.message ?: "Failed to sync spending"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        raceRepository.cleanup()
    }
}

class RaceViewModelFactory(
    private val raceRepository: UnifiedRaceRepository,
    private val userPreferences: UserPreferences,
    private val firebaseAuth: FirebaseAuth
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RaceViewModel::class.java)) {
            return RaceViewModel(raceRepository, userPreferences, firebaseAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}