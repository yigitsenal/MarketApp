package com.yigitsenal.marketapp.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.yigitsenal.marketapp.data.model.MarketItem
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketItemDao {
    @Query("SELECT * FROM market_items")
    fun getAllItems(): Flow<List<MarketItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: MarketItem)

    @Update
    suspend fun updateItem(item: MarketItem)

    @Delete
    suspend fun deleteItem(item: MarketItem)
}