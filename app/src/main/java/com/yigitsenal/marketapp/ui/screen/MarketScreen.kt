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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.LaunchedEffect
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
import java.text.DecimalFormat

// formatWithDecimal uzantı fonksiyonunu ekliyorum
fun Double.formatWithDecimal(): String {
    val formatter = DecimalFormat("#,##0.00")
    return formatter.format(this)
}

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
    
    // Compose'da yan etki - kullanıcı bu ekrana geldiğinde mevcut aramaları ve detayları temizle
    LaunchedEffect(key1 = Unit) {
        viewModel.updateNewItemText("")
    }

    // Filtreleme dialog'unu göstermek için state
    var showSortDialog by remember { mutableStateOf(false) }

    // Popup gösterme durumunu takip etmek için state oluşturalım
    var showProductDetails by remember { mutableStateOf(false) }

    // Eğer seçilen ürün değiştiyse ve null değilse, popup'ı göster
    LaunchedEffect(selectedProduct) {
        if (selectedProduct != null && !showProductDetails) {
            showProductDetails = true
        } else if (selectedProduct == null) {
            showProductDetails = false
        }
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
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    // Arama alanı
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { viewModel.updateNewItemText(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("şeker") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Ara",
                                tint = Color.Gray
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { }) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "Konum",
                                    tint = Color.Gray
                                )
                            }
                        },
                        shape = RoundedCornerShape(30.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Search,
                            capitalization = KeyboardCapitalization.None,
                            autoCorrect = true
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = Color(0xFFF0F0F0),
                            unfocusedContainerColor = Color(0xFFF0F0F0)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Arama sonuçları başlığı ve filtrele butonu
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                           if (searchText.isNotEmpty()) {
                                Text(
                                    text = "${searchText} Fiyatları",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            // Başlık altında ürün sayısı
                            Text(
                                text = when (val currentState = uiState) {
                                    is MarketUiState.Success -> "${currentState.items.size} Ürün Listeleniyor"
                                    else -> "Ürünler Yükleniyor"
                                },
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                        
                        // Filtrele butonu
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                                .clickable { showSortDialog = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Filtrele",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Icon(
                                    Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Filtrele",
                                    modifier = Modifier.size(18.dp),
                                    tint = Color.Black
                                )
                            }
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
                        CircularProgressIndicator(color = PrimaryColor)
                    }
                }
                is MarketUiState.Success -> {
                    if (currentState.items.isEmpty() && searchText.isNotEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sonuç bulunamadı",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(currentState.items.size) { index ->
                                val item = currentState.items[index]
                                ProductCard(
                                    product = item,
                                    onClick = {
                                        viewModel.setSelectedProduct(item)
                                    },
                                    onAddToCart = { product ->
                                        shoppingListViewModel.addItemFromMarket(product)
                                    },
                                    productDetails = viewModel.productDetails.value,
                                    shoppingListViewModel = shoppingListViewModel
                                )

                                // Son öğeye yaklaşıldığında daha fazla ürün yükle
                                if (index >= currentState.items.size - 4) {
                                    LaunchedEffect(key1 = Unit) {
                                        Log.d("MarketScreen", "Son öğelere yaklaşıldı, daha fazla ürün yükleniyor")
                                        viewModel.loadMoreProducts()
                                    }
                                }
                            }
                        }
                    }
                }
                is MarketUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = currentState.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                            ElevatedButton(
                                onClick = { viewModel.searchProducts(searchText) },
                                colors = ButtonDefaults.elevatedButtonColors(
                                    containerColor = PrimaryColor
                                )
                            ) {
                                Text("Tekrar Dene")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: MarketItem,
    onClick: () -> Unit,
    onAddToCart: (MarketItem) -> Unit,
    productDetails: ProductDetailResponse? = null,
    shoppingListViewModel: ShoppingListViewModel
) {
    // Ürünün kendi satıcı sayısını al
    val offerCount = product.offer_count
    
    // Ürün miktarını takip etmek için state
    val activeListItems by shoppingListViewModel.activeListItems.collectAsState()
    val quantity = activeListItems.find { item -> 
        item.name == product.name && item.merchantId == product.merchant_id 
    }?.quantity?.toInt() ?: 0

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(278.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 6.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // Ürün resmi
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(
                            if (product.image.startsWith("/")) {
                                "http://10.0.2.2:8000${product.image}"
                            } else if (product.image.contains("file=")) {
                                val filename = product.image.substringAfter("file=").substringBefore("&")
                                "http://10.0.2.2:8000/image.php?file=${filename}&size=md"
                            } else {
                                "http://10.0.2.2:8000/image.php?file=${product.image}&size=md"
                            }
                        )
                        .crossfade(true)
                        .build(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .height(130.dp)
                        .fillMaxWidth(),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Mağaza bilgisi ve satıcı sayısı
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Mağaza logosu
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(
                                if (product.merchant_logo.startsWith("/")) {
                                    "http://10.0.2.2:8000${product.merchant_logo}"
                                } else {
                                    "http://10.0.2.2:8000/${product.merchant_logo}"
                                }
                            )
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .height(20.dp)
                            .width(48.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(0.5.dp, Color(0xFFEEEEEE), RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Fit
                    )
                    
                    // Satıcı sayısı - Görseldeki gibi belirgin şekilde
                    if (offerCount > 0) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+$offerCount satıcı",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrimaryColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryColor.copy(alpha = 0.1f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Ürün adı
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.height(40.dp)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Fiyat ve sepete ekle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Fiyat
                    Column {
                        Text(
                            text = "${product.price.formatWithDecimal()} TL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryColor
                        )
                        Text(
                            text = if (product.unit.isNotEmpty()) "${product.unit_price.formatWithDecimal()} TL/${product.unit}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    // Sepete ekle button veya miktar göstergesi
                    if (quantity > 0) {
                        // Eğer üründen sepette varsa miktar göster
                        QuantityControl(quantity, product, shoppingListViewModel)
                    } else {
                        // Sepette yoksa ekleme butonu göster
                        IconButton(
                            onClick = { onAddToCart(product) },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    PrimaryColor,
                                    shape = CircleShape
                                )
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
    }
}

@Composable
fun QuantityControl(
    quantity: Int,
    product: MarketItem,
    shoppingListViewModel: ShoppingListViewModel,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(PrimaryColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Eksiltme butonu
            Icon(
                Icons.Default.Delete,
                contentDescription = if (quantity == 1) "Sil" else "Azalt",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        shoppingListViewModel.decreaseItemQuantity(product)
                    }
            )
            
            // Miktar
            Text(
                text = quantity.toString(),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            // Artırma butonu
            Icon(
                Icons.Default.Add,
                contentDescription = "Artır",
                tint = Color.White,
                modifier = Modifier
                    .size(20.dp)
                    .clickable {
                        shoppingListViewModel.increaseItemQuantity(product)
                    }
            )
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

                                // Mağaza logosu (marka ve miktar arasında)
                                if (product.merchant_logo.isNotEmpty()) {
                                    val logoUrl = if (product.merchant_logo.startsWith("/")) {
                                        "http://10.0.2.2:8000${product.merchant_logo}"
                                    } else {
                                        "http://10.0.2.2:8000/${product.merchant_logo}"
                                    }
                                    
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(logoUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Mağaza logosu",
                                        modifier = Modifier
                                            .width(50.dp)
                                            .height(30.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(4.dp)),
                                        contentScale = ContentScale.Fit
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
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Fiyat bilgileri
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.Bottom
                                    ) {
                                        // Sol taraf: Birim fiyat
                                        Column {
                                            Text(
                                                text = "Birim Fiyat",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )

                                            Text(
                                                text = "${product.unit_price} ₺",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        // Sağ taraf: Toplam fiyat
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "Toplam",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.Gray
                                            )

                                            Text(
                                                text = "${product.price} ₺",
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryColor
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Sepete ekle butonu (fiyatların altında, genişletilmiş)
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
                                            .height(48.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.elevatedButtonColors(
                                            containerColor = PrimaryColor,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ShoppingCart,
                                                contentDescription = "Sepete Ekle",
                                                modifier = Modifier.size(20.dp)
                                            )
                                            
                                            Spacer(modifier = Modifier.width(8.dp))
                                            
                                            Text(
                                                text = "SEPETE EKLE",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
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
                    
                    // Fiyat tahmini butonu ekliyoruz
                    ElevatedButton(
                        onClick = { 
                            viewModel.predictFuturePrice() 
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = SecondaryColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Gelecek Fiyat Tahmini Yap",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Fiyat tahmini sonuçları
                    val pricePrediction by viewModel.pricePrediction.collectAsState()
                    val isLoadingPrediction by viewModel.isLoadingPricePrediction.collectAsState()
                    
                    if (isLoadingPrediction) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = SecondaryColor)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Fiyat tahminleri hesaplanıyor...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    pricePrediction?.let { prediction ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Fiyat Tahminleri",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                if (prediction.errorMessage != null) {
                                    // Hata mesajı
                                    Text(
                                        text = prediction.errorMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Red
                                    )
                                } else {
                                    // Tahmin sonuçları
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // 30 gün tahmini
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "30 Gün",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "${prediction.prediction30Days ?: "--"} ₺",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (prediction.prediction30Days != null && 
                                                            productDetails?.product?.offers?.firstOrNull()?.price ?: 0.0 < prediction.prediction30Days) {
                                                    Color.Red
                                                } else {
                                                    Color.Green
                                                }
                                            )
                                        }
                                        
                                        // 60 gün tahmini
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "60 Gün",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "${prediction.prediction60Days ?: "--"} ₺",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (prediction.prediction60Days != null && 
                                                            productDetails?.product?.offers?.firstOrNull()?.price ?: 0.0 < prediction.prediction60Days) {
                                                    Color.Red
                                                } else {
                                                    Color.Green
                                                }
                                            )
                                        }
                                        
                                        // 90 gün tahmini
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "90 Gün",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "${prediction.prediction90Days ?: "--"} ₺",
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (prediction.prediction90Days != null && 
                                                            productDetails?.product?.offers?.firstOrNull()?.price ?: 0.0 < prediction.prediction90Days) {
                                                    Color.Red
                                                } else {
                                                    Color.Green
                                                }
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    // Analiz
                                    if (prediction.analysis != null) {
                                        Text(
                                            text = "Analiz",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = prediction.analysis,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
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
                        .width(60.dp)
                        .height(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(6.dp)),
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

