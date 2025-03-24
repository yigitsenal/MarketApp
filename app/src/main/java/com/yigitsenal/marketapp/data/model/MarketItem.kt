package com.yigitsenal.marketapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "market_items")
data class MarketItem(
    @PrimaryKey var id: String = "",
    var name: String = "",
    var brand: String = "",
    var price: Double = 0.0,
    var unit_price: Double = 0.0,
    var quantity: Int = 0,
    var unit: String = "",
    var merchant_id: String = "",
    var merchant_logo: String = "",
    var image: String = "",
    var url: String = "",
    var offer_count: Int = 0
)