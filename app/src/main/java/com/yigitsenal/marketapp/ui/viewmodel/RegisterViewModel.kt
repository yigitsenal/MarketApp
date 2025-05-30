package com.yigitsenal.marketapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yigitsenal.marketapp.data.repository.AuthRepository
import com.yigitsenal.marketapp.ui.state.RegisterState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _registerState = MutableStateFlow(RegisterState())
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()
    fun updateDisplayName(displayName: String) {
        _registerState.value = _registerState.value.copy(displayName = displayName)
    }

    fun updateEmail(email: String) {
        _registerState.value = _registerState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _registerState.value = _registerState.value.copy(password = password)
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _registerState.value = _registerState.value.copy(confirmPassword = confirmPassword)
    }

    fun togglePasswordVisibility() {
        _registerState.value = _registerState.value.copy(
            isPasswordVisible = !_registerState.value.isPasswordVisible
        )
    }

    fun toggleConfirmPasswordVisibility() {
        _registerState.value = _registerState.value.copy(
            isConfirmPasswordVisible = !_registerState.value.isConfirmPasswordVisible
        )
    }

    fun signUpWithEmail(onSuccess: () -> Unit) {
        if (!isValidInput()) return

        viewModelScope.launch {
            _registerState.value = _registerState.value.copy(isLoading = true, error = null)
              val result = authRepository.signUpWithEmail(
                _registerState.value.email.trim(),
                _registerState.value.password,
                _registerState.value.displayName.trim()
            )
            
            result.fold(
                onSuccess = {
                    _registerState.value = _registerState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { exception ->
                    _registerState.value = _registerState.value.copy(
                        isLoading = false,
                        error = getErrorMessage(exception)
                    )
                }
            )
        }
    }

    private fun isValidInput(): Boolean {
        val state = _registerState.value
          when {
            state.displayName.isBlank() -> {
                _registerState.value = state.copy(error = "Ad Soyad gerekli")
                return false
            }
            state.displayName.length < 2 -> {
                _registerState.value = state.copy(error = "Ad Soyad en az 2 karakter olmalı")
                return false
            }
            state.email.isBlank() -> {
                _registerState.value = state.copy(error = "Email gerekli")
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches() -> {
                _registerState.value = state.copy(error = "Geçerli bir email adresi girin")
                return false
            }
            state.password.isBlank() -> {
                _registerState.value = state.copy(error = "Şifre gerekli")
                return false
            }
            state.password.length < 6 -> {
                _registerState.value = state.copy(error = "Şifre en az 6 karakter olmalı")
                return false
            }
            state.confirmPassword.isBlank() -> {
                _registerState.value = state.copy(error = "Şifre tekrarı gerekli")
                return false
            }
            state.password != state.confirmPassword -> {
                _registerState.value = state.copy(error = "Şifreler eşleşmiyor")
                return false
            }
        }
        return true
    }

    private fun getErrorMessage(exception: Throwable): String {
        return when (exception.message) {
            "The email address is already in use by another account." -> "Bu email adresi zaten kullanımda"
            "The email address is badly formatted." -> "Geçersiz email formatı"
            "The given password is invalid. [ Password should be at least 6 characters ]" -> "Şifre en az 6 karakter olmalı"
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Ağ bağlantısı hatası"
            else -> exception.message ?: "Bilinmeyen hata oluştu"
        }
    }

    fun clearError() {
        _registerState.value = _registerState.value.copy(error = null)
    }
}
