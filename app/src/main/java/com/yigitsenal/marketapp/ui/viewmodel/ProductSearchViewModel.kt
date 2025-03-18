package com.yigitsenal.marketapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.model.ProductDetailResponse
import com.yigitsenal.marketapp.data.repository.MarketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductSearchViewModel(private val repository: MarketRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _sortOption = MutableStateFlow<String?>(null)
    val sortOption: StateFlow<String?> = _sortOption

    private val _selectedProduct = MutableStateFlow<MarketItem?>(null)
    val selectedProduct: StateFlow<MarketItem?> = _selectedProduct

    // Ürün detaylarını saklamak için yeni bir state ekliyoruz
    private val _productDetails = MutableStateFlow<ProductDetailResponse?>(null)
    val productDetails: StateFlow<ProductDetailResponse?> = _productDetails

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(sort: String?) {
        _sortOption.value = sort
    }

    fun setSelectedProduct(product: MarketItem?) {
        _selectedProduct.value = product

        // Eğer seçilen ürün varsa ve URL'si doluysa detayları yüklüyoruz
        if (product != null && product.url.isNotEmpty()) {
            loadProductDetails(product.url)
        } else {
            // Seçilen ürün yoksa ürün detaylarını sıfırlıyoruz
            _productDetails.value = null
        }
    }

    fun searchProducts(): Flow<PagingData<MarketItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                ProductPagingSource(
                    repository = repository,
                    query = _searchQuery.value,
                    sort = _sortOption.value
                )
            }
        ).flow.cachedIn(viewModelScope)
    }

    fun loadProductDetails(productPath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Detayları ayrı bir state'de saklıyoruz
                val productDetailResponse = repository.getProductDetails(productPath)
                _productDetails.value = productDetailResponse

                // Burada ürünün basit bilgilerini _selectedProduct'a atamıyoruz
                // çünkü zaten setSelectedProduct tarafından atanmış durumda
            } catch (e: Exception) {
                // Hata yönetimi
                _productDetails.value = null
            } finally {
                _isLoading.value = false
            }
        }
    }
}