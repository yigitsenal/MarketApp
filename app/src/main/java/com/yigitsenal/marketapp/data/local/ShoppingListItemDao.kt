package com.yigitsenal.marketapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yigitsenal.marketapp.data.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListItemDao {
    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId")
    fun getItemsForList(listId: Int): Flow<List<ShoppingListItem>>
    
    @Query("SELECT * FROM shopping_list_items WHERE listId = :listId")
    suspend fun getItemsForListSync(listId: Int): List<ShoppingListItem>
    
    @Query("SELECT * FROM shopping_list_items WHERE id = :id")
    suspend fun getItemById(id: Int): ShoppingListItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingListItem): Long
    
    @Update
    suspend fun updateItem(item: ShoppingListItem)
    
    @Delete
    suspend fun deleteItem(item: ShoppingListItem)
    
    @Query("UPDATE shopping_list_items SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateItemCompletionStatus(id: Int, isCompleted: Boolean)
}