package com.yigitsenal.marketapp.ui.screen

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.yigitsenal.marketapp.data.model.MarketItem
import com.yigitsenal.marketapp.data.model.Offer
import com.yigitsenal.marketapp.data.model.PriceHistory
import com.yigitsenal.marketapp.data.model.ProductDetail
import com.yigitsenal.marketapp.data.model.ProductDetailResponse
import com.yigitsenal.marketapp.ui.component.PriceHistoryChart
import com.yigitsenal.marketapp.ui.component.TimeRange
import com.yigitsenal.marketapp.ui.theme.PrimaryColor
import com.yigitsenal.marketapp.ui.theme.SecondaryColor
import com.yigitsenal.marketapp.ui.viewmodel.MarketUiState
import com.yigitsenal.marketapp.ui.viewmodel.MarketViewModel
import com.yigitsenal.marketapp.ui.viewmodel.ShoppingListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketScreen(
    viewModel: MarketViewModel,
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    shoppingListViewModel: ShoppingListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchText by viewModel.newItemText.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()
    val currentSort by viewModel.sortOption.collectAsState()

    // Filtreleme dialog'unu göstermek için state
    var showSortDialog by remember { mutableStateOf(false) }

    // Popup gösterme durumunu takip etmek için state oluşturalım
    var showProductDetails by remember { mutableStateOf(false) }

    // Eğer seçilen ürün değiştiyse ve null değilse, popup'ı göster
    if (selectedProduct != null && !showProductDetails) {
        showProductDetails = true
    }

    // Sıralama dialog'u
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            title = {
                Text(
                    text = "Sıralama",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = currentSort == "price-asc",
                        onClick = {
                            viewModel.updateSortOption("price-asc")
                            showSortDialog = false
                        },
                        label = { Text("En Düşük Fiyat") },
                        leadingIcon = if (currentSort == "price-asc") {
                            {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )

                    FilterChip(
                        selected = currentSort == "specUnit-asc",
                        onClick = {
                            viewModel.updateSortOption("specUnit-asc")
                            showSortDialog = false
                        },
                        label = { Text("En Düşük Birim Fiyat") },
                        leadingIcon = if (currentSort == "specUnit-asc") {
                            {
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else null
                    )

                    if (currentSort != null) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                viewModel.updateSortOption(null)
                                showSortDialog = false
                            },
                            label = { Text("Sıralamayı Kaldır") }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSortDialog = false }) {
                    Text("Kapat")
                }
            }
        )
    }

    // Popup dialog'u gösteriyoruz
    if (showProductDetails && selectedProduct != null) {
        ProductDetailDialog(
            product = selectedProduct!!,
            onDismiss = {
                showProductDetails = false
                viewModel.setSelectedProduct(null)
            },
            viewModel = viewModel,
            onAddToCart = { product ->
                shoppingListViewModel.addItemFromMarket(product)
                onBackPressed()
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { onBackPressed() },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Geri",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        Text(
                            text = "Ürün Ara",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { viewModel.updateNewItemText(it) },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("tuvalet kağıdı") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Search,
                                capitalization = KeyboardCapitalization.None,
                                autoCorrect = true
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = Color.LightGray,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        IconButton(
                            onClick = { showSortDialog = true },
                            modifier = Modifier
                                .background(
                                    color = if (currentSort != null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(4.dp)
                        ) {
                            Icon(
                                Icons.Default.List,
                                contentDescription = "Sırala",
                                tint = if (currentSort != null) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val currentState = uiState) {
                is MarketUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is MarketUiState.Success -> {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut()
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                top = 8.dp,
                                bottom = 8.dp,
                                start = 16.dp,
                                end = 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(
                                items = currentState.items,
                                key = { it.id }
                            ) { item ->
                                ProductCard(
                                    item = item,
                                    onClick = {
                                        // Ürün kart'ına tıklandığında
                                        Log.d("MarketScreen", "Product clicked: ${item.id}")
                                        Log.d("MarketScreen", "Product URL: ${item.url}")
                                        Log.d("MarketScreen", "Product name: ${item.name}")
                                        viewModel.setSelectedProduct(item)
                                    },
                                    onAddToCart = { product ->
                                        shoppingListViewModel.addItemFromMarket(product)
                                        onBackPressed()
                                    }
                                )
                            }
                        }
                    }
                }
                is MarketUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentState.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    item: MarketItem,
    onClick: () -> Unit,
    onAddToCart: (MarketItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Image URL handling
                val imageUrl = if (item.image.startsWith("/")) {
                    "http://10.0.2.2:8000${item.image}"
                } else if (item.image.contains("file=")) {
                    val filename = item.image.substringAfter("file=").substringBefore("&")
                    "http://10.0.2.2:8000//image.php?file=${filename}&size=md"
                } else {
                    "http://10.0.2.2:8000//image.php?file=${item.image}&size=md"
                }

                Log.d("MarketApp", "Using image URL: $imageUrl")

                // Product Image
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF5F5F5))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                // Product Info
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Unit count badge and store logo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Unit count badge (e.g. 40 lt)
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "${item.quantity} ${item.unit}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Store logo - using merchant_logo field directly
                            if (item.merchant_logo.isNotEmpty()) {
                                val logoUrl = if (item.merchant_logo.startsWith("/")) {
                                    "http://10.0.2.2:8000${item.merchant_logo}"
                                } else {
                                    "http://10.0.2.2:8000/${item.merchant_logo}"
                                }

                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(logoUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Store logo",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }

                        // Brand name - more prominent
                        Text(
                            text = item.brand,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF2196F3),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Product name
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Divider(color = Color(0xFFEEEEEE), thickness = 1.dp)

            // Price section with add to cart button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Unit price
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Birim Fiyat",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${item.unit_price} ₺",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Total price
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Fiyat",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    Text(
                        text = "${item.price} ₺",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF2196F3),
                        fontWeight = FontWeight.Bold
                    )
                }

                // Add to cart button - now aligned with prices
                IconButton(
                    onClick = { 
                        Log.d("MarketScreen", "Sepete ekle butonuna tıklandı: ${item.name}")
                        onAddToCart(item) 
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF2196F3), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Sepete Ekle",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun ProductDetailDialog(
    product: MarketItem,
    onDismiss: () -> Unit,
    viewModel: MarketViewModel,
    onAddToCart: (MarketItem) -> Unit
) {
    // Fiyat geçmişi ve satıcılar için expand/collapse durumu
    var showPriceHistory by remember { mutableStateOf(true) }
    var showOffers by remember { mutableStateOf(true) }

    // Seçili zaman aralığı
    var selectedTimeRange by remember { mutableStateOf(TimeRange.MONTH) }

    // API'den gelen detayları al
    val productDetails by viewModel.productDetails.collectAsState()
    val isLoadingDetails by viewModel.isLoadingProductDetails.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()

    // Seçilen satıcıyı takip etmek için state ekleyelim
    var selectedOffer by remember { mutableStateOf<Offer?>(null) }

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
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            LazyColumn(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    // Header with close button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ürün Detayları",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryColor
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = Color.LightGray.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Kapat",
                                tint = Color.Gray
                            )
                        }
                    }

                    Divider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = Color.LightGray.copy(alpha = 0.5f)
                    )
                }

                item {
                    // Product Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageUrl = if (product.image.startsWith("/")) {
                            "http://10.0.2.2:8000${product.image}"
                        } else if (product.image.contains("file=")) {
                            val filename = product.image.substringAfter("file=").substringBefore("&")
                            "http://10.0.2.2:8000//image.php?file=${filename}&size=lg"
                        } else {
                            "http://10.0.2.2:8000//image.php?file=${product.image}&size=lg"
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(0.8f),
                            contentScale = ContentScale.Fit
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    // Product details
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Marka
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryColor.copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = product.brand,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryColor
                                    )
                                }

                                // Miktar
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SecondaryColor.copy(alpha = 0.1f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "${product.quantity} ${product.unit}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = SecondaryColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            if (isLoadingDetails) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = PrimaryColor)
                                }
                            } else {
                                // API'den gelen fiyat bilgileri
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        Text(
                                            text = "Birim Fiyat",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )

                                        Text(
                                            text = "${productDetails?.product?.offers?.firstOrNull()?.unit_price ?: product.unit_price} ₺",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Toplam",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.Gray
                                        )

                                        Text(
                                            text = "${productDetails?.product?.offers?.firstOrNull()?.price ?: product.price} ₺",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Fiyat Geçmişi Bölümü
                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Başlık ve genişletme/daraltma butonu
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showPriceHistory = !showPriceHistory }
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Fiyat Geçmişi",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            AnimatedVisibility(visible = showPriceHistory) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Zaman aralığı seçimi
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TimeRange.values().forEach { range ->
                                            FilterChip(
                                                selected = selectedTimeRange == range,
                                                onClick = { selectedTimeRange = range },
                                                label = { Text(range.label) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = PrimaryColor,
                                                    selectedLabelColor = Color.White
                                                )
                                            )
                                        }
                                    }

                                    // Fiyat geçmişi grafiği
                                    if (productDetails?.product?.price_history != null) {
                                        PriceHistoryChart(
                                            priceHistory = productDetails!!.product.price_history,
                                            timeRange = selectedTimeRange
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Satıcılar Bölümü
                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Başlık ve genişletme/daraltma butonu
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Satıcılar (${productDetails?.product?.offers?.size ?: 0})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                productDetails?.product?.offers?.forEach { offer ->
                                    OfferCard(
                                        offer = offer,
                                        onAddToCart = {
                                            selectedOffer = offer
                                            viewModel.updateSelectedProductWithOffer(offer)
                                            onAddToCart(viewModel.selectedProduct.value ?: product)
                                        },
                                        viewModel = viewModel,
                                        isSelected = selectedOffer == offer
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Add to cart button
                    ElevatedButton(
                        onClick = { 
                            Log.d("MarketScreen", "Detay ekranında SEPETE EKLE butonuna tıklandı: ${product.name}")
                            if (selectedOffer != null) {
                                viewModel.updateSelectedProductWithOffer(selectedOffer!!)
                            }
                            onAddToCart(viewModel.selectedProduct.value ?: product)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = PrimaryColor,
                            contentColor = Color.White
                        ),
                        elevation = ButtonDefaults.elevatedButtonElevation(
                            defaultElevation = 6.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Sepete Ekle"
                            )
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Text(
                                text = "SEPETE EKLE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OfferCard(
    offer: Offer,
    onAddToCart: () -> Unit,
    viewModel: MarketViewModel,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE3F2FD) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onAddToCart() }
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mağaza logosu ve adı
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mağaza logosu
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(
                            if (offer.merchant_logo.startsWith("/")) {
                                "http://10.0.2.2:8000${offer.merchant_logo}"
                            } else {
                                "http://10.0.2.2:8000/image.php?file=${offer.merchant_logo}&size=sm"
                            }
                        )
                        .crossfade(true)
                        .build(),
                    contentDescription = offer.merchant_name,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Fit
                )
                
                // Mağaza adı
                Text(
                    text = offer.merchant_name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Fiyat ve sepete ekle butonu
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Fiyat
                Text(
                    text = "₺${String.format("%.2f", offer.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryColor
                )
            }
        }
    }
}
