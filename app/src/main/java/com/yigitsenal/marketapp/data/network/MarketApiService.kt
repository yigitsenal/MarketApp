package com.yigitsenal.marketapp.data.network

import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.model.PaginationInfo
import com.yigitsenal.marketapp.data.model.ProductDetailResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

data class PaginationInfo(
    val current_page: Int,
    val total_pages: Int,
    val has_next: Boolean,
    val has_previous: Boolean,
    val total_items: Int
)

data class SearchResponse(
    val success: Boolean,
    val timestamp: String,
    val pagination: PaginationInfo,
    val total: Int,
    val products: List<MarketItem>
)

interface MarketApiService {
    @GET("api.php")
    suspend fun searchProducts(
        @Query("q") query: String,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int? = null
    ): SearchResponse
    
    @GET("product.php")
    suspend fun getProductDetails(
        @Query("path") productPath: String
    ): ProductDetailResponse
    
    companion object {
        const val BASE_URL = "http://192.168.1.7:8000/"
        const val IMAGE_URL = BASE_URL + "image.php"

        fun getImageUrl(fileName: String, size: String = "md"): String {
            return "${IMAGE_URL}?file=${fileName}&size=${size}"
        }

        fun getProductUrl(path: String): String {
            return "${BASE_URL}product.php?path=$path"
        }
    }
}