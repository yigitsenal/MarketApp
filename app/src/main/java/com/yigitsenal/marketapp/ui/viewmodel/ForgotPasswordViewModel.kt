package com.yigitsenal.marketapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yigitsenal.marketapp.data.repository.AuthRepository
import com.yigitsenal.marketapp.ui.state.ForgotPasswordState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _forgotPasswordState = MutableStateFlow(ForgotPasswordState())
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    fun updateEmail(email: String) {
        _forgotPasswordState.value = _forgotPasswordState.value.copy(email = email)
    }

    fun resetPassword() {
        if (!isValidInput()) return

        viewModelScope.launch {
            _forgotPasswordState.value = _forgotPasswordState.value.copy(
                isLoading = true, 
                error = null
            )
            
            val result = authRepository.resetPassword(_forgotPasswordState.value.email.trim())
            
            result.fold(
                onSuccess = {
                    _forgotPasswordState.value = _forgotPasswordState.value.copy(
                        isLoading = false,
                        isEmailSent = true
                    )
                },
                onFailure = { exception ->
                    _forgotPasswordState.value = _forgotPasswordState.value.copy(
                        isLoading = false,
                        error = getErrorMessage(exception)
                    )
                }
            )
        }
    }

    private fun isValidInput(): Boolean {
        val state = _forgotPasswordState.value
        
        when {
            state.email.isBlank() -> {
                _forgotPasswordState.value = state.copy(error = "Email gerekli")
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches() -> {
                _forgotPasswordState.value = state.copy(error = "Geçerli bir email adresi girin")
                return false
            }
        }
        return true
    }

    private fun getErrorMessage(exception: Throwable): String {
        return when (exception.message) {
            "There is no user record corresponding to this identifier. The user may have been deleted." -> "Bu email ile kayıtlı kullanıcı bulunamadı"
            "The email address is badly formatted." -> "Geçersiz email formatı"
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> "Ağ bağlantısı hatası"
            else -> exception.message ?: "Bilinmeyen hata oluştu"
        }
    }

    fun clearError() {
        _forgotPasswordState.value = _forgotPasswordState.value.copy(error = null)
    }

    fun resetState() {
        _forgotPasswordState.value = ForgotPasswordState()
    }
}
