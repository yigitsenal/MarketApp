package com.yigitsenal.marketapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.annotation.ExperimentalCoilApi
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.util.DebugLogger
import com.yigitsenal.marketapp.data.model.ShoppingList
import com.yigitsenal.marketapp.data.model.ShoppingListItem
import com.yigitsenal.marketapp.ui.screen.MarketScreen
import com.yigitsenal.marketapp.ui.screen.ShoppingListScreen
import com.yigitsenal.marketapp.ui.theme.MarketAppTheme
import com.yigitsenal.marketapp.ui.theme.PrimaryColor
import com.yigitsenal.marketapp.ui.viewmodel.MarketViewModel
import com.yigitsenal.marketapp.ui.viewmodel.ShoppingListViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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
    var selectedCompletedList by remember { mutableStateOf<ShoppingList?>(null) }
    var showCompletedListDialog by remember { mutableStateOf(false) }

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
                val allLists by shoppingListViewModel.allShoppingLists.collectAsState()
                val completedLists = allLists.filter { it.isCompleted }
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                ) {
                    item {
                        Text(
                            text = "Tamamlanmış Alışveriş Listeleri",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    items(completedLists) { list ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { 
                                    selectedCompletedList = list
                                    showCompletedListDialog = true
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = list.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = list.getFormattedDate(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // Completed List Dialog
                if (showCompletedListDialog && selectedCompletedList != null) {
                    CompletedListDialog(
                        list = selectedCompletedList!!,
                        items = shoppingListViewModel.getItemsForList(selectedCompletedList!!.id).collectAsState(initial = emptyList()).value,
                        onDismiss = {
                            showCompletedListDialog = false
                            selectedCompletedList = null
                        }
                    )
                }
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

@Composable
fun CompletedListDialog(
    list: ShoppingList,
    items: List<ShoppingListItem>,
    onDismiss: () -> Unit
) {
    val totalCost = items.sumOf { it.price }
    val completedItems = items.count { it.isCompleted }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(8.dp)
                .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = list.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = list.getFormattedDate(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                        Text(
                            text = "$completedItems/${items.size} ürün alındı",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PrimaryColor
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Kapat")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Items List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (item.imageUrl.isNotEmpty()) {
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Column {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                        color = if (item.isCompleted) Color.Gray else Color.Unspecified
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (item.merchantLogo.isNotEmpty()) {
                                            AsyncImage(
                                                model = item.merchantLogo,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .height(16.dp)
                                                    .width(48.dp),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                        Text(
                                            text = "${item.quantity.toInt()} ${item.unit}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray,
                                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${item.price.toInt()} ₺",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (item.isCompleted) Color.Gray else Color.Unspecified
                            )
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Total
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Toplam Tutar",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${totalCost.toInt()} ₺",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryColor
                    )
                }
            }
        }
    }
}