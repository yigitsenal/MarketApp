package com.yigitsenal.marketapp.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.yigitsenal.marketapp.data.local.ShoppingListDao
import com.yigitsenal.marketapp.data.local.ShoppingListItemDao
import com.yigitsenal.marketapp.data.model.ShoppingList
import com.yigitsenal.marketapp.data.model.ShoppingListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShoppingListRepository @Inject constructor(
    private val shoppingListDao: ShoppingListDao,
    private val shoppingListItemDao: ShoppingListItemDao,
    private val firestore: FirebaseFirestore
) {    fun getAllShoppingLists(userId: String): Flow<List<ShoppingList>> {
        return shoppingListDao.getAllShoppingLists(userId)
    }
    
    suspend fun getShoppingListById(id: Int, userId: String): ShoppingList? {
        return shoppingListDao.getShoppingListById(id, userId)
    }
    
    suspend fun insertShoppingList(shoppingList: ShoppingList): Long {
        val localId = shoppingListDao.insertShoppingList(shoppingList)
        
        // Firestore'a da kaydet
        try {
            val firestoreData = hashMapOf(
                "id" to localId,
                "name" to shoppingList.name,
                "date" to shoppingList.date,
                "isCompleted" to shoppingList.isCompleted,
                "userId" to shoppingList.userId
            )
            firestore.collection("shopping_lists")
                .document("${shoppingList.userId}_$localId")
                .set(firestoreData)
                .await()
        } catch (e: Exception) {
            // Firestore hatası durumunda local kayıt devam eder
            e.printStackTrace()
        }
        
        return localId
    }
    
    suspend fun updateShoppingList(shoppingList: ShoppingList) {
        shoppingListDao.updateShoppingList(shoppingList)
        
        // Firestore'da da güncelle
        try {
            val firestoreData = hashMapOf(
                "id" to shoppingList.id,
                "name" to shoppingList.name,
                "date" to shoppingList.date,
                "isCompleted" to shoppingList.isCompleted,
                "userId" to shoppingList.userId
            )
            firestore.collection("shopping_lists")
                .document("${shoppingList.userId}_${shoppingList.id}")
                .set(firestoreData)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun deleteShoppingList(shoppingList: ShoppingList) {
        shoppingListDao.deleteShoppingList(shoppingList)
        
        // Firestore'dan da sil
        try {
            firestore.collection("shopping_lists")
                .document("${shoppingList.userId}_${shoppingList.id}")
                .delete()
                .await()
                
            // Liste öğelerini de sil
            val itemsSnapshot = firestore.collection("shopping_list_items")
                .whereEqualTo("listId", shoppingList.id)
                .whereEqualTo("userId", shoppingList.userId)
                .get()
                .await()
                
            itemsSnapshot.documents.forEach { doc ->
                doc.reference.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun syncShoppingListsFromFirestore(userId: String) {
        try {
            val snapshot = firestore.collection("shopping_lists")
                .whereEqualTo("userId", userId)
                .get()
                .await()
                
            snapshot.documents.forEach { doc ->
                val data = doc.data
                if (data != null) {
                    val shoppingList = ShoppingList(
                        id = (data["id"] as Long).toInt(),
                        name = data["name"] as String,
                        date = data["date"] as Long,
                        isCompleted = data["isCompleted"] as Boolean,
                        userId = data["userId"] as String
                    )
                    
                    // Local veritabanında yoksa ekle
                    val existingList = shoppingListDao.getShoppingListById(shoppingList.id, userId)
                    if (existingList == null) {
                        shoppingListDao.insertShoppingList(shoppingList)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    

    fun getItemsForList(listId: Int): Flow<List<ShoppingListItem>> {
        return shoppingListItemDao.getItemsForList(listId)
    }
    
    suspend fun getItemById(id: Int): ShoppingListItem? {
        return shoppingListItemDao.getItemById(id)
    }
      suspend fun insertItem(item: ShoppingListItem): Long {
        val localId = shoppingListItemDao.insertItem(item)
        
        // Firestore'a da kaydet
        try {
            val firestoreData = hashMapOf(
                "id" to localId,
                "listId" to item.listId,
                "name" to item.name,
                "quantity" to item.quantity,
                "unit" to item.unit,
                "price" to item.price,
                "unitPrice" to item.unitPrice,
                "merchantId" to item.merchantId,
                "merchantLogo" to item.merchantLogo,
                "imageUrl" to item.imageUrl,
                "isCompleted" to item.isCompleted,
                "date" to item.date,
                "userId" to getUserIdFromListId(item.listId)
            )
            firestore.collection("shopping_list_items")
                .document("${getUserIdFromListId(item.listId)}_${item.listId}_$localId")
                .set(firestoreData)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return localId
    }
    
    suspend fun updateItem(item: ShoppingListItem) {
        shoppingListItemDao.updateItem(item)
        
        // Firestore'da da güncelle
        try {
            val userId = getUserIdFromListId(item.listId)
            val firestoreData = hashMapOf(
                "id" to item.id,
                "listId" to item.listId,
                "name" to item.name,
                "quantity" to item.quantity,
                "unit" to item.unit,
                "price" to item.price,
                "unitPrice" to item.unitPrice,
                "merchantId" to item.merchantId,
                "merchantLogo" to item.merchantLogo,
                "imageUrl" to item.imageUrl,
                "isCompleted" to item.isCompleted,
                "date" to item.date,
                "userId" to userId
            )
            firestore.collection("shopping_list_items")
                .document("${userId}_${item.listId}_${item.id}")
                .set(firestoreData)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    suspend fun deleteItem(item: ShoppingListItem) {
        shoppingListItemDao.deleteItem(item)
        
        // Firestore'dan da sil
        try {
            val userId = getUserIdFromListId(item.listId)
            firestore.collection("shopping_list_items")
                .document("${userId}_${item.listId}_${item.id}")
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
      suspend fun syncShoppingListItemsFromFirestore(userId: String, listId: Int) {
        try {
            val snapshot = firestore.collection("shopping_list_items")
                .whereEqualTo("listId", listId)
                .whereEqualTo("userId", userId)
                .get()
                .await()
                  snapshot.documents.forEach { doc ->
                val data = doc.data
                if (data != null) {                    try {
                        val isCompletedValue = data["isCompleted"] as? Boolean ?: false
                        val item = ShoppingListItem(
                            id = (data["id"] as Long).toInt(),
                            listId = (data["listId"] as Long).toInt(),
                            name = data["name"] as String,
                            quantity = data["quantity"] as Double,
                            unit = data["unit"] as String,
                            price = data["price"] as Double,
                            unitPrice = data["unitPrice"] as Double,
                            merchantId = data["merchantId"] as String,
                            merchantLogo = data["merchantLogo"] as String,
                            imageUrl = data["imageUrl"] as String,
                            isCompleted = isCompletedValue,
                            date = data["date"] as Long
                        )
                        
                        println("Firestore'dan item yüklendi: ${item.name}, isCompleted: ${item.isCompleted}")
                        
                        // Local veritabanında aynı listId ve item kombinasyonu var mı kontrol et
                        val existingItems = shoppingListItemDao.getItemsForListSync(listId)
                        val existingItem = existingItems.find { 
                            it.id == item.id && it.listId == item.listId 
                        }
                        
                        if (existingItem == null) {
                            shoppingListItemDao.insertItem(item)
                        } else {
                            // Mevcut öğeyi güncelle - özellikle isCompleted durumunu
                            shoppingListItemDao.updateItem(item)
                        }
                    } catch (e: Exception) {
                        // Parsing hatası durumunda log
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
      private suspend fun getUserIdFromListId(listId: Int): String {
        // Liste ID'sinden kullanıcı ID'sini almak için local veritabanından sorgula
        return try {
            // Tüm listeleri sorgula ve listId'ye göre bul
            val allUserIds = mutableListOf<String>()
            
            // Bu basit bir implementation, gerçek uygulamada cache kullanılabilir
            return firestore.collection("shopping_lists")
                .whereEqualTo("id", listId)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.getString("userId") ?: ""
        } catch (e: Exception) {
            // Fallback: local veritabanından bul
            try {
                // Bu durumda tüm kullanıcıları kontrol etmek gerekir
                // Daha iyi bir yaklaşım, ShoppingListItem modelinde userId tutmak olurdu
                ""
            } catch (e2: Exception) {
                ""
            }
        }
    }    suspend fun updateItemCompletionStatus(id: Int, isCompleted: Boolean, userId: String? = null) {
        // Local veritabanını güncelle
        shoppingListItemDao.updateItemCompletionStatus(id, isCompleted)
        
        println("Item completion güncellendi: id=$id, isCompleted=$isCompleted")
        
        // Firestore'u da güncelle
        try {
            val item = shoppingListItemDao.getItemById(id)
            if (item != null) {
                val userIdToUse = userId ?: getUserIdFromListId(item.listId)
                val updatedItem = item.copy(isCompleted = isCompleted)
                
                println("Firestore'a completion status kaydediliyor: ${updatedItem.name}, isCompleted: ${updatedItem.isCompleted}")
                
                firestore.collection("shopping_list_items")
                    .document("${userIdToUse}_${item.listId}_${item.id}")
                    .set(mapOf(
                        "id" to updatedItem.id,
                        "listId" to updatedItem.listId,
                        "name" to updatedItem.name,
                        "quantity" to updatedItem.quantity,
                        "unit" to updatedItem.unit,
                        "price" to updatedItem.price,
                        "unitPrice" to updatedItem.unitPrice,
                        "merchantId" to updatedItem.merchantId,
                        "merchantLogo" to updatedItem.merchantLogo,
                        "imageUrl" to updatedItem.imageUrl,
                        "isCompleted" to updatedItem.isCompleted,
                        "date" to updatedItem.date,
                        "userId" to userIdToUse
                    ))
                    .await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    }
    

