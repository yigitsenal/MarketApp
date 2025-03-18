package com.yigitsenal.marketapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.model.ShoppingList
import com.yigitsenal.marketapp.data.model.ShoppingListItem

@Database(
    entities = [
        MarketItem::class,
        ShoppingList::class,
        ShoppingListItem::class
    ],
    version = 7,
    exportSchema = false
)
abstract class MarketDatabase : RoomDatabase() {
    abstract fun marketItemDao(): MarketItemDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao

    companion object {
        @Volatile
        private var INSTANCE: MarketDatabase? = null

        fun getDatabase(context: Context): MarketDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarketDatabase::class.java,
                    "market_database"
                )
                .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop existing table if exists
                database.execSQL("DROP TABLE IF EXISTS market_items")
                
                // Create new table with all required columns
                database.execSQL("""
                    CREATE TABLE market_items (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL DEFAULT '',
                        description TEXT NOT NULL DEFAULT '',
                        price REAL NOT NULL DEFAULT 0.0,
                        originalPrice REAL,
                        unit TEXT NOT NULL DEFAULT '',
                        quantity INTEGER NOT NULL DEFAULT 1,
                        imagePath TEXT NOT NULL DEFAULT '',
                        productPath TEXT NOT NULL DEFAULT '',
                        isOnSale INTEGER NOT NULL DEFAULT 0,
                        isPriceByWeight INTEGER NOT NULL DEFAULT 0
                    )
                """)
            }
        }
        
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create shopping lists table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_lists (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        date INTEGER NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0
                    )
                """)
                
                // Create shopping list items table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS shopping_list_items (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        unitPrice REAL NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        date INTEGER NOT NULL
                    )
                """)
            }
        }
        
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop existing market_items table
                database.execSQL("DROP TABLE IF EXISTS market_items")
                
                // Create new market_items table with current schema
                database.execSQL("""
                    CREATE TABLE market_items (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL DEFAULT '',
                        brand TEXT NOT NULL DEFAULT '',
                        price REAL NOT NULL DEFAULT 0.0,
                        unit_price REAL NOT NULL DEFAULT 0.0,
                        quantity INTEGER NOT NULL DEFAULT 0,
                        unit TEXT NOT NULL DEFAULT '',
                        merchant_id TEXT NOT NULL DEFAULT '',
                        merchant_logo TEXT NOT NULL DEFAULT '',
                        image TEXT NOT NULL DEFAULT '',
                        url TEXT NOT NULL DEFAULT ''
                    )
                """)
            }
        }
        
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Yedek tablo oluştur
                database.execSQL("""
                    CREATE TABLE shopping_list_items_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        unitPrice REAL NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        date INTEGER NOT NULL,
                        FOREIGN KEY (listId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """)
                
                // Mevcut verileri yedek tabloya kopyala (listId alanı için varsayılan değer 1 kullan)
                database.execSQL("""
                    INSERT OR IGNORE INTO shopping_list_items_backup (id, listId, name, quantity, unit, unitPrice, isCompleted, date)
                    SELECT id, 1, name, quantity, unit, unitPrice, isCompleted, date FROM shopping_list_items
                """)
                
                // Eski tabloyu sil
                database.execSQL("DROP TABLE shopping_list_items")
                
                // Yedek tabloyu yeniden adlandır
                database.execSQL("ALTER TABLE shopping_list_items_backup RENAME TO shopping_list_items")
                
                // listId için indeks oluştur
                database.execSQL("CREATE INDEX index_shopping_list_items_listId ON shopping_list_items(listId)")
            }
        }
        
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Yedek tablo oluştur
                database.execSQL("""
                    CREATE TABLE shopping_list_items_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        unitPrice REAL NOT NULL,
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        date INTEGER NOT NULL,
                        imageUrl TEXT,
                        FOREIGN KEY (listId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """)
                
                // Mevcut verileri yedek tabloya kopyala
                database.execSQL("""
                    INSERT INTO shopping_list_items_backup (
                        id, listId, name, quantity, unit, unitPrice, isCompleted, date
                    )
                    SELECT id, listId, name, quantity, unit, unitPrice, isCompleted, date 
                    FROM shopping_list_items
                """)
                
                // Eski tabloyu sil
                database.execSQL("DROP TABLE shopping_list_items")
                
                // Yedek tabloyu yeniden adlandır
                database.execSQL("ALTER TABLE shopping_list_items_backup RENAME TO shopping_list_items")
                
                // listId için indeks oluştur
                database.execSQL("CREATE INDEX index_shopping_list_items_listId ON shopping_list_items(listId)")
            }
        }
    }
}