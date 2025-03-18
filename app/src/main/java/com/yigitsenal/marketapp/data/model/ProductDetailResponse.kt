package com.yigitsenal.marketapp.data.model

import com.google.gson.annotations.SerializedName

data class ProductDetailResponse(
    val success: Boolean,
    val timestamp: String,
    val product: ProductDetail
)

data class ProductDetail(
    val id: String,
    val name: String,
    val description: String?,
    val specs: List<Any>,
    val price_history: List<PriceHistory>,
    val offers: List<Offer>
)

data class PriceHistory(
    val date: String,
    val price: Double
)

data class Offer(
    val merchant_id: Int,
    val merchant_name: String,
    val merchant_logo: String,
    val price: Double,
    val unit_price: Double
)