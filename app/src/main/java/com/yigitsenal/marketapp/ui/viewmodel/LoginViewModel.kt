package com.yigitsenal.marketapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yigitsenal.marketapp.data.repository.AuthRepository
import com.yigitsenal.marketapp.ui.state.LoginState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun updateEmail(email: String) {
        _loginState.value = _loginState.value.copy(email = email)
    }

    fun updatePassword(password: String) {
        _loginState.value = _loginState.value.copy(password = password)
    }

    fun togglePasswordVisibility() {
        _loginState.value = _loginState.value.copy(
            isPasswordVisible = !_loginState.value.isPasswordVisible
        )
    }

    fun signInWithEmail(onSuccess: () -> Unit) {
        if (!isValidInput()) return

        viewModelScope.launch {
            _loginState.value = _loginState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.signInWithEmail(
                _loginState.value.email.trim(),
                _loginState.value.password
            )
            
            result.fold(
                onSuccess = {
                    _loginState.value = _loginState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { exception ->
                    _loginState.value = _loginState.value.copy(
                        isLoading = false,
                        error = getErrorMessage(exception)
                    )
                }
            )
        }
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _loginState.value = _loginState.value.copy(isLoading = true, error = null)
            
            val result = authRepository.signInWithGoogle(idToken)
            
            result.fold(
                onSuccess = {
                    _loginState.value = _loginState.value.copy(isLoading = false)
                    onSuccess()
                },
                onFailure = { exception ->
                    _loginState.value = _loginState.value.copy(
                        isLoading = false,
                        error = getErrorMessage(exception)
                    )
                }
            )
        }
    }

    private fun isValidInput(): Boolean {
        val state = _loginState.value
        
        when {
            state.email.isBlank() -> {
                _loginState.value = state.copy(error = "Email gerekli")
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches() -> {
                _loginState.value = state.copy(error = "Geçerli bir email adresi girin")
                return false
            }
            state.password.isBlank() -> {
                _loginState.value = state.copy(error = "Şifre gerekli")
                return false
            }
            state.password.length < 6 -> {
                _loginState.value = state.copy(error = "Şifre en az 6 karakter olmalı")
                return false
            }
        }
        return true
    }

    private fun getErrorMessage(exception: Throwable): String {
        return when (exception.message) {
            "The email address is badly formatted." -> "Geçersiz email formatı"
            "There is no user record corresponding to this identifier. The user may have been deleted." -> "Bu email ile kayıtlı kullanıcı bulunamadı"
            "The password is invalid or the user does not have a password." -> "Geçersiz şifre"
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Ağ bağlantısı hatası"
            "Too many unsuccessful login attempts. Please try again later." -> "Çok fazla başarısız giriş denemesi. Lütfen daha sonra tekrar deneyin"
            else -> exception.message ?: "Bilinmeyen hata oluştu"
        }
    }

    fun clearError() {
        _loginState.value = _loginState.value.copy(error = null)
    }
}
