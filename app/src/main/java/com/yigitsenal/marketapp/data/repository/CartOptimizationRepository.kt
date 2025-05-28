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
                        println("DEBUG - Shopping item details: quantity=${item.quantity}, unit=${item.unit}, price=${item.price}")
                        
                        // İlk birkaç teklifi detaylı logla
                        offers.take(3).forEach { offer ->
                            println("DEBUG - Raw offer: name='${offer.name}', price=${offer.price}, unit_price=${offer.unit_price}, quantity=${offer.quantity}, unit='${offer.unit}', merchant=${offer.merchant_id}")
                        }
                        
                        // En iyi teklifi bul - akıllı ürün eşleştirme ile
                        val bestOffer = findBestMatchingOffer(item, offers)
                        
                        val currentTotalPrice = item.price // Bu zaten toplam fiyat (quantity dahil)
                        val potentialSavings = if (bestOffer != null) {
                            //  API'deki price ile sepetteki price'ı direkt karşılaştır

                            val bestOfferPrice = bestOffer.price
                            
                            // Debug için log ekleyelim
                            println("DEBUG - Best offer found: ${bestOffer.name} at ${bestOffer.merchant_id}")
                            println("DEBUG - Original item: name='${item.name}', quantity=${item.quantity}, unit='${item.unit}', total_price=$currentTotalPrice")
                            println("DEBUG - Best offer price: $bestOfferPrice")
                            println("DEBUG - Direct comparison - Current: $currentTotalPrice vs Best: $bestOfferPrice")
                            
                            // Sadece mantıklı tasarruf varsa göster
                            val savings = currentTotalPrice - bestOfferPrice
                            println("DEBUG - Potential savings: $savings")
                            if (savings > 0 && bestOfferPrice > 0) savings else 0.0
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
                    // Use package price directly instead of unit calculations
                    val packagePrice = item.bestOffer!!.price
                    Triple(
                        item,
                        item.bestOffer.merchant_id,
                        packagePrice
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

                val totalCost = allItems.sumOf { item ->
                    if (item.bestOffer != null) {

                        item.bestOffer.price
                    } else {
                        0.0
                    }
                }
                
                println("DEBUG - Building final store: $merchantId, Items: ${allItems.size}, Total Cost: $totalCost")
                allItems.forEach { item ->
                    val packagePrice = item.bestOffer?.price ?: 0.0
                    println("DEBUG - Store item: ${item.originalItem.name}, Package Price: $packagePrice")
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
        // Local API endpoint'ini kullan - merchant ID'yi direkt logo.php'ye gönder
        val logoUrl = "http://10.95.3.127/logo.php?id=$merchantId"
        println("DEBUG - Logo URL for merchant '$merchantId': '$logoUrl'")
        return logoUrl
    }
    
    private fun extractMerchantIdFromName(merchantName: String): String? {
        // "Market #16743" formatındaki isimlerden ID'yi çıkar
        val regex = Regex("Market #(\\d+)")
        return regex.find(merchantName)?.groupValues?.get(1)
    }
    
    private fun findBestMatchingOffer(shoppingItem: ShoppingListItem, offers: List<MarketItem>): MarketItem? {
        if (offers.isEmpty()) return null
        
        println("DEBUG - Finding best match for: '${shoppingItem.name}' (${shoppingItem.quantity} ${shoppingItem.unit})")
        
        // Önce valid fiyatlı teklifleri filtrele
        val validOffers = offers.filter { it.price > 0 }
        if (validOffers.isEmpty()) return null
        
        // Akıllı eşleştirme: relevance score hesapla
        val scoredOffers = validOffers.map { offer ->
            val relevanceScore = calculateRelevanceScore(shoppingItem, offer)
            println("DEBUG - Offer: '${offer.name}' - Price: ${offer.price}, Relevance: $relevanceScore")
            Pair(offer, relevanceScore)
        }.filter { it.second > 0.0 } // Sadece relevant olanları al
        
        if (scoredOffers.isEmpty()) {
            println("DEBUG - No relevant offers found, taking cheapest")
            return validOffers.minByOrNull { it.price }
        }
        
        // En yüksek relevance score'a sahip teklifleri al
        val maxRelevance = scoredOffers.maxOf { it.second }
        val bestMatches = scoredOffers.filter { it.second == maxRelevance }
        
        // Aynı relevance'a sahip olanlar arasından en ucuzunu seç
        val bestOffer = bestMatches.minByOrNull { it.first.price }?.first
        
        println("DEBUG - Selected best match: '${bestOffer?.name}' - Price: ${bestOffer?.price}, Relevance: $maxRelevance")
        return bestOffer
    }
    
    private fun calculateRelevanceScore(shoppingItem: ShoppingListItem, offer: MarketItem): Double {
        var score = 0.0
        
        val shoppingName = shoppingItem.name.lowercase().trim()
        val offerName = offer.name.lowercase().trim()
        

        if (shoppingName == offerName) {
            score += 100.0
            return score
        }
        

        val shoppingBrand = extractBrand(shoppingName)
        val offerBrand = extractBrand(offerName)
        if (shoppingBrand.isNotEmpty() && offerBrand.isNotEmpty() && shoppingBrand == offerBrand) {
            score += 50.0
        }
        

        val shoppingKeywords = extractProductKeywords(shoppingName)
        val offerKeywords = extractProductKeywords(offerName)
        val keywordMatches = shoppingKeywords.intersect(offerKeywords).size
        val keywordScore = (keywordMatches.toDouble() / maxOf(shoppingKeywords.size, 1)) * 30.0
        score += keywordScore
        

        val sizeScore = calculateSizeCompatibility(shoppingItem, offer)
        score += sizeScore
        

        if (detectProductMismatch(shoppingName, offerName)) {
            score -= 50.0
        }
        
        return maxOf(0.0, score)
    }
    
    private fun extractBrand(productName: String): String {
        val brands = listOf("lipton", "arifoğlu", "ülker", "eti", "nestle", "unilever", "p&g", "coca-cola", "pepsi")
        return brands.find { productName.contains(it) } ?: ""
    }
    
    private fun extractProductKeywords(productName: String): Set<String> {

        val cleanName = productName
            .replace(Regex("\\d+\\s*(gr|kg|ml|lt|adet|li|lü|lu)"), "")
            .replace(Regex("(lipton|arifoğlu|ülker|eti)"), "")
            .replace(Regex("\\+.*"), "")
            .split("\\s+".toRegex())
            .map { it.trim() }
            .filter { it.length > 2 && !it.matches(Regex("\\d+")) }
        
        return cleanName.toSet()
    }
    
    private fun calculateSizeCompatibility(shoppingItem: ShoppingListItem, offer: MarketItem): Double {

        val shoppingSize = extractSize(shoppingItem.name)
        val offerSize = extractSize(offer.name)
        

        if (shoppingSize != null && offerSize != null) {
            val (shoppingValue, shoppingUnit) = shoppingSize
            val (offerValue, offerUnit) = offerSize
            

            if (shoppingUnit == offerUnit) {
                val ratio = offerValue / shoppingValue
                return when {
                    ratio in 0.8..1.2 -> 20.0
                    ratio in 0.5..2.0 -> 10.0
                    else -> -10.0
                }
            }
            

            if ((shoppingUnit == "gr" && offerUnit == "kg") || (shoppingUnit == "kg" && offerUnit == "gr")) {
                val normalizedShopping = if (shoppingUnit == "gr") shoppingValue / 1000 else shoppingValue
                val normalizedOffer = if (offerUnit == "gr") offerValue / 1000 else offerValue
                val ratio = normalizedOffer / normalizedShopping
                return when {
                    ratio in 0.8..1.2 -> 15.0
                    ratio in 0.5..2.0 -> 5.0
                    else -> -5.0
                }
            }
        }
        
        return 0.0
    }
    
    private fun extractSize(productName: String): Pair<Double, String>? {
        val sizeRegex = Regex("(\\d+(?:[.,]\\d+)?)\\s*(gr|kg|ml|lt|adet)")
        val match = sizeRegex.find(productName.lowercase())
        return if (match != null) {
            val value = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
            val unit = match.groupValues[2]
            Pair(value, unit)
        } else null
    }
    
    private fun detectProductMismatch(shoppingName: String, offerName: String): Boolean {

        val bundleKeywords = listOf("\\+", "combo", "set", "paket", "bundle", "tekli", "tek dem")
        val shoppingHasBundle = bundleKeywords.any { shoppingName.contains(it.toRegex(RegexOption.IGNORE_CASE)) }
        val offerHasBundle = bundleKeywords.any { offerName.contains(it.toRegex(RegexOption.IGNORE_CASE)) }
        

        return shoppingHasBundle != offerHasBundle
    }
}
