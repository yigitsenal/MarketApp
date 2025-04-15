package com.yigitsenal.marketapp.data.repository

import android.util.Log
import com.yigitsenal.marketapp.data.local.MarketItemDao
import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.model.PaginationInfo
import com.yigitsenal.marketapp.data.model.ProductDetailResponse
import com.yigitsenal.marketapp.data.network.MarketApiService
import com.yigitsenal.marketapp.data.network.SearchResponse
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

class MarketRepository(
    private val marketItemDao: MarketItemDao,
    private val apiService: MarketApiService? = null
) {
    val allItems: Flow<List<MarketItem>> = marketItemDao.getAllItems()
    
    // Arama sonuçları için önbellek
    private val searchCache = ConcurrentHashMap<String, SearchResponse>()
    
    // Ürün detayları için önbellek
    private val detailsCache = ConcurrentHashMap<String, ProductDetailResponse>()

    suspend fun insertItem(item: MarketItem) {
        marketItemDao.insertItem(item)
    }

    suspend fun updateItem(item: MarketItem) {
        marketItemDao.updateItem(item)
    }

    suspend fun deleteItem(item: MarketItem) {
        marketItemDao.deleteItem(item)
    }
    
    suspend fun searchProducts(query: String, sort: String? = null, page: Int? = null): SearchResponse {
        val normalizedQuery = query
            .replace('ı', 'i')
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
            
        // Önbellek anahtarı oluştur (sorgu + sıralama + sayfa)
        val cacheKey = "${normalizedQuery}_${sort}_${page}"
        
        // Önbellekte bu arama varsa, hemen döndür
        searchCache[cacheKey]?.let {
            Log.d("MarketRepository", "Cache hit for query: $normalizedQuery")
            return it
        }
        
        Log.d("MarketRepository", "Cache miss for query: $normalizedQuery, fetching from API")
        val encodedQuery = java.net.URLEncoder.encode(normalizedQuery, "UTF-8")
        val response = requireNotNull(apiService) { "API service is not initialized" }.searchProducts(encodedQuery, sort, page)

        // Ürünlerin satıcı sayılarını güncelleyelim
        val updatedProducts = response.products?.map { product ->
            // Eğer bu ürün için önbellekte bir detay bilgisi varsa satıcı sayısını oradan al
            val cachedDetails = product.url.takeIf { it.isNotEmpty() }?.let { url ->
                val path = if (url.contains("?path=")) url.substringAfter("?path=") else url
                val decodedPath = java.net.URLDecoder.decode(path, "UTF-8")
                detailsCache[decodedPath]
            }
            
            if (cachedDetails != null) {
                // Önbellekte varsa satıcı sayısını güncelle
                product.copy(offer_count = cachedDetails.product.offers.size)
            } else {
                // Önbellekte yoksa varsayılan değer
                product
            }
        } ?: emptyList()
        
        // Güncellenmiş ürünlerle yeni bir yanıt oluştur
        val updatedResponse = response.copy(
            products = updatedProducts
        )
        
        // Sonucu önbelleğe al
        searchCache[cacheKey] = updatedResponse
        
        return updatedResponse
    }
    
    suspend fun getProductDetails(productPath: String): ProductDetailResponse {
        Log.d("MarketRepository", "Getting product details for path: $productPath")
        
        val cleanPath = if (productPath.contains("?path=")) {
            productPath.substringAfter("?path=")
        } else {
            productPath
        }
        
        val decodedPath = java.net.URLDecoder.decode(cleanPath, "UTF-8")
        
        // Önbellekte bu ürün detayı varsa, hemen döndür
        detailsCache[decodedPath]?.let {
            Log.d("MarketRepository", "Cache hit for product details: ${decodedPath}")
            return it
        }
        
        Log.d("MarketRepository", "Using decoded path: $decodedPath")
        try {
            val response = requireNotNull(apiService) { "API service is not initialized" }.getProductDetails(decodedPath)
            Log.d("MarketRepository", "Product details loaded successfully: ${response.product.name}")
            
            // Sonucu önbelleğe al
            detailsCache[decodedPath] = response
            
            return response
        } catch (e: Exception) {
            Log.e("MarketRepository", "Error getting product details", e)
            Log.e("MarketRepository", "Error message: ${e.message}")
            throw e
        }
    }
    
    // Önbellekleri temizle
    fun clearCaches() {
        searchCache.clear()
        detailsCache.clear()
    }
}