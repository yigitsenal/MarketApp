package com.yigitsenal.marketapp.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.model.ShoppingList
import com.yigitsenal.marketapp.data.model.ShoppingListItem
import com.yigitsenal.marketapp.data.repository.ShoppingListRepository
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

class ShoppingListViewModel(
    private val repository: ShoppingListRepository
) : ViewModel() {
    
    private val _activeShoppingList = MutableStateFlow<ShoppingList?>(null)
    val activeShoppingList: StateFlow<ShoppingList?> = _activeShoppingList
    
    // Tüm alışveriş listeleri
    val allShoppingLists = repository.getAllShoppingLists()
        .stateIn(
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
            repository.getAllShoppingLists().collect { lists ->
                if (lists.isEmpty()) {
                    createNewShoppingList("Alışveriş Listem")
                } else if (_activeShoppingList.value == null) {
                    _activeShoppingList.value = lists.first()
                }
            }
        }
    }
    
    fun createNewShoppingList(name: String) {
        viewModelScope.launch {
            val newList = ShoppingList(
                name = name,
                date = Date().time,
                isCompleted = false
            )
            val id = repository.insertShoppingList(newList)
            _activeShoppingList.value = newList.copy(id = id.toInt())
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
                    unitPrice = unitPrice,
                    isCompleted = false,
                    date = Date().time
                )
                repository.insertItem(newItem)
            }
        }
    }
    
    // Market ürününden yeni öğe ekle
    fun addItemFromMarket(marketItem: MarketItem) {
        viewModelScope.launch {
            // Görsel URL'lerini düzenle
            val imageUrl = when {
                marketItem.image.startsWith("/") -> "http://10.0.2.2:8000${marketItem.image}"
                marketItem.image.contains("file=") -> {
                    val filename = marketItem.image.substringAfter("file=").substringBefore("&")
                    "http://10.0.2.2:8000/image.php?file=${filename}&size=md"
                }
                else -> "http://10.0.2.2:8000/image.php?file=${marketItem.image}&size=md"
            }

            val merchantLogo = when {
                marketItem.merchant_logo.startsWith("/") -> "http://10.0.2.2:8000${marketItem.merchant_logo}"
                marketItem.merchant_logo.contains("file=") -> {
                    val filename = marketItem.merchant_logo.substringAfter("file=").substringBefore("&")
                    "http://10.0.2.2:8000/image.php?file=${filename}&size=sm"
                }
                else -> "http://10.0.2.2:8000/image.php?file=${marketItem.merchant_logo}&size=sm"
            }

            // Aynı ürünün aynı mağazadan olup olmadığını kontrol et
            val existingItem = activeListItems.value.find { item ->
                item.name == marketItem.name && 
                item.merchantId == marketItem.merchant_id
            }

            if (existingItem != null) {
                // Aynı mağazadan aynı ürün varsa miktarını ve fiyatını güncelle
                val updatedItem = existingItem.copy(
                    quantity = existingItem.quantity + 1.0,
                    price = existingItem.price + marketItem.price
                )
                repository.updateItem(updatedItem)
            } else {
                // Farklı mağazadan veya yeni ürünse yeni satır olarak ekle
                val newItem = ShoppingListItem(
                    listId = _activeShoppingList.value?.id ?: 0,
                    name = marketItem.name,
                    quantity = 1.0,
                    unit = "adet",
                    price = marketItem.price,
                    unitPrice = marketItem.unit_price,
                    merchantId = marketItem.merchant_id,
                    merchantLogo = merchantLogo,
                    imageUrl = imageUrl,
                    isCompleted = false
                )
                repository.insertItem(newItem)
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
                val updatedItem = existingItem.copy(
                    quantity = existingItem.quantity + 1.0,
                    price = existingItem.price + marketItem.price
                )
                repository.updateItem(updatedItem)
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
                    val updatedItem = existingItem.copy(
                        quantity = existingItem.quantity - 1.0,
                        price = existingItem.price - marketItem.price
                    )
                    repository.updateItem(updatedItem)
                } else {
                    repository.deleteItem(existingItem)
                }
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
        }
    }
    
    fun updateItemDetails(item: ShoppingListItem, name: String, quantity: Double, unit: String, unitPrice: Double) {
        viewModelScope.launch {
            repository.updateItem(
                item.copy(
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    unitPrice = unitPrice
                )
            )
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
        }
    }

    fun clearSelection() {
        _selectedItems.value = emptySet()
        _isSelectionMode.value = false
    }

    fun updateItemQuantity(item: ShoppingListItem, newQuantity: Double) {
        viewModelScope.launch {
            val updatedItem = item.copy(
                quantity = newQuantity,
                price = (newQuantity * item.unitPrice)
            )
            repository.updateItem(updatedItem)
        }
    }
}

class ShoppingListViewModelFactory(private val repository: ShoppingListRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 