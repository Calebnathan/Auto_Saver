package com.example.auto_saver.ui.social

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.auto_saver.UserPreferences
import com.example.auto_saver.data.model.FriendProfile
import com.example.auto_saver.data.model.FriendRequest
import com.example.auto_saver.data.repository.UnifiedFriendRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SocialViewModel(
    private val friendRepository: UnifiedFriendRepository,
    private val userPreferences: UserPreferences,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val currentUid: String? = firebaseAuth.currentUser?.uid
    val sessionActive: Boolean = currentUid != null
    private val _uiEvents = MutableSharedFlow<UiEvent>(extraBufferCapacity = 1)
    val uiEvents = _uiEvents.asSharedFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    val friends: StateFlow<List<FriendProfile>> = if (currentUid != null && firebaseAuth.currentUser != null) {
        friendRepository.observeFriends(currentUid)
            .catch { error ->
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.Error("Failed to load friends: ${error.localizedMessage}"))
                }
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    val friendRequests: StateFlow<List<FriendRequest>> = if (currentUid != null && firebaseAuth.currentUser != null) {
        friendRepository.observeFriendRequests(currentUid)
            .catch { error ->
                viewModelScope.launch {
                    _uiEvents.emit(UiEvent.Error("Failed to load friend requests: ${error.localizedMessage}"))
                }
                emit(emptyList())
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    init {
        if (currentUid == null || firebaseAuth.currentUser == null) {
            viewModelScope.launch {
                _uiEvents.emit(UiEvent.Error("Session expired. Please log in again."))
            }
        }
    }

    fun sendFriendInvite(email: String) {
        val uid = currentUid ?: return
        val sanitized = email.trim()
        if (!Patterns.EMAIL_ADDRESS.matcher(sanitized).matches()) {
            viewModelScope.launch { _uiEvents.emit(UiEvent.Error("Enter a valid email address")) }
            return
        }
        val currentEmail = firebaseAuth.currentUser?.email
        if (currentEmail.isNullOrBlank()) {
            viewModelScope.launch { _uiEvents.emit(UiEvent.Error("Unable to find your email. Please re-authenticate.")) }
            return
        }
        viewModelScope.launch {
            _isProcessing.value = true
            val result = friendRepository.sendFriendRequest(uid, currentEmail, sanitized)
            _isProcessing.value = false
            result.onSuccess {
                _uiEvents.emit(UiEvent.Message("Invite sent to $sanitized"))
            }.onFailure {
                _uiEvents.emit(UiEvent.Error(it.localizedMessage ?: "Unable to send invite"))
            }
        }
    }

    fun acceptFriend(requestId: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _isProcessing.value = true
            val result = friendRepository.acceptFriendRequest(uid, requestId)
            _isProcessing.value = false
            result.onSuccess {
                _uiEvents.emit(UiEvent.Message("Friend added"))
            }.onFailure {
                _uiEvents.emit(UiEvent.Error(it.localizedMessage ?: "Unable to accept request"))
            }
        }
    }

    fun declineFriend(requestId: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            val result = friendRepository.declineFriendRequest(uid, requestId)
            result.onSuccess {
                _uiEvents.emit(UiEvent.Message("Request declined"))
            }.onFailure {
                _uiEvents.emit(UiEvent.Error(it.localizedMessage ?: "Unable to decline request"))
            }
        }
    }

    fun removeFriend(friendUid: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            val result = friendRepository.removeFriend(uid, friendUid)
            result.onSuccess {
                _uiEvents.emit(UiEvent.Message("Friend removed"))
            }.onFailure {
                _uiEvents.emit(UiEvent.Error(it.localizedMessage ?: "Unable to remove friend"))
            }
        }
    }

    sealed class UiEvent {
        data class Message(val text: String) : UiEvent()
        data class Error(val text: String) : UiEvent()
    }
}

class SocialViewModelFactory(
    private val friendRepository: UnifiedFriendRepository,
    private val userPreferences: UserPreferences,
    private val firebaseAuth: FirebaseAuth
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SocialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SocialViewModel(friendRepository, userPreferences, firebaseAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}