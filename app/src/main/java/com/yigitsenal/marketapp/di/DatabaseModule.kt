package com.yigitsenal.marketapp.di

import android.content.Context
import androidx.room.Room
import com.yigitsenal.marketapp.data.local.MarketDatabase
import com.yigitsenal.marketapp.data.local.ShoppingListDao
import com.yigitsenal.marketapp.data.local.ShoppingListItemDao
import com.yigitsenal.marketapp.data.local.MarketItemDao
import com.yigitsenal.marketapp.data.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {    @Provides
    @Singleton
    fun provideMarketDatabase(@ApplicationContext context: Context): MarketDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            MarketDatabase::class.java,
            "market_database_v2" // Changed database name to force fresh creation
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideShoppingListDao(database: MarketDatabase): ShoppingListDao {
        return database.shoppingListDao()
    }

    @Provides
    fun provideShoppingListItemDao(database: MarketDatabase): ShoppingListItemDao {
        return database.shoppingListItemDao()
    }    @Provides
    fun provideMarketDao(database: MarketDatabase): MarketItemDao {
        return database.marketItemDao()
    }

    @Provides
    fun provideUserDao(database: MarketDatabase): UserDao {
        return database.userDao()
    }
}
