package com.yigitsenal.marketapp.data.network

import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.model.PaginationInfo
import com.yigitsenal.marketapp.data.model.ProductDetailResponse
import com.yigitsenal.marketapp.util.Constants
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
        const val BASE_URL = Constants.API_BASE_URL + "/"
        const val IMAGE_URL = Constants.ApiEndpoints.IMAGE

        fun getImageUrl(fileName: String, size: String = Constants.ImageSizes.MEDIUM): String {
            return "${IMAGE_URL}?file=${fileName}&size=${size}"
        }

        fun getProductUrl(path: String): String {
            return "${Constants.ApiEndpoints.PRODUCT_DETAILS}?path=$path"
        }
    }
}