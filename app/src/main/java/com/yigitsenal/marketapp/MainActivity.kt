package com.yigitsenal.marketapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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
import com.yigitsenal.marketapp.ui.screen.HomeScreen
import com.yigitsenal.marketapp.ui.screen.MarketScreen
import com.yigitsenal.marketapp.ui.screen.ShoppingListScreen
import com.yigitsenal.marketapp.ui.theme.MarketAppTheme
import com.yigitsenal.marketapp.ui.theme.PrimaryColor
import com.yigitsenal.marketapp.ui.viewmodel.MarketViewModel
import com.yigitsenal.marketapp.ui.viewmodel.ShoppingListViewModel
import com.yigitsenal.marketapp.ui.viewmodel.CartOptimizationViewModel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalCoilApi::class)
class MainActivity : ComponentActivity(), ImageLoaderFactory {
    private lateinit var marketViewModel: MarketViewModel
    private lateinit var shoppingListViewModel: ShoppingListViewModel
    private lateinit var cartOptimizationViewModel: CartOptimizationViewModel
    private lateinit var marketViewModelFactory: ViewModelProvider.Factory
    private lateinit var shoppingListViewModelFactory: ViewModelProvider.Factory
    private lateinit var cartOptimizationViewModelFactory: ViewModelProvider.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val appContainer = (application as MarketApplication).container
        marketViewModelFactory = appContainer.marketViewModelFactory
        shoppingListViewModelFactory = appContainer.shoppingListViewModelFactory
        cartOptimizationViewModelFactory = appContainer.cartOptimizationViewModelFactory

        setContent {
            val systemInDarkTheme = isSystemInDarkTheme()
            MarketAppTheme(
                darkTheme = systemInDarkTheme,
                dynamicColor = true
            ) {
                val marketViewModel = ViewModelProvider(this, marketViewModelFactory)[MarketViewModel::class.java]
                val shoppingListViewModel = ViewModelProvider(this, shoppingListViewModelFactory)[ShoppingListViewModel::class.java]
                val cartOptimizationViewModel = ViewModelProvider(this, cartOptimizationViewModelFactory)[CartOptimizationViewModel::class.java]

                // Otomatik optimizasyon için callback kurulumu
                shoppingListViewModel.setOnListChangedCallback { listId ->
                    if (cartOptimizationViewModel.uiState.value.isAutoOptimizationEnabled) {
                        cartOptimizationViewModel.optimizeCart(listId)
                    }
                }

                MainScreen(
                    marketViewModel = marketViewModel,
                    shoppingListViewModel = shoppingListViewModel,
                    cartOptimizationViewModel = cartOptimizationViewModel
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
    shoppingListViewModel: ShoppingListViewModel,
    cartOptimizationViewModel: CartOptimizationViewModel
) {
    var currentScreen by remember { mutableStateOf(Screen.HOME) }
    var selectedCompletedList by remember { mutableStateOf<ShoppingList?>(null) }
    var showCompletedListDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (currentScreen == Screen.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Ana Sayfa"
                        )
                    },
                    label = { Text("Ana Sayfa") },
                    selected = currentScreen == Screen.HOME,
                    onClick = { currentScreen = Screen.HOME },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (currentScreen == Screen.SEARCH) Icons.Filled.Search else Icons.Outlined.Search,
                            contentDescription = "Ürün Arama"
                        )
                    },
                    label = { Text("Ürün Arama") },
                    selected = currentScreen == Screen.SEARCH,
                    onClick = { currentScreen = Screen.SEARCH },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                NavigationBarItem(
                    icon = {
                        Icon(
                            if (currentScreen == Screen.CART) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                            contentDescription = "Sepet"
                        )
                    },
                    label = { Text("Sepet") },
                    selected = currentScreen == Screen.CART,
                    onClick = { currentScreen = Screen.CART },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        }
    ) { innerPadding ->
        Surface(modifier = Modifier.fillMaxSize().padding(innerPadding), color = MaterialTheme.colorScheme.background) {
            when (currentScreen) {
                Screen.HOME -> {
                    val allLists by shoppingListViewModel.allShoppingLists.collectAsState()
                    
                    HomeScreen(
                        allLists = allLists,
                        onNavigateToSearch = { currentScreen = Screen.SEARCH },
                        onNavigateToCart = { currentScreen = Screen.CART },
                        onListClick = { list ->
                            if (list.isCompleted) {
                                selectedCompletedList = list
                                showCompletedListDialog = true
                            } else {
                                // Navigate to active list (cart)
                                shoppingListViewModel.setActiveShoppingList(list)
                                currentScreen = Screen.CART
                            }
                        }
                    )

                    if (showCompletedListDialog && selectedCompletedList != null) {
                        CompletedListDialog(
                            list = selectedCompletedList!!,
                            items = shoppingListViewModel.getItemsForList(selectedCompletedList!!.id).collectAsState(initial = emptyList()).value,
                            onDismiss = {
                                showCompletedListDialog = false
                                selectedCompletedList = null
                            },
                            onDeleteList = { list ->
                                shoppingListViewModel.deleteCompletedList(list)
                                showCompletedListDialog = false
                                selectedCompletedList = null
                            }
                        )
                    }
                }
                Screen.SEARCH -> {
                    MarketScreen(
                        viewModel = marketViewModel,
                        modifier = Modifier,
                        onBackPressed = { currentScreen = Screen.HOME },
                        shoppingListViewModel = shoppingListViewModel
                    )
                }
                Screen.CART -> {
                    ShoppingListScreen(
                        viewModel = shoppingListViewModel,
                        cartOptimizationViewModel = cartOptimizationViewModel,
                        onNavigateToMarket = { currentScreen = Screen.SEARCH },
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
fun CompletedListDialog(
    list: ShoppingList,
    items: List<ShoppingListItem>,
    onDismiss: () -> Unit,
    onDeleteList: (ShoppingList) -> Unit
) {
    val totalCost = items.sumOf { it.price }
    val completedItems = items.count { it.isCompleted }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    // Delete confirmation dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Listeyi Sil",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Bu listeyi silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.",
                    style = MaterialTheme.typography.bodyLarge
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteList(list)
                        showDeleteConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("İptal")
                }
            }
        )
    }

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
                .heightIn(max = 600.dp)
                .padding(16.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 16.dp
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Modern Gradient Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
                                )
                            ),
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            Color.White.copy(alpha = 0.2f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Completed",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Tamamlandı",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = list.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = list.getFormattedDate(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            Text(
                                text = "$completedItems/${items.size} ürün alındı",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = { showDeleteConfirmation = true },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Sil",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.15f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Kapat",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Content Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Items List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(items) { item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.isCompleted) 
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 2.dp
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (item.imageUrl.isNotEmpty()) {
                                            AsyncImage(
                                                model = item.imageUrl,
                                                contentDescription = null,
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .alpha(if (item.isCompleted) 0.6f else 1f),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                style = MaterialTheme.typography.titleMedium,
                                                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                                color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                fontWeight = FontWeight.Medium
                                            )
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (item.merchantLogo.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = item.merchantLogo,
                                                        contentDescription = null,
                                                        modifier = Modifier
                                                            .height(16.dp)
                                                            .width(48.dp)
                                                            .alpha(if (item.isCompleted) 0.6f else 1f),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                }
                                                Text(
                                                    text = "${item.quantity.toInt()} ${item.unit}",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                                )
                                            }
                                        }
                                    }
                                    
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "${item.price.toInt()} ₺",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            else MaterialTheme.colorScheme.primary
                                        )
                                        
                                        if (item.isCompleted) {
                                            Icon(
                                                imageVector = Icons.Filled.CheckCircle,
                                                contentDescription = "Completed",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Total Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 4.dp
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Toplam Tutar",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${totalCost.toInt()} ₺",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Filled.Analytics,
                                contentDescription = "Total",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

