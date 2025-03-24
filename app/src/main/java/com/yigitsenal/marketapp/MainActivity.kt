package com.yigitsenal.marketapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.annotation.ExperimentalCoilApi
import coil.util.DebugLogger
import com.yigitsenal.marketapp.ui.screen.MarketScreen
import com.yigitsenal.marketapp.ui.screen.ShoppingListScreen
import com.yigitsenal.marketapp.ui.theme.MarketAppTheme
import com.yigitsenal.marketapp.ui.viewmodel.MarketViewModel
import com.yigitsenal.marketapp.ui.viewmodel.ShoppingListViewModel
import coil.request.CachePolicy

@OptIn(ExperimentalCoilApi::class)
class MainActivity : ComponentActivity(), ImageLoaderFactory {
    private lateinit var marketViewModel: MarketViewModel
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private lateinit var marketViewModelFactory: ViewModelProvider.Factory
    private lateinit var shoppingListViewModelFactory: ViewModelProvider.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ViewModelFactory'leri oluştur
        val appContainer = (application as MarketApplication).container
        marketViewModelFactory = appContainer.marketViewModelFactory
        shoppingListViewModelFactory = appContainer.shoppingListViewModelFactory

        setContent {
            MarketAppTheme {
                val marketViewModel = ViewModelProvider(this, marketViewModelFactory)[MarketViewModel::class.java]
                val shoppingListViewModel = ViewModelProvider(this, shoppingListViewModelFactory)[ShoppingListViewModel::class.java]
                
                MainScreen(
                    marketViewModel = marketViewModel,
                    shoppingListViewModel = shoppingListViewModel
                )
            }
        }
    }
    
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .diskCachePolicy(CachePolicy.DISABLED)
            .logger(DebugLogger())
            .respectCacheHeaders(false)
            .build()
    }
}

enum class Screen {
    HOME,
    SEARCH,
    CART
}

@Composable
fun MainScreen(
    marketViewModel: MarketViewModel,
    shoppingListViewModel: ShoppingListViewModel
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Ana Sayfa") },
                    label = { Text("Ana Sayfa") },
                    selected = currentScreen == Screen.HOME,
                    onClick = { currentScreen = Screen.HOME }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = "Ürün Arama") },
                    label = { Text("Ürün Arama") },
                    selected = currentScreen == Screen.SEARCH,
                    onClick = { currentScreen = Screen.SEARCH }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Sepet") },
                    label = { Text("Sepet") },
                    selected = currentScreen == Screen.CART,
                    onClick = { currentScreen = Screen.CART }
                )
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            Screen.HOME -> {
                // Ana sayfa ekranı
                Text(
                    text = "Ana Sayfa",
                    modifier = Modifier.padding(innerPadding)
                )
            }
            Screen.SEARCH -> {
                MarketScreen(
                    viewModel = marketViewModel,
                    modifier = Modifier.padding(innerPadding),
                    onBackPressed = { currentScreen = Screen.HOME },
                    shoppingListViewModel = shoppingListViewModel
                )
            }
            Screen.CART -> {
                ShoppingListScreen(
                    viewModel = shoppingListViewModel,
                    onNavigateToMarket = { currentScreen = Screen.SEARCH },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}