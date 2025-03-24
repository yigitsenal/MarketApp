package com.yigitsenal.marketapp.data.repository

import android.util.Log
import com.yigitsenal.marketapp.data.local.MarketItemDao
import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.model.PaginationInfo
import com.yigitsenal.marketapp.data.model.ProductDetailResponse
import com.yigitsenal.marketapp.data.network.MarketApiService
import com.yigitsenal.marketapp.data.network.SearchResponse
import kotlinx.coroutines.flow.Flow

class MarketRepository(
    private val marketItemDao: MarketItemDao,
    private val apiService: MarketApiService? = null
) {
    val allItems: Flow<List<MarketItem>> = marketItemDao.getAllItems()

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
            
        val encodedQuery = java.net.URLEncoder.encode(normalizedQuery, "UTF-8")
        val response = requireNotNull(apiService) { "API service is not initialized" }.searchProducts(encodedQuery, sort, page)

        // Her ürün için detayları yükle ve satıcı sayısını güncelle
        val updatedProducts = response.products?.map { product ->
            try {
                if (product.url.isNotEmpty()) {
                    val details = getProductDetails(product.url)
                    product.copy(offer_count = details.product.offers.size)
                } else {
                    product.copy(offer_count = 0)
                }
            } catch (e: Exception) {
                Log.e("MarketRepository", "Error loading product details for ${product.name}", e)
                product.copy(offer_count = 0)
            }
        } ?: emptyList()

        return response.copy(products = updatedProducts)
    }
    
    suspend fun getProductDetails(productPath: String): ProductDetailResponse {
        Log.d("MarketRepository", "Getting product details for path: $productPath")
        try {
            val cleanPath = if (productPath.contains("?path=")) {
                productPath.substringAfter("?path=")
            } else {
                productPath
            }
            
            val decodedPath = java.net.URLDecoder.decode(cleanPath, "UTF-8")
            
            Log.d("MarketRepository", "Using decoded path: $decodedPath")
            
            val response = requireNotNull(apiService) { "API service is not initialized" }.getProductDetails(decodedPath)
            Log.d("MarketRepository", "Product details loaded successfully: ${response.product.name}")
            return response
        } catch (e: Exception) {
            Log.e("MarketRepository", "Error getting product details", e)
            Log.e("MarketRepository", "Error message: ${e.message}")
            throw e
        }
    }
}