package com.yigitsenal.marketapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import com.yigitsenal.marketapp.data.dao.UserDao
import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.model.ShoppingList
import com.yigitsenal.marketapp.data.model.ShoppingListItem
import com.yigitsenal.marketapp.data.model.User

@Database(
    entities = [
        MarketItem::class,
        ShoppingList::class,
        ShoppingListItem::class,
        User::class
    ],
    version = 12,
    exportSchema = false
)
abstract class MarketDatabase : RoomDatabase() {
    abstract fun marketItemDao(): MarketItemDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: MarketDatabase? = null

        fun getDatabase(context: Context): MarketDatabase {            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MarketDatabase::class.java,
                    "market_database"
                )                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12
                )
                .fallbackToDestructiveMigration()
                .allowMainThreadQueries() // Only for debugging - remove in production
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
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
        
        val MIGRATION_2_3 = object : Migration(2, 3) {
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
        
        val MIGRATION_3_4 = object : Migration(3, 4) {
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
        
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Yedek tablo oluştur
                database.execSQL("""
                    CREATE TABLE shopping_list_items_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        price REAL NOT NULL DEFAULT 0.0,
                        unitPrice REAL NOT NULL DEFAULT 0.0,
                        merchantId TEXT NOT NULL DEFAULT '',
                        merchantLogo TEXT NOT NULL DEFAULT '',
                        imageUrl TEXT NOT NULL DEFAULT '',
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        date INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (listId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """)
                
                // Mevcut verileri yedek tabloya kopyala
                database.execSQL("""
                    INSERT INTO shopping_list_items_backup (
                        id, listId, name, quantity, unit, price, unitPrice, 
                        merchantId, merchantLogo, imageUrl, isCompleted, date
                    )
                    SELECT 
                        id, listId, name, quantity, unit, 
                        COALESCE(unitPrice * quantity, 0.0) as price,
                        COALESCE(unitPrice, 0.0) as unitPrice,
                        '' as merchantId,
                        COALESCE(merchant_logo, '') as merchantLogo,
                        COALESCE(imageUrl, '') as imageUrl,
                        isCompleted,
                        date
                    FROM shopping_list_items
                """)
                
                // Eski tabloyu sil
                database.execSQL("DROP TABLE shopping_list_items")
                
                // Yedek tabloyu yeniden adlandır
                database.execSQL("ALTER TABLE shopping_list_items_backup RENAME TO shopping_list_items")
                
                // İndeksi yeniden oluştur
                database.execSQL("CREATE INDEX index_shopping_list_items_listId ON shopping_list_items(listId)")
            }
        }
        
        val MIGRATION_5_6 = object : Migration(5, 6) {
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
        
        val MIGRATION_6_7 = object : Migration(6, 7) {
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
        
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Yedek tablo oluştur
                database.execSQL("""
                    CREATE TABLE shopping_list_items_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        price REAL NOT NULL DEFAULT 0.0,
                        unitPrice REAL NOT NULL DEFAULT 0.0,
                        merchantId TEXT NOT NULL DEFAULT '',
                        merchantLogo TEXT NOT NULL DEFAULT '',
                        imageUrl TEXT NOT NULL DEFAULT '',
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        date INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (listId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """)

                // Mevcut verileri yedek tabloya kopyala
                database.execSQL("""
                    INSERT INTO shopping_list_items_backup (
                        id, listId, name, quantity, unit, price, unitPrice, 
                        merchantId, merchantLogo, imageUrl, isCompleted, date
                    )
                    SELECT 
                        id, listId, name, quantity, unit, 
                        COALESCE(unitPrice * quantity, 0.0) as price,
                        COALESCE(unitPrice, 0.0) as unitPrice,
                        '' as merchantId,
                        COALESCE(merchant_logo, '') as merchantLogo,
                        COALESCE(imageUrl, '') as imageUrl,
                        isCompleted,
                        date
                    FROM shopping_list_items
                """)

                // Eski tabloyu sil
                database.execSQL("DROP TABLE shopping_list_items")

                // Yedek tabloyu yeniden adlandır
                database.execSQL("ALTER TABLE shopping_list_items_backup RENAME TO shopping_list_items")

                // İndeksi yeniden oluştur
                database.execSQL("CREATE INDEX index_shopping_list_items_listId ON shopping_list_items(listId)")
            }
        }
        
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Yedek tablo oluştur
                database.execSQL("""
                    CREATE TABLE shopping_list_items_backup (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        listId INTEGER NOT NULL,
                        name TEXT NOT NULL,
                        quantity REAL NOT NULL,
                        unit TEXT NOT NULL,
                        price REAL NOT NULL DEFAULT 0.0,
                        unitPrice REAL NOT NULL DEFAULT 0.0,
                        merchantId TEXT NOT NULL DEFAULT '',
                        merchantLogo TEXT NOT NULL DEFAULT '',
                        imageUrl TEXT NOT NULL DEFAULT '',
                        isCompleted INTEGER NOT NULL DEFAULT 0,
                        date INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (listId) REFERENCES shopping_lists(id) ON DELETE CASCADE
                    )
                """)

                // Mevcut verileri yedek tabloya kopyala
                database.execSQL("""
                    INSERT INTO shopping_list_items_backup (
                        id, listId, name, quantity, unit, price, unitPrice, 
                        merchantId, merchantLogo, imageUrl, isCompleted, date
                    )
                    SELECT 
                        id, listId, name, quantity, unit, 
                        COALESCE(unitPrice * quantity, 0.0) as price,
                        COALESCE(unitPrice, 0.0) as unitPrice,
                        '' as merchantId,
                        '' as merchantLogo,
                        COALESCE(imageUrl, '') as imageUrl,
                        isCompleted,
                        COALESCE(date, strftime('%s', 'now') * 1000)
                    FROM shopping_list_items
                """)

                // Eski tabloyu sil
                database.execSQL("DROP TABLE shopping_list_items")

                // Yedek tabloyu yeniden adlandır
                database.execSQL("ALTER TABLE shopping_list_items_backup RENAME TO shopping_list_items")

                // İndeksi yeniden oluştur
                database.execSQL("CREATE INDEX index_shopping_list_items_listId ON shopping_list_items(listId)")
            }
        }
          val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add offer_count column to market_items table
                database.execSQL("ALTER TABLE market_items ADD COLUMN offer_count INTEGER NOT NULL DEFAULT 0")
            }
        }
          val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create users table for Firebase authentication
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        uid TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        photoUrl TEXT NOT NULL,
                        isEmailVerified INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastSignInAt INTEGER NOT NULL
                    )
                """)
            }
        }
        
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add userId column to shopping_lists table
                database.execSQL("ALTER TABLE shopping_lists ADD COLUMN userId TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}