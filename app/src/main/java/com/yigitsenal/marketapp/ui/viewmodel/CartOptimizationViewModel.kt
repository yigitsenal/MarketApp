package com.yigitsenal.marketapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yigitsenal.marketapp.data.model.StoreOptimization
import com.yigitsenal.marketapp.data.repository.CartOptimizationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class OptimizationUiState(
    val isLoading: Boolean = false,
    val optimization: StoreOptimization? = null,
    val error: String? = null,
    val isAutoOptimizationEnabled: Boolean = true
)

class CartOptimizationViewModel(
    private val repository: CartOptimizationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OptimizationUiState())
    val uiState: StateFlow<OptimizationUiState> = _uiState.asStateFlow()
    
    private var currentListId: Int? = null
    
    fun optimizeCart(listId: Int) {
        currentListId = listId
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        
        viewModelScope.launch {
            try {
                repository.optimizeShoppingCart(listId).collect { optimization ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        optimization = optimization,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Optimizasyon sırasında hata oluştu: ${e.message}",
                    optimization = null
                )
            }
        }
    }
    
    fun refreshOptimization() {
        currentListId?.let { listId ->
            // Önce cache'i temizle, sonra optimizasyonu temizle ve yeniden hesapla
            _uiState.value = _uiState.value.copy(
                optimization = null,
                isLoading = true,
                error = null
            )
            // currentListId'yi null yaparak optimizeCart'ın yeniden çalışmasını sağla
            val tempListId = currentListId
            currentListId = null
            tempListId?.let { optimizeCart(it) }
        }
    }
    
    fun toggleAutoOptimization() {
        _uiState.value = _uiState.value.copy(
            isAutoOptimizationEnabled = !_uiState.value.isAutoOptimizationEnabled
        )
    }
    
    fun clearOptimization() {
        _uiState.value = _uiState.value.copy(
            optimization = null,
            error = null,
            isLoading = false
        )
        currentListId = null
    }
    
    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

class CartOptimizationViewModelFactory(
    private val repository: CartOptimizationRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CartOptimizationViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CartOptimizationViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
