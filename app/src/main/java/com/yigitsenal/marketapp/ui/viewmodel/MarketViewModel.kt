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
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            try {
                _uiState.value = MarketUiState.Loading
                val response = repository.searchProducts(
                    query = _newItemText.value,
                    sort = _sortOption.value
                )
                
                if (response.success && response.products?.isNotEmpty() == true) {
                    _uiState.value = MarketUiState.Success(response.products)
                } else {
                    _uiState.value = MarketUiState.Success(emptyList())
                }
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Error searching products", e)
                _uiState.value = MarketUiState.Error("Arama sırasında bir hata oluştu: ${e.message}")
            }
        }
    }

    fun updateNewItemText(text: String) {
        _newItemText.value = text
    }

    // Sıralama seçeneğini güncellemek için fonksiyon
    fun updateSortOption(sort: String?) {
        _sortOption.value = sort
        if (_newItemText.value.isNotEmpty()) {
            searchProducts()
        } else {
            loadItems()
        }
    }

    // Seçilen ürünü güncellemek için metodu ekleyelim
    fun setSelectedProduct(product: MarketItem?) {
        _selectedProduct.value = product
        
        // Fiyat tahminini sıfırla
        _pricePrediction.value = null
        
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
                
                // Satıcıları fiyata göre sırala
                val sortedResponse = detailResponse.copy(
                    product = detailResponse.product.copy(
                        offers = detailResponse.product.offers.sortedBy { it.price }
                    )
                )
                
                // Sıralanmış ürün detaylarını state'e kaydet
                _productDetails.value = sortedResponse
                
                // Seçili ürünü en ucuz fiyatlı satıcının bilgileriyle güncelle
                val cheapestOffer = sortedResponse.product.offers.firstOrNull()
                if (cheapestOffer != null) {
                    _selectedProduct.value = _selectedProduct.value?.copy(
                        price = cheapestOffer.price,
                        unit_price = cheapestOffer.unit_price,
                        merchant_id = cheapestOffer.merchant_id.toString(),
                        merchant_logo = cheapestOffer.merchant_logo
                    )
                }
                
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
                
                // API'yi çağır
                val response = pricePredictionService.predictPrice(
                    request = request,
                    apiKey = PricePredictionApiService.API_KEY // API anahtarını query parametresi olarak gönder
                )
                
                // Yanıtı işle
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (responseText != null) {
                    // Yanıttan tahmin değerlerini çıkar
                    val result = parseAIResponse(responseText)
                    _pricePrediction.value = result
                } else {
                    _pricePrediction.value = PricePredictionResult(
                        prediction30Days = null,
                        prediction60Days = null,
                        prediction90Days = null,
                        analysis = null,
                        errorMessage = "Fiyat tahmini alınamadı: AI yanıtı boş."
                    )
                }
                
            } catch (e: Exception) {
                Log.e("MarketViewModel", "Error predicting price", e)
                val errorMessage = when {
                    e.message?.contains("403") == true -> "Gemini API erişim hatası: API anahtarı geçersiz veya sınırlamalar aşıldı."
                    e.message?.contains("404") == true -> "Gemini API erişim hatası: İstek yapılan endpoit bulunamadı."
                    e.message?.contains("429") == true -> "Gemini API erişim hatası: Çok fazla istek gönderildi, lütfen daha sonra tekrar deneyin."
                    e.message?.contains("timeout") == true || e.message?.contains("timed out") == true -> "Gemini API zaman aşımına uğradı, lütfen daha sonra tekrar deneyin."
                    e.message?.contains("network") == true || e.message?.contains("connection") == true -> "Ağ bağlantısı hatası, internet bağlantınızı kontrol edin."
                    else -> "Fiyat tahmini yapılırken hata oluştu: ${e.message}"
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
    
    private fun parseAIResponse(responseText: String): PricePredictionResult {
        try {
            // Regex ile tahmin değerlerini al
            val pattern30 = Pattern.compile("30 Gün: ([0-9.]+) TL")
            val pattern60 = Pattern.compile("60 Gün: ([0-9.]+) TL")
            val pattern90 = Pattern.compile("90 Gün: ([0-9.]+) TL")
            
            val matcher30 = pattern30.matcher(responseText)
            val matcher60 = pattern60.matcher(responseText)
            val matcher90 = pattern90.matcher(responseText)
            
            val prediction30 = if (matcher30.find()) matcher30.group(1)?.toDoubleOrNull() else null
            val prediction60 = if (matcher60.find()) matcher60.group(1)?.toDoubleOrNull() else null
            val prediction90 = if (matcher90.find()) matcher90.group(1)?.toDoubleOrNull() else null
            
            // Tahmin nedenleri bölümünü al
            val analysisPattern = Pattern.compile("Tahminin nedenleri:(.+)", Pattern.DOTALL)
            val analysisMatcher = analysisPattern.matcher(responseText)
            val analysis = if (analysisMatcher.find()) analysisMatcher.group(1)?.trim() else null
            
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
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}