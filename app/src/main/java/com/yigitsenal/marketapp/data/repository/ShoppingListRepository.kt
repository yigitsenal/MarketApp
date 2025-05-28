package com.yigitsenal.marketapp.data.repository

import com.yigitsenal.marketapp.data.local.ShoppingListDao
import com.yigitsenal.marketapp.data.local.ShoppingListItemDao
import com.yigitsenal.marketapp.data.model.ShoppingList
import com.yigitsenal.marketapp.data.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow

class ShoppingListRepository(
    private val shoppingListDao: ShoppingListDao,
    private val shoppingListItemDao: ShoppingListItemDao
) {

    fun getAllShoppingLists(): Flow<List<ShoppingList>> {
        return shoppingListDao.getAllShoppingLists()
    }
    
    suspend fun getShoppingListById(id: Int): ShoppingList? {
        return shoppingListDao.getShoppingListById(id)
    }
    
    suspend fun insertShoppingList(shoppingList: ShoppingList): Long {
        return shoppingListDao.insertShoppingList(shoppingList)
    }
    
    suspend fun updateShoppingList(shoppingList: ShoppingList) {
        shoppingListDao.updateShoppingList(shoppingList)
    }
    
    suspend fun deleteShoppingList(shoppingList: ShoppingList) {
        shoppingListDao.deleteShoppingList(shoppingList)
    }
    

    fun getItemsForList(listId: Int): Flow<List<ShoppingListItem>> {
        return shoppingListItemDao.getItemsForList(listId)
    }
    
    suspend fun getItemById(id: Int): ShoppingListItem? {
        return shoppingListItemDao.getItemById(id)
    }
    
    suspend fun insertItem(item: ShoppingListItem): Long {
        return shoppingListItemDao.insertItem(item)
    }
    
    suspend fun updateItem(item: ShoppingListItem) {
        shoppingListItemDao.updateItem(item)
    }
    
    suspend fun deleteItem(item: ShoppingListItem) {
        shoppingListItemDao.deleteItem(item)
    }
    
    suspend fun updateItemCompletionStatus(id: Int, isCompleted: Boolean) {
        shoppingListItemDao.updateItemCompletionStatus(id, isCompleted)
    }
} 