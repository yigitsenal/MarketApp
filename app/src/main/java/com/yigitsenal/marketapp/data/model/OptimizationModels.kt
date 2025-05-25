package com.yigitsenal.marketapp.data.model

data class StoreOptimization(
    val stores: List<StoreInfo>,
    val totalCost: Double,
    val totalSavings: Double,
    val itemDistribution: Map<String, StoreInfo>, // ürün adı -> hangi mağaza
    val completionPercentage: Double, // kaç % ürün bulundu
    val foundItems: List<ShoppingListItem>,
    val notFoundItems: List<ShoppingListItem>
)

data class StoreInfo(
    val merchantId: String,
    val merchantName: String,
    val merchantLogo: String,
    val itemCount: Int,
    val totalCost: Double,
    val items: List<OptimizedItem>
)

data class OptimizedItem(
    val originalItem: ShoppingListItem,
    val bestOffer: MarketItem?,
    val potentialSavings: Double = 0.0,
    val isAvailable: Boolean = false
)

data class StoreComparison(
    val currentCost: Double,
    val optimizedCost: Double,
    val savings: Double,
    val savingsPercentage: Double
)
