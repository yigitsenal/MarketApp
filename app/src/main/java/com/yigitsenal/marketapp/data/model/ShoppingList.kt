package com.yigitsenal.marketapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "shopping_lists")
data class ShoppingList(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val date: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
) {
    fun getFormattedDate(): String {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("tr"))
        return dateFormat.format(Date(date))
    }
    
    fun getCompletionPercentage(items: List<ShoppingListItem>): Int {
        if (items.isEmpty()) return 0
        val completedItems = items.count { it.isCompleted }
        return ((completedItems.toFloat() / items.size) * 100).toInt()
    }
}