package com.yigitsenal.marketapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "shopping_list_items",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingList::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")]
)
data class ShoppingListItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val listId: Int,
    val name: String,
    val quantity: Double,
    val unit: String,
    val price: Double = 0.0,
    val unitPrice: Double = 0.0,
    val merchantId: String = "",
    val merchantLogo: String = "",
    val imageUrl: String = "",
    val isCompleted: Boolean = false,
    val date: Long = System.currentTimeMillis()
) 