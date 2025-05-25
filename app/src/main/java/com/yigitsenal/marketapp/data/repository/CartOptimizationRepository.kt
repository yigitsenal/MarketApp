package com.yigitsenal.marketapp.data.repository

import com.yigitsenal.marketapp.data.local.ShoppingListItemDao
import com.yigitsenal.marketapp.data.model.*
import com.yigitsenal.marketapp.data.network.MarketApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CartOptimizationRepository(
    private val apiService: MarketApiService,
    private val shoppingListItemDao: ShoppingListItemDao
) {
    
    suspend fun optimizeShoppingCart(listId: Int): Flow<StoreOptimization> = flow {
        try {
            // Alışveriş listesindeki ürünleri al
            val shoppingItems = shoppingListItemDao.getItemsForList(listId)
            
            shoppingItems.collect { items ->
                if (items.isEmpty()) {
                    emit(createEmptyOptimization())
                    return@collect
                }
                
                // Her ürün için mevcut teklifleri bul
                val optimizedItems = mutableListOf<OptimizedItem>()
                val allOffers = mutableMapOf<String, List<MarketItem>>()
                
                // Her ürün için API'den teklif al
                items.forEach { item ->
                    try {
                        val response = apiService.searchProducts(item.name)
                        val offers = response.products
                        allOffers[item.name] = offers
                        
                        // API response'u debug edelim
                        println("DEBUG - API Response for '${item.name}':")
                        println("DEBUG - Number of offers: ${offers.size}")
                        
                        // En iyi teklifi bul (en düşük birim fiyat)
                        val bestOffer = offers.minByOrNull { offer ->
                            // Fiyat hesaplama önceliği: unit_price > price bölü miktar > price
                            val effectiveUnitPrice = when {
                                offer.unit_price > 0 -> offer.unit_price
                                offer.price > 0 -> offer.price // price zaten birim fiyat olabilir
                                else -> Double.MAX_VALUE
                            }
                            println("DEBUG - Offer: ${offer.name} from ${offer.merchant_id}, unit_price: ${offer.unit_price}, price: ${offer.price}, effective: $effectiveUnitPrice")
                            effectiveUnitPrice
                        }
                        
                        val currentTotalPrice = item.price // Bu zaten toplam fiyat (quantity dahil)
                        val potentialSavings = if (bestOffer != null) {
                            // En iyi fiyatı belirle - unit_price öncelikli
                            val bestUnitPrice = when {
                                bestOffer.unit_price > 0 -> bestOffer.unit_price
                                bestOffer.price > 0 -> bestOffer.price
                                else -> 0.0
                            }
                            
                            val bestOfferTotalPrice = bestUnitPrice * item.quantity
                            
                            // Debug için log ekleyelim
                            println("DEBUG - Best offer found: ${bestOffer.name} at ${bestOffer.merchant_id}")
                            println("DEBUG - Original item: ${item.name}, quantity: ${item.quantity}, current total: $currentTotalPrice")
                            println("DEBUG - Best unit price: $bestUnitPrice, calculated total: $bestOfferTotalPrice")
                            println("DEBUG - Potential savings: ${maxOf(0.0, currentTotalPrice - bestOfferTotalPrice)}")
                            
                            maxOf(0.0, currentTotalPrice - bestOfferTotalPrice)
                        } else 0.0
                        
                        optimizedItems.add(
                            OptimizedItem(
                                originalItem = item,
                                bestOffer = bestOffer,
                                potentialSavings = potentialSavings,
                                isAvailable = bestOffer != null
                            )
                        )
                    } catch (e: Exception) {
                        // API hatası durumunda mevcut ürünü koru
                        optimizedItems.add(
                            OptimizedItem(
                                originalItem = item,
                                bestOffer = null,
                                potentialSavings = 0.0,
                                isAvailable = false
                            )
                        )
                    }
                }
                
                // Mağaza kombinasyonlarını hesapla
                val optimization = calculateOptimalStoreDistribution(optimizedItems)
                emit(optimization)
            }
        } catch (e: Exception) {
            emit(createEmptyOptimization())
        }
    }
    
    private fun calculateOptimalStoreDistribution(optimizedItems: List<OptimizedItem>): StoreOptimization {
        var totalCurrentCost = 0.0
        val foundItems = mutableListOf<ShoppingListItem>()
        val notFoundItems = mutableListOf<ShoppingListItem>()
        
        // Önce hangi ürünlerin bulunduğunu tespit et
        val availableItems = mutableListOf<OptimizedItem>()
        optimizedItems.forEach { optimizedItem ->
            totalCurrentCost += optimizedItem.originalItem.price // Bu zaten quantity dahil toplam fiyat
            
            if (optimizedItem.isAvailable && optimizedItem.bestOffer != null) {
                availableItems.add(optimizedItem)
                foundItems.add(optimizedItem.originalItem)
            } else {
                notFoundItems.add(optimizedItem.originalItem)
            }
        }
        
        // Akıllı mağaza kombinasyonu hesapla
        val optimalCombination = findOptimalStoreCombination(availableItems)
        
        val totalOptimizedCost = optimalCombination.stores.sumOf { it.totalCost } + 
                                notFoundItems.sumOf { it.price }
        
        val totalSavings = totalCurrentCost - totalOptimizedCost
        val completionPercentage = if (optimizedItems.isNotEmpty()) {
            (foundItems.size.toDouble() / optimizedItems.size) * 100
        } else 0.0
        
        return StoreOptimization(
            stores = optimalCombination.stores,
            totalCost = totalOptimizedCost,
            totalSavings = maxOf(0.0, totalSavings),
            itemDistribution = optimalCombination.itemDistribution,
            completionPercentage = completionPercentage,
            foundItems = foundItems,
            notFoundItems = notFoundItems
        )
    }
    
    private data class StoreCombination(
        val stores: List<StoreInfo>,
        val itemDistribution: Map<String, StoreInfo>,
        val totalCost: Double,
        val storeCount: Int
    )
    
    private fun findOptimalStoreCombination(availableItems: List<OptimizedItem>): StoreCombination {
        if (availableItems.isEmpty()) {
            return StoreCombination(emptyList(), emptyMap(), 0.0, 0)
        }
        
        // Her ürün için mevcut tüm mağaza seçeneklerini al
        val itemOptions = availableItems.groupBy { it.originalItem.name }
            .mapValues { (_, items) -> 
                items.map { item ->
                    Triple(
                        item,
                        item.bestOffer!!.merchant_id,
                        item.bestOffer.unit_price * item.originalItem.quantity
                    )
                }.distinctBy { it.second } // Aynı mağazadan gelen teklifler varsa en iyisini al
                    .sortedBy { it.third } // Fiyata göre sırala
            }
        
        // Dinamik programlama ile en optimal kombinasyonu bul
        return findBestCombinationDP(itemOptions)
    }
    
    private fun findBestCombinationDP(itemOptions: Map<String, List<Triple<OptimizedItem, String, Double>>>): StoreCombination {
        val itemNames = itemOptions.keys.toList()
        val numItems = itemNames.size
        
        // Her durum için: hangi ürünlerin dahil olduğu ve hangi mağazaların kullanıldığı
        data class State(
            val itemIndex: Int,
            val selectedStores: Set<String>
        )
        
        // Memoization için cache
        val memo = mutableMapOf<State, StoreCombination>()
        
        fun solve(itemIndex: Int, usedStores: Set<String>): StoreCombination {
            if (itemIndex >= numItems) {
                return StoreCombination(emptyList(), emptyMap(), 0.0, usedStores.size)
            }
            
            val state = State(itemIndex, usedStores)
            if (memo.containsKey(state)) {
                return memo[state]!!
            }
            
            val currentItem = itemNames[itemIndex]
            val options = itemOptions[currentItem] ?: emptyList()
            
            var bestCombination = StoreCombination(emptyList(), emptyMap(), Double.MAX_VALUE, Int.MAX_VALUE)
            
            // Bu ürün için her mağaza seçeneğini dene
            for ((optimizedItem, merchantId, cost) in options) {
                val newUsedStores = usedStores + merchantId
                val nextCombination = solve(itemIndex + 1, newUsedStores)
                
                if (nextCombination.totalCost != Double.MAX_VALUE) {
                    val totalCost = cost + nextCombination.totalCost
                    val storeCount = newUsedStores.size
                    
                    // Daha iyi kombinasyon mu kontrol et
                    // Öncelik: daha az mağaza, sonra daha az maliyet
                    val isBetter = when {
                        storeCount < bestCombination.storeCount -> true
                        storeCount == bestCombination.storeCount && totalCost < bestCombination.totalCost -> true
                        else -> false
                    }
                    
                    if (isBetter) {
                        val newItemDistribution = nextCombination.itemDistribution.toMutableMap()
                        
                        // Bu ürün için mağaza bilgisini ekle (geçici)
                        val tempStoreInfo = StoreInfo(
                            merchantId = merchantId,
                            merchantName = getMerchantDisplayName(merchantId),
                            merchantLogo = getMerchantLogoUrl(merchantId),
                            itemCount = 1,
                            totalCost = cost,
                            items = listOf(optimizedItem)
                        )
                        newItemDistribution[currentItem] = tempStoreInfo
                        
                        bestCombination = StoreCombination(
                            stores = emptyList(), // Stores'u daha sonra hesaplayacağız
                            itemDistribution = newItemDistribution,
                            totalCost = totalCost,
                            storeCount = storeCount
                        )
                    }
                }
            }
            
            memo[state] = bestCombination
            return bestCombination
        }
        
        val result = solve(0, emptySet())
        
        // Final stores listesini oluştur
        val finalStores = buildFinalStoresList(result.itemDistribution)
        
        return result.copy(stores = finalStores)
    }
    
    private fun buildFinalStoresList(itemDistribution: Map<String, StoreInfo>): List<StoreInfo> {
        return itemDistribution.values
            .groupBy { it.merchantId }
            .map { (merchantId, storeInfos) ->
                val allItems = storeInfos.flatMap { it.items }
                // Recalculate total cost from actual items instead of summing temp totalCost values
                val totalCost = allItems.sumOf { item ->
                    if (item.bestOffer != null) {
                        val unitPrice = when {
                            item.bestOffer.unit_price > 0 -> item.bestOffer.unit_price
                            item.bestOffer.price > 0 -> item.bestOffer.price
                            else -> 0.0
                        }
                        unitPrice * item.originalItem.quantity
                    } else {
                        0.0
                    }
                }
                val firstStore = storeInfos.first()
                
                println("DEBUG - Building final store: $merchantId, Items: ${allItems.size}, Total Cost: $totalCost")
                allItems.forEach { item ->
                    val unitPrice = item.bestOffer?.unit_price ?: 0.0
                    val itemTotal = unitPrice * item.originalItem.quantity
                    println("DEBUG - Item: ${item.originalItem.name}, Unit Price: $unitPrice, Quantity: ${item.originalItem.quantity}, Item Total: $itemTotal")
                }
                
                StoreInfo(
                    merchantId = merchantId,
                    merchantName = getMerchantDisplayName(merchantId),
                    merchantLogo = getMerchantLogoUrl(merchantId),
                    itemCount = allItems.size,
                    totalCost = totalCost,
                    items = allItems
                )
            }
            .sortedWith(compareByDescending<StoreInfo> { it.itemCount }.thenBy { it.totalCost })
    }
    
    private fun createEmptyOptimization() = StoreOptimization(
        stores = emptyList(),
        totalCost = 0.0,
        totalSavings = 0.0,
        itemDistribution = emptyMap(),
        completionPercentage = 0.0,
        foundItems = emptyList(),
        notFoundItems = emptyList()
    )
    
    private fun getMerchantDisplayName(merchantId: String): String {
        return when (merchantId.lowercase()) {
            "migros" -> "Migros"
            "carrefour" -> "CarrefourSA"
            "carrefoursa" -> "CarrefourSA"
            "pazarama" -> "Pazarama"
            "bim" -> "BİM"
            "a101" -> "A101"
            "sok" -> "ŞOK"
            "tesco" -> "Tesco Kipa"
            "metro" -> "Metro"
            "teknosa" -> "Teknosa"
            "mediamarkt" -> "Media Markt"
            "vatan" -> "Vatan Bilgisayar"
            "hepsiburada" -> "Hepsiburada"
            "trendyol" -> "Trendyol"
            "amazon" -> "Amazon"
            "getir" -> "Getir"
            "banabi" -> "Banabi"
            else -> {
                // Eğer sayısal ID geliyorsa, friendly name'e çevirelim
                try {
                    val id = merchantId.toInt()
                    when (id) {
                        10370 -> "Pazarama"
                        10371 -> "CarrefourSA"
                        10372 -> "Migros"
                        10373 -> "BİM"
                        10374 -> "A101"
                        10375 -> "ŞOK"
                        else -> "Market #$id"
                    }
                } catch (e: NumberFormatException) {
                    merchantId.replaceFirstChar { it.uppercase() }
                }
            }
        }
    }
    
    private fun getMerchantLogoUrl(merchantId: String): String {
        return when (merchantId.lowercase()) {
            "migros" -> "https://logo.clearbit.com/migros.com.tr"
            "carrefour", "carrefoursa" -> "https://logo.clearbit.com/carrefour.com"
            "pazarama" -> "https://logo.clearbit.com/pazarama.com"
            "bim" -> "https://logo.clearbit.com/bim.com.tr"
            "a101" -> "https://logo.clearbit.com/a101.com.tr"
            "sok" -> "https://logo.clearbit.com/sokmarket.com.tr"
            "tesco" -> "https://logo.clearbit.com/tesco.com"
            "metro" -> "https://logo.clearbit.com/metro.com.tr"
            "teknosa" -> "https://logo.clearbit.com/teknosa.com"
            "mediamarkt" -> "https://logo.clearbit.com/mediamarkt.com.tr"
            "vatan" -> "https://logo.clearbit.com/vatanbilgisayar.com"
            "hepsiburada" -> "https://logo.clearbit.com/hepsiburada.com"
            "trendyol" -> "https://logo.clearbit.com/trendyol.com"
            "amazon" -> "https://logo.clearbit.com/amazon.com.tr"
            "getir" -> "https://logo.clearbit.com/getir.com"
            "banabi" -> "https://logo.clearbit.com/banabi.com"
            else -> {
                // Sayısal ID'ler için de logo URL'leri
                try {
                    val id = merchantId.toInt()
                    when (id) {
                        10370 -> "https://logo.clearbit.com/pazarama.com"
                        10371 -> "https://logo.clearbit.com/carrefour.com"
                        10372 -> "https://logo.clearbit.com/migros.com.tr"
                        10373 -> "https://logo.clearbit.com/bim.com.tr"
                        10374 -> "https://logo.clearbit.com/a101.com.tr"
                        10375 -> "https://logo.clearbit.com/sokmarket.com.tr"
                        else -> ""
                    }
                } catch (e: NumberFormatException) {
                    ""
                }
            }
        }
    }
}
