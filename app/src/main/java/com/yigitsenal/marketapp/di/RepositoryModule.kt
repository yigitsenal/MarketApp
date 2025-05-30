package com.yigitsenal.marketapp.di

import com.yigitsenal.marketapp.data.local.MarketItemDao
import com.yigitsenal.marketapp.data.local.ShoppingListDao
import com.yigitsenal.marketapp.data.local.ShoppingListItemDao
import com.yigitsenal.marketapp.data.network.MarketApiService
import com.yigitsenal.marketapp.data.repository.CartOptimizationRepository
import com.yigitsenal.marketapp.data.repository.MarketRepository
import com.yigitsenal.marketapp.data.repository.ShoppingListRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {    @Provides
    @Singleton
    fun provideMarketRepository(
        marketDao: MarketItemDao,
        apiService: MarketApiService
    ): MarketRepository {
        return MarketRepository(marketDao, apiService)
    }

    @Provides
    @Singleton
    fun provideShoppingListRepository(
        shoppingListDao: ShoppingListDao,
        shoppingListItemDao: ShoppingListItemDao
    ): ShoppingListRepository {
        return ShoppingListRepository(shoppingListDao, shoppingListItemDao)
    }

    @Provides
    @Singleton
    fun provideCartOptimizationRepository(
        apiService: MarketApiService,
        shoppingListItemDao: ShoppingListItemDao
    ): CartOptimizationRepository {
        return CartOptimizationRepository(apiService, shoppingListItemDao)
    }
}
