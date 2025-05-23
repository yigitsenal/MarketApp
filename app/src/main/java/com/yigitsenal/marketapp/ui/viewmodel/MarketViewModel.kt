package com.yigitsenal.marketapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.model.Offer
import com.yigitsenal.marketapp.data.model.ProductDetailResponse
import com.yigitsenal.marketapp.data.network.PriceHistoryEntry
import com.yigitsenal.marketapp.data.network.PricePredictionApiService
import com.yigitsenal.marketapp.data.network.PricePredictionResult
import com.yigitsenal.marketapp.data.repository.MarketRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import java.util.regex.Pattern

class MarketViewModel(private val repository: MarketRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<MarketUiState>(MarketUiState.Loading)
    val uiState: StateFlow<MarketUiState> = _uiState

    private val _newItemText = MutableStateFlow("")
    val newItemText: StateFlow<String> = _newItemText

    // Pagination için state'ler
    private var currentPage = 1
    private var isLastPage = false
    private var isLoading = false
    private val _products = MutableStateFlow<List<MarketItem>>(emptyList())

    // Seçilen ürün için state ekleyelim
    private val _selectedProduct = MutableStateFlow<MarketItem?>(null)
    val selectedProduct: StateFlow<MarketItem?> = _selectedProduct
    
    // Ürün detayları için yeni state
    private val _productDetails = MutableStateFlow<ProductDetailResponse?>(null)
    val productDetails: StateFlow<ProductDetailResponse?> = _productDetails
    
    // API'den ürün yükleniyor mu durumunu tutacak state
    private val _isLoadingProductDetails = MutableStateFlow(false)
    val isLoadingProductDetails: StateFlow<Boolean> = _isLoadingProductDetails

    // Sıralama seçeneği için state
    private val _sortOption = MutableStateFlow<String?>(null)
    val sortOption: StateFlow<String?> = _sortOption
    
    // Fiyat tahmini için state
    private val _pricePrediction = MutableStateFlow<PricePredictionResult?>(null)
    val pricePrediction: StateFlow<PricePredictionResult?> = _pricePrediction
    
    // Fiyat tahmini yükleniyor mu durumunu tutacak state
    private val _isLoadingPricePrediction = MutableStateFlow(false)
    val isLoadingPricePrediction: StateFlow<Boolean> = _isLoadingPricePrediction

    private var searchJob: Job? = null
    private var productDetailJob: Job? = null
    private var pricePredictionJob: Job? = null
    
    private val pricePredictionService = PricePredictionApiService.create()

    init {
        observeSearchText()
    }

    private fun observeSearchText() {
        viewModelScope.launch {
            _newItemText
                .debounce(300) // Kullanıcı yazarken her harf için arama yapmayı engelle
                .collect { query ->
                    if (query.isNotEmpty() && query.length > 1) { // En az 2 karakter olmalı
                        searchProducts(query)
                    } else if (query.isEmpty()) {
                        _products.value = emptyList()
                        _uiState.value = MarketUiState.Success(emptyList())
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

    fun updateNewItemText(text: String) {
        _newItemText.value = text
    }

    fun searchProducts(query: String, isNewSearch: Boolean = true) {
        // Önceki aramayı iptal et
        searchJob?.cancel()
        
        // Çok kısa sorgular için arama yapma
        if (query.trim().length < 1) {
            if (isNewSearch) {
                _uiState.value = MarketUiState.Success(emptyList())
            }
            return
        }
        
        // Yeni arama ise sayfayı sıfırla
        if (isNewSearch) {
            currentPage = 1
            isLastPage = false
            _products.value = emptyList()
            _uiState.value = MarketUiState.Loading
        }
        
        // Eğer son sayfaya ulaşıldıysa veya yükleme devam ediyorsa yeni sorgu yapma
        if (isLastPage || isLoading) return

        searchJob = viewModelScope.launch {
            try {
                isLoading = true
                
                val response = repository.searchProducts(
                    query = query,
                    sort = _sortOption.value,
                    page = currentPage
                )
                
                if (response.success) {
                    val newProducts = response.products ?: emptyList()
                    
                    // Son sayfa kontrolü
                    if (newProducts.isEmpty()) {
                        isLastPage = true
                    } else {
                        currentPage++
                        // Mevcut listeye yeni ürünleri ekle
                        val updatedProducts = if (isNewSearch) {
                            newProducts
                        } else {
                            _products.value + newProducts
                        }
                        
                        _products.value = updatedProducts
                        _uiState.value = MarketUiState.Success(updatedProducts)
                        Log.d("MarketViewModel", "Ürünler güncellendi. Toplam: ${updatedProducts.size}")
                    }
                } else {
                    if (isNewSearch) {
                        _uiState.value = MarketUiState.Success(emptyList())
                        Log.d("MarketViewModel", "Arama başarısız oldu veya sonuç bulunamadı")
                    }
                }
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Error searching products", e)
                if (isNewSearch) {
                    _uiState.value = MarketUiState.Error("Arama sırasında bir hata oluştu: ${e.message}")
                }
            } finally {
                isLoading = false
            }
        }
    }

    // Daha fazla ürün yüklemek için fonksiyon
    fun loadMoreProducts() {
        if (!isLoading && !isLastPage && _newItemText.value.isNotEmpty()) {
            Log.d("MarketViewModel", "Daha fazla ürün yükleniyor. Sayfa: $currentPage")
            searchProducts(_newItemText.value, false)
        }
    }

    // Sıralama seçeneğini güncellemek için fonksiyon
    fun updateSortOption(sort: String?) {
        _sortOption.value = sort
        if (_newItemText.value.isNotEmpty()) {
            // Sıralama değiştiğinde mevcut aramanın ilk sayfasından başla
            currentPage = 1
            isLastPage = false
            _products.value = emptyList()
            searchProducts(_newItemText.value)
        }
    }

    // Seçilen ürünü güncellemek için metodu ekleyelim
    fun setSelectedProduct(product: MarketItem?) {
        _selectedProduct.value = product
        // Ürün değiştiğinde detayları yükle
        if (product != null) {
            loadProductDetails(product.url)
            // Fiyat tahminini sıfırla
            _pricePrediction.value = null
            _isLoadingPricePrediction.value = false
        } else {
            _productDetails.value = null
            _pricePrediction.value = null
            _isLoadingPricePrediction.value = false
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
                
                // Satıcıları fiyata göre sırala
                val sortedResponse = detailResponse.copy(
                    product = detailResponse.product.copy(
                        offers = detailResponse.product.offers.sortedBy { it.price }
                    )
                )
                
                // Sıralanmış ürün detaylarını state'e kaydet
                _productDetails.value = sortedResponse

                // Seçili ürünü güncelle ve satıcı sayısını ekle
                _selectedProduct.update { currentProduct ->
                    currentProduct?.copy(
                        offer_count = sortedResponse.product.offers.size
                    )
                }
                
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Error loading product details", e)
                _productDetails.value = null
            } finally {
                _isLoadingProductDetails.value = false
            }
        }
    }
    
    fun predictFuturePrice() {
        val product = _selectedProduct.value ?: return
        val details = _productDetails.value ?: return
        val priceHistory = details.product.price_history
        
        if (priceHistory.isEmpty()) {
            _pricePrediction.value = PricePredictionResult(
                prediction30Days = null,
                prediction60Days = null,
                prediction90Days = null,
                analysis = null,
                errorMessage = "Fiyat tahmini yapılamıyor: Geçmiş fiyat bilgisi bulunamadı."
            )
            return
        }
        
        pricePredictionJob?.cancel()
        pricePredictionJob = viewModelScope.launch {
            try {
                _isLoadingPricePrediction.value = true
                
                // Model için uygun formatta veri oluştur
                val historyEntries = priceHistory.map { 
                    PriceHistoryEntry(it.date, it.price)
                }
                
                // API isteği oluştur
                val request = PricePredictionApiService.buildRequestForPricePrediction(
                    productName = product.name,
                    priceHistory = historyEntries
                )
                
                // API'yi çağır (API anahtarını ekleyelim)
                val response = pricePredictionService.predictPrice(
                    request = request,
                    apiKey = PricePredictionApiService.API_KEY
                )
                
                // Yanıtı işle - PredictionResponse formatında döndüğü için uygun şekilde işleyelim
                val candidate = response.candidates?.firstOrNull()
                if (candidate != null && candidate.content != null) {
                    // İlk part'ın text'ini alalım - Gemini API yanıt metni
                    val responseText = candidate.content.parts?.firstOrNull()?.text ?: ""
                    
                    // Yanıt metnini ayrıştırıp fiyat tahminlerini çıkaralım
                    val result = parseAIResponse(responseText)
                    _pricePrediction.value = result
                } else {
                    _pricePrediction.value = PricePredictionResult(
                        prediction30Days = null,
                        prediction60Days = null,
                        prediction90Days = null,
                        analysis = null,
                        errorMessage = "Fiyat tahmini başarısız oldu: Geçerli bir yanıt alınamadı."
                    )
                }
                
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Error predicting price", e)
                
                val errorMessage = when {
                    e.message?.contains("timeout") == true -> "Sunucu yanıt vermiyor. Lütfen daha sonra tekrar deneyin."
                    e.message?.contains("403") == true -> "Gemini API erişim hatası: API anahtarı geçersiz veya sınırlamalar aşıldı."
                    e.message?.contains("404") == true -> "Gemini API erişim hatası: İstek yapılan endpoint bulunamadı."
                    e.message?.contains("429") == true -> "Gemini API erişim hatası: Çok fazla istek gönderildi, lütfen daha sonra tekrar deneyin."
                    e.message?.contains("timeout") == true || e.message?.contains("timed out") == true -> "Gemini API zaman aşımına uğradı, lütfen daha sonra tekrar deneyin."
                    e.message?.contains("network") == true || e.message?.contains("connection") == true -> "Ağ bağlantısı hatası, internet bağlantınızı kontrol edin."
                    else -> "Fiyat tahmininde hata: ${e.message}"
                }
                
                _pricePrediction.value = PricePredictionResult(
                    prediction30Days = null,
                    prediction60Days = null,
                    prediction90Days = null,
                    analysis = null,
                    errorMessage = errorMessage
                )
            } finally {
                _isLoadingPricePrediction.value = false
            }
        }
    }
    
    // AI yanıtını ayrıştırarak fiyat tahminlerini çıkar
    private fun parseAIResponse(responseText: String): PricePredictionResult {
        try {
            // Regex ile fiyat tahminlerini çıkar
            val pattern30 = Pattern.compile("30 [Gg]ün:?\\s*([0-9]+[.,]?[0-9]*) ?(?:TL|₺)")
            val pattern60 = Pattern.compile("60 [Gg]ün:?\\s*([0-9]+[.,]?[0-9]*) ?(?:TL|₺)")
            val pattern90 = Pattern.compile("90 [Gg]ün:?\\s*([0-9]+[.,]?[0-9]*) ?(?:TL|₺)")
            
            val matcher30 = pattern30.matcher(responseText)
            val matcher60 = pattern60.matcher(responseText)
            val matcher90 = pattern90.matcher(responseText)
            
            val prediction30 = if (matcher30.find()) {
                matcher30.group(1)?.replace(",", ".")?.toDoubleOrNull()
            } else null

            val prediction60 = if (matcher60.find()) {
                matcher60.group(1)?.replace(",", ".")?.toDoubleOrNull()
            } else null

            val prediction90 = if (matcher90.find()) {
                matcher90.group(1)?.replace(",", ".")?.toDoubleOrNull()
            } else null
            
            // Tahmin nedenleri bölümünü al
            val analysisPattern = Pattern.compile("Tahminin nedenleri:?\\s*(.+)", Pattern.DOTALL)
            val analysisMatcher = analysisPattern.matcher(responseText)
            val analysis = if (analysisMatcher.find()) analysisMatcher.group(1)?.trim() else null

            // Debug için yanıtı logla
            Log.d("MarketViewModel", "AI Response: $responseText")
            Log.d("MarketViewModel", "Parsed predictions: 30d=$prediction30, 60d=$prediction60, 90d=$prediction90")
            
            return PricePredictionResult(
                prediction30Days = prediction30,
                prediction60Days = prediction60,
                prediction90Days = prediction90,
                analysis = analysis
            )
        } catch (e: Exception) {
            Log.e("MarketViewModel", "Error parsing AI response", e)
            return PricePredictionResult(
                prediction30Days = null,
                prediction60Days = null,
                prediction90Days = null,
                analysis = null,
                errorMessage = "AI yanıtı işlenirken hata oluştu: ${e.message}"
            )
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

    fun updateSelectedProductWithOffer(offer: Offer) {
        _selectedProduct.update { currentProduct ->
            currentProduct?.copy(
                price = offer.price,
                unit_price = offer.unit_price,
                merchant_id = offer.merchant_id.toString(),
                merchant_logo = offer.merchant_logo
            )
        }
    }
    
    // Önbelleği temizle
    fun clearCache() {
        repository.clearCaches()
    }
}

sealed class MarketUiState {
    object Loading : MarketUiState()
    data class Success(val items: List<MarketItem> = emptyList()) : MarketUiState()
    data class Error(val message: String) : MarketUiState()
}

class MarketViewModelFactory(private val repository: MarketRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MarketViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MarketViewModel(repository) as T
        }
        if (modelClass.isAssignableFrom(ProductSearchViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ProductSearchViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}