package com.yigitsenal.marketapp

import android.app.Application
import com.yigitsenal.marketapp.data.local.MarketDatabase
import com.yigitsenal.marketapp.data.network.RetrofitClient
import com.yigitsenal.marketapp.data.repository.MarketRepository
import com.yigitsenal.marketapp.data.repository.ShoppingListRepository
import com.yigitsenal.marketapp.ui.viewmodel.MarketViewModelFactory
import com.yigitsenal.marketapp.ui.viewmodel.ShoppingListViewModelFactory

class MarketApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(private val application: Application) {
    private val database = MarketDatabase.getDatabase(application)
    private val marketDao = database.marketItemDao()
    private val shoppingListDao = database.shoppingListDao()
    private val shoppingListItemDao = database.shoppingListItemDao()
    private val apiService = RetrofitClient.getApiService()

    private val marketRepository = MarketRepository(marketDao, apiService)
    private val shoppingListRepository = ShoppingListRepository(shoppingListDao, shoppingListItemDao)

    val marketViewModelFactory = MarketViewModelFactory(marketRepository)
    val shoppingListViewModelFactory = ShoppingListViewModelFactory(shoppingListRepository)
} 