package com.yigitsenal.marketapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.model.ProductDetailResponse
import com.yigitsenal.marketapp.data.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce

class MarketViewModel(private val repository: MarketRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<MarketUiState>(MarketUiState.Loading)
    val uiState: StateFlow<MarketUiState> = _uiState

    private val _newItemText = MutableStateFlow("")
    val newItemText: StateFlow<String> = _newItemText

    // Seçilen ürün için state ekleyelim
    private val _selectedProduct = MutableStateFlow<MarketItem?>(null)
    val selectedProduct: StateFlow<MarketItem?> = _selectedProduct
    
    // Ürün detayları için yeni state
    private val _productDetails = MutableStateFlow<ProductDetailResponse?>(null)
    val productDetails: StateFlow<ProductDetailResponse?> = _productDetails
    
    // API'den ürün yükleniyor mu durumunu tutacak state
    private val _isLoadingProductDetails = MutableStateFlow(false)
    val isLoadingProductDetails: StateFlow<Boolean> = _isLoadingProductDetails

    private var searchJob: Job? = null
    private var productDetailJob: Job? = null

    init {
        loadItems()
        observeSearchText()
    }

    private fun observeSearchText() {
        viewModelScope.launch {
            _newItemText
                .debounce(300)
                .collect { query ->
                    if (query.isNotEmpty()) {
                        searchProducts()
                    } else {
                        loadItems()
                    }
                }
        }
    }

    private fun loadItems() {
        viewModelScope.launch {
            repository.allItems
                .catch { _uiState.value = MarketUiState.Error(it.message ?: "Unknown error") }
                .collect { items ->
                    _uiState.value = MarketUiState.Success(items)
                }
        }
    }

    private fun searchProducts() {
        viewModelScope.launch {
            try {
                _uiState.value = MarketUiState.Loading

                // Türkçe karakterleri düzelt
                val searchQuery = _newItemText.value.replace('ı', 'i')
                    .replace('ğ', 'g')
                    .replace('ü', 'u')
                    .replace('ş', 's')
                    .replace('ö', 'o')
                    .replace('ç', 'c')
                    .replace('İ', 'I')
                    .replace('Ğ', 'G')
                    .replace('Ü', 'U')
                    .replace('Ş', 'S')
                    .replace('Ö', 'O')
                    .replace('Ç', 'C')

                val response = repository.searchProducts(searchQuery)
                _uiState.value = MarketUiState.Success(response.products)
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Error searching products", e)
                _uiState.value = MarketUiState.Error(e.message ?: "Bir hata oluştu")
            }
        }
    }

    fun updateNewItemText(text: String) {
        _newItemText.value = text
    }

    // Seçilen ürünü güncellemek için metodu ekleyelim
    fun setSelectedProduct(product: MarketItem?) {
        _selectedProduct.value = product
        
        // Eğer ürün seçildiyse ve URL değeri varsa, detayları yüklemeyi deneyebiliriz
        if (product != null && product.url.isNotEmpty()) {
            loadProductDetails(product.url)
        }
    }
    
    // Ürün detaylarını API'den yüklemek için metodu güncelleyelim
    private fun loadProductDetails(url: String) {
        productDetailJob?.cancel()
        productDetailJob = viewModelScope.launch {
            try {
                _isLoadingProductDetails.value = true
                Log.d("MarketViewModel", "Loading product details for URL: $url")
                
                // URL'den path parametresini çıkar
                val path = if (url.contains("?path=")) {
                    url.substringAfter("?path=")
                } else {
                    // URL'yi doğrudan kullan
                    url
                }
                
                Log.d("MarketViewModel", "Using path: $path")
                
                // API'ye path'i gönder
                val detailResponse = repository.getProductDetails(path)
                Log.d("MarketViewModel", "Product details loaded successfully: ${detailResponse.product.name}")
                
                // Ürün detaylarını state'e kaydet
                _productDetails.value = detailResponse
                
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Error loading product details", e)
                Log.e("MarketViewModel", "Error message: ${e.message}")
                Log.e("MarketViewModel", "Error cause: ${e.cause}")
                Log.e("MarketViewModel", "Error stack trace: ${e.stackTraceToString()}")
                e.printStackTrace()
                _productDetails.value = null
            } finally {
                _isLoadingProductDetails.value = false
            }
        }
    }

    fun toggleItemCompletion(item: MarketItem) {
        viewModelScope.launch {
            repository.updateItem(item)
        }
    }

    fun deleteItem(item: MarketItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
        }
    }

    fun updateItemQuantity(item: MarketItem, quantity: Int) {
        if (quantity >= 1) {
            viewModelScope.launch {
                repository.updateItem(item.copy(quantity = quantity))
            }
        }
    }
}

sealed class MarketUiState {
    object Loading : MarketUiState()
    data class Success(val items: List<MarketItem>) : MarketUiState()
    data class Error(val message: String) : MarketUiState()
}

class MarketViewModelFactory(private val repository: MarketRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarketViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}