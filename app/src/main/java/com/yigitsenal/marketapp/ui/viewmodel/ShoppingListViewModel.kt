package com.yigitsenal.marketapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.model.ShoppingList
import com.yigitsenal.marketapp.data.model.ShoppingListItem
import com.yigitsenal.marketapp.data.repository.ShoppingListRepository
import com.yigitsenal.marketapp.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ShoppingListViewModel @Inject constructor(
    private val repository: ShoppingListRepository
) : ViewModel() {
    
    // Aktif kullanıcı ID'si
    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId
    
    private val _activeShoppingList = MutableStateFlow<ShoppingList?>(null)
    val activeShoppingList: StateFlow<ShoppingList?> = _activeShoppingList
    
    // Optimizasyon tetikleme için callback
    private var onListChangedCallback: ((Int) -> Unit)? = null
    
    // Tüm alışveriş listeleri - userId'ye göre filtreleme
    val allShoppingLists = _userId.flatMapLatest { userId ->
        if (userId != null) {
            repository.getAllShoppingLists(userId)
        } else {
            flowOf(emptyList())
        }    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    // Aktif listedeki öğeler - flatMapLatest kullanarak aktif liste değiştiğinde otomatik güncelleme
    val activeListItems: StateFlow<List<ShoppingListItem>> = _activeShoppingList
        .flatMapLatest { list ->
            if (list != null) {
                repository.getItemsForList(list.id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    // Yeni öğe ekleme için state'ler
    private val _newItemName = MutableStateFlow("")
    val newItemName: StateFlow<String> = _newItemName
    
    private val _newItemQuantity = MutableStateFlow(1.0)
    val newItemQuantity: StateFlow<Double> = _newItemQuantity
    
    private val _newItemUnit = MutableStateFlow("adet")
    val newItemUnit: StateFlow<String> = _newItemUnit
    
    private val _newItemUnitPrice = MutableStateFlow(0.0)
    val newItemUnitPrice: StateFlow<Double> = _newItemUnitPrice
    
    private val _selectedItems = MutableStateFlow<Set<ShoppingListItem>>(emptySet())
    val selectedItems: StateFlow<Set<ShoppingListItem>> = _selectedItems
    
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode
    init {
        viewModelScope.launch {
            // UserId değiştiğinde listeleri kontrol et
            _userId.collect { userId ->
                if (userId != null) {
                    // UserId set edildikten sonra listeleri kontrol et
                    allShoppingLists.first().let { lists ->
                        if (lists.isEmpty()) {
                            createNewShoppingList("Alışveriş Listem")
                        } else {
                            // Mevcut aktif liste farklı kullanıcıya aitse null yap
                            val currentActiveList = _activeShoppingList.value
                            if (currentActiveList == null || currentActiveList.userId != userId) {
                                _activeShoppingList.value = lists.first()
                            }
                        }
                    }
                } else {
                    // UserId null ise aktif listeyi de temizle
                    _activeShoppingList.value = null
                }
            }
        }
    }
    
    // Kullanıcı ID'sini ayarla
    fun setUserId(userId: String) {
        _userId.value = userId
    }
      fun createNewShoppingList(name: String) {
        viewModelScope.launch {
            val userId = _userId.value
            if (userId != null) {
                val newList = ShoppingList(
                    name = name,
                    date = Date().time,
                    isCompleted = false,
                    userId = userId
                )
                val id = repository.insertShoppingList(newList)
                _activeShoppingList.value = newList.copy(id = id.toInt())
            }
        }
    }
    
    fun setActiveShoppingList(list: ShoppingList) {
        _activeShoppingList.value = list
    }
    
    // Yeni öğe ekle
    fun addItem(name: String, quantity: Double, unit: String, unitPrice: Double) {
        viewModelScope.launch {
            val activeList = _activeShoppingList.value
            if (activeList != null) {
                val newItem = ShoppingListItem(
                    id = 0, // Room otomatik olarak ID atayacak
                    listId = activeList.id,
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    price = quantity * unitPrice, // Toplam fiyat = miktar * birim fiyat
                    unitPrice = unitPrice,
                    isCompleted = false,
                    date = Date().time
                )
                repository.insertItem(newItem)
                notifyListChanged()
            }
        }
    }    // Market ürününden yeni öğe ekle
    fun addItemFromMarket(marketItem: MarketItem) {
        viewModelScope.launch {
            // Aktif alışveriş listesi yoksa yeni bir tane oluştur
            var activeList = _activeShoppingList.value
            if (activeList == null) {
                val userId = _userId.value
                if (userId != null) {
                    val newListJob = async {
                        createNewShoppingList("Alışveriş Listem")
                    }
                    newListJob.await()
                    // Yeni oluşturulan listeyi al
                    activeList = _activeShoppingList.value
                } else {
                    Log.e("ShoppingListViewModel", "UserId is null, cannot create shopping list")
                    return@launch
                }
            }
            
            // Hala null ise hata
            if (activeList == null) {
                Log.e("ShoppingListViewModel", "Could not create or get active shopping list")
                return@launch
            }

            // Görsel URL'lerini düzenle
            val imageUrl = when {
                marketItem.image.startsWith("/") -> "${Constants.API_BASE_URL}${marketItem.image}"
                marketItem.image.contains("file=") -> {
                    val filename = marketItem.image.substringAfter("file=").substringBefore("&")
                    "${Constants.ApiEndpoints.IMAGE}?file=${filename}&size=${Constants.ImageSizes.MEDIUM}"
                }
                else -> "${Constants.ApiEndpoints.IMAGE}?file=${marketItem.image}&size=${Constants.ImageSizes.MEDIUM}"
            }

            val merchantLogo = when {
                marketItem.merchant_logo.startsWith("/") -> "${Constants.API_BASE_URL}${marketItem.merchant_logo}"
                marketItem.merchant_logo.contains("file=") -> {
                    val filename = marketItem.merchant_logo.substringAfter("file=").substringBefore("&")
                    "${Constants.ApiEndpoints.IMAGE}?file=${filename}&size=${Constants.ImageSizes.SMALL}"
                }
                else -> "${Constants.ApiEndpoints.IMAGE}?file=${marketItem.merchant_logo}&size=${Constants.ImageSizes.SMALL}"
            }

            // Aynı ürünün aynı mağazadan olup olmadığını kontrol et
            val existingItem = activeListItems.value.find { item ->
                item.name == marketItem.name && 
                item.merchantId == marketItem.merchant_id
            }

            if (existingItem != null) {
                // Aynı mağazadan aynı ürün varsa miktarını ve fiyatını güncelle
                val newQuantity = existingItem.quantity + 1.0
                val updatedItem = existingItem.copy(
                    quantity = newQuantity,
                    price = marketItem.price * newQuantity // Toplam fiyat = price * miktar
                )
                repository.updateItem(updatedItem)
                notifyListChanged()
            } else {
                // Farklı mağazadan veya yeni ürünse yeni satır olarak ekle
                val newItem = ShoppingListItem(
                    listId = activeList.id,
                    name = marketItem.name,
                    quantity = 1.0,
                    unit = "adet",
                    price = marketItem.price * 1.0, // Toplam fiyat = price * miktar
                    unitPrice = marketItem.price,
                    merchantId = marketItem.merchant_id,
                    merchantLogo = merchantLogo,
                    imageUrl = imageUrl,
                    isCompleted = false
                )
                repository.insertItem(newItem)
                notifyListChanged()
            }
        }
    }
    
    fun increaseItemQuantity(marketItem: MarketItem) {
        viewModelScope.launch {
            val existingItem = activeListItems.value.find { item ->
                item.name == marketItem.name && 
                item.merchantId == marketItem.merchant_id
            }

            if (existingItem != null) {
                val newQuantity = existingItem.quantity + 1.0
                val updatedItem = existingItem.copy(
                    quantity = newQuantity,
                    price = marketItem.price * newQuantity // Toplam fiyat = price * miktar
                )
                repository.updateItem(updatedItem)
                notifyListChanged()
            }
        }
    }
    
    fun decreaseItemQuantity(marketItem: MarketItem) {
        viewModelScope.launch {
            val existingItem = activeListItems.value.find { item ->
                item.name == marketItem.name && 
                item.merchantId == marketItem.merchant_id
            }

            if (existingItem != null) {
                if (existingItem.quantity > 1) {
                    val newQuantity = existingItem.quantity - 1.0
                    val updatedItem = existingItem.copy(
                        quantity = newQuantity,
                        price = marketItem.price * newQuantity // Toplam fiyat = price * miktar
                    )
                    repository.updateItem(updatedItem)
                } else {
                    repository.deleteItem(existingItem)
                }
                notifyListChanged() // Bu satır eksikti!
            }
        }
    }
    
    fun toggleItemCompletion(item: ShoppingListItem) {
        viewModelScope.launch {
            repository.updateItemCompletionStatus(item.id, !item.isCompleted)
        }
    }
    
    fun deleteItem(item: ShoppingListItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
            notifyListChanged()
        }
    }
    
    fun updateItemDetails(item: ShoppingListItem, name: String, quantity: Double, unit: String, unitPrice: Double) {
        viewModelScope.launch {
            repository.updateItem(
                item.copy(
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    unitPrice = unitPrice,
                    price = quantity * unitPrice // Toplam fiyat = miktar * birim fiyat
                )
            )
            notifyListChanged()
        }
    }
    
    fun updateNewItemName(name: String) {
        _newItemName.value = name
    }
    
    fun updateNewItemQuantity(quantity: Double) {
        _newItemQuantity.value = quantity
    }
    
    fun updateNewItemUnit(unit: String) {
        _newItemUnit.value = unit
    }
    
    fun updateNewItemUnitPrice(price: Double) {
        _newItemUnitPrice.value = price
    }

    fun toggleItemSelection(item: ShoppingListItem) {
        _selectedItems.value = _selectedItems.value.toMutableSet().apply {
            if (contains(item)) remove(item) else add(item)
        }
        _isSelectionMode.value = _selectedItems.value.isNotEmpty()
    }

    fun deleteSelectedItems() {
        viewModelScope.launch {
            _selectedItems.value.forEach { item ->
                repository.deleteItem(item)
            }
            _selectedItems.value = emptySet()
            _isSelectionMode.value = false
            notifyListChanged()
        }
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
        _isSelectionMode.value = false
    }

    fun updateItemQuantity(item: ShoppingListItem, newQuantity: Double) {
        viewModelScope.launch {
            // unitPrice alanı varsa onu kullan, yoksa mevcut fiyatı miktara böl
            val pricePerUnit = if (item.unitPrice > 0) item.unitPrice else item.price / item.quantity
            val updatedItem = item.copy(
                quantity = newQuantity,
                price = pricePerUnit * newQuantity
            )
            repository.updateItem(updatedItem)
            notifyListChanged()
        }
    }
    
    fun completeShoppingList() {
        viewModelScope.launch {
            val activeList = _activeShoppingList.value
            if (activeList != null) {
                // Listeyi tamamlandı olarak işaretle
                val completedList = activeList.copy(isCompleted = true)
                repository.updateShoppingList(completedList)
                
                // Yeni boş bir liste oluştur
                createNewShoppingList("Alışveriş Listem")
            }
        }
    }

    fun deleteCompletedList(list: ShoppingList) {
        viewModelScope.launch {
            repository.deleteShoppingList(list)
        }
    }

    fun getItemsForList(listId: Int): Flow<List<ShoppingListItem>> {
        return repository.getItemsForList(listId)
    }
    
    fun setOnListChangedCallback(callback: (Int) -> Unit) {
        onListChangedCallback = callback
    }
    
    private fun notifyListChanged() {
        _activeShoppingList.value?.let { list ->
            onListChangedCallback?.invoke(list.id)
        }
    }
}