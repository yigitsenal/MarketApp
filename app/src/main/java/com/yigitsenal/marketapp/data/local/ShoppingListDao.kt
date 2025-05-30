package com.yigitsenal.marketapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yigitsenal.marketapp.data.model.ShoppingList
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists WHERE userId = :userId ORDER BY date DESC")
    fun getAllShoppingLists(userId: String): Flow<List<ShoppingList>>
    
    @Query("SELECT * FROM shopping_lists WHERE id = :id AND userId = :userId")
    suspend fun getShoppingListById(id: Int, userId: String): ShoppingList?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingList(shoppingList: ShoppingList): Long
    
    @Update
    suspend fun updateShoppingList(shoppingList: ShoppingList)
    
    @Delete
    suspend fun deleteShoppingList(shoppingList: ShoppingList)
} 