package com.yigitsenal.marketapp

import android.util.Log

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.yigitsenal.marketapp.data.local.MarketDatabase
import com.yigitsenal.marketapp.data.network.RetrofitClient
import com.yigitsenal.marketapp.data.repository.MarketRepository
import com.yigitsenal.marketapp.data.repository.ShoppingListRepository
import com.yigitsenal.marketapp.ui.screen.MarketScreen
import com.yigitsenal.marketapp.ui.screen.ShoppingListScreen
import com.yigitsenal.marketapp.ui.theme.MarketAppTheme
import com.yigitsenal.marketapp.ui.viewmodel.MarketViewModel
import com.yigitsenal.marketapp.ui.viewmodel.MarketViewModelFactory
import com.yigitsenal.marketapp.ui.viewmodel.ShoppingListViewModel
import com.yigitsenal.marketapp.ui.viewmodel.ShoppingListViewModelFactory

class MainActivity : ComponentActivity(), ImageLoaderFactory {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        Log.d("MarketApp", "Creating custom ImageLoader for debugging")
        
        val database = MarketDatabase.getDatabase(applicationContext)
        val marketDao = database.marketItemDao()
        val shoppingListDao = database.shoppingListDao()
        val shoppingListItemDao = database.shoppingListItemDao()
        
        val apiService = RetrofitClient.getApiService()
        
        val marketRepository = MarketRepository(marketDao, apiService)
        val shoppingListRepository = ShoppingListRepository(shoppingListDao, shoppingListItemDao)
        
        val marketViewModelFactory = MarketViewModelFactory(marketRepository)
        val shoppingListViewModelFactory = ShoppingListViewModelFactory(shoppingListRepository)
        
        setContent {
            MarketAppTheme {
                val marketViewModel = ViewModelProvider(this, marketViewModelFactory)[MarketViewModel::class.java]
                val shoppingListViewModel = ViewModelProvider(this, shoppingListViewModelFactory)[ShoppingListViewModel::class.java]
                
                var currentScreen by remember { mutableStateOf(Screen.SHOPPING_LIST) }
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    when (currentScreen) {
                        Screen.SHOPPING_LIST -> {
                            ShoppingListScreen(
                                viewModel = shoppingListViewModel,
                                onNavigateToMarket = { currentScreen = Screen.MARKET },
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        Screen.MARKET -> {
                            MarketScreen(
                                viewModel = marketViewModel,
                                modifier = Modifier.padding(innerPadding),
                                onBackPressed = { currentScreen = Screen.SHOPPING_LIST },
                                shoppingListViewModel = shoppingListViewModel
                            )
                        }
                    }
                }
            }
        }
    }
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCachePolicy(CachePolicy.DISABLED) // Disable memory cache for debugging
            .diskCachePolicy(CachePolicy.DISABLED) // Disable disk cache for debugging
            .logger(DebugLogger()) // Enable debug logging
            .respectCacheHeaders(false) // Ignore cache headers
            .build()
    }
}

enum class Screen {
    SHOPPING_LIST,
    MARKET
}