package com.yigitsenal.marketapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.yigitsenal.marketapp.data.repository.AuthRepository
import com.yigitsenal.marketapp.ui.state.AuthState
import com.yigitsenal.marketapp.ui.state.AuthUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    init {
        checkAuthState()
    }

    private fun checkAuthState() {
        val currentUser = authRepository.currentUser
        if (currentUser != null) {            _authState.value = _authState.value.copy(
                isAuthenticated = true,
                user = AuthUser(
                    uid = currentUser.uid,
                    email = currentUser.email,
                    displayName = currentUser.displayName,
                    photoUrl = currentUser.photoUrl?.toString()
                )
            )
        } else {
            _authState.value = _authState.value.copy(
                isAuthenticated = false,
                user = null
            )
        }
    }    fun signOut() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
                // State'i temizle
                _authState.value = AuthState(
                    isAuthenticated = false, 
                    user = null,
                    isLoading = false,
                    error = null
                )
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    error = e.message
                )
            }
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }

    fun reloadUserProfile() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.currentUser
                if (currentUser != null) {
                    // Reload Firebase user to get latest data
                    currentUser.reload().await()
                    checkAuthState()
                }
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    error = e.message
                )
            }
        }
    }

    fun refreshAuthState() {
        checkAuthState()
    }
}
