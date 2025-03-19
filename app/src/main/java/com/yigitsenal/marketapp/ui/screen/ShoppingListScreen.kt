package com.yigitsenal.marketapp.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yigitsenal.marketapp.R
import com.yigitsenal.marketapp.data.model.ShoppingList
import com.yigitsenal.marketapp.data.model.ShoppingListItem
import com.yigitsenal.marketapp.ui.theme.PrimaryColor
import com.yigitsenal.marketapp.ui.viewmodel.ShoppingListViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext


@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel,
    onNavigateToMarket: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeList by viewModel.activeShoppingList.collectAsState()
    val items by viewModel.activeListItems.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedItems by viewModel.selectedItems.collectAsState()
    
    // Toplam maliyet hesaplama
    val totalCost = items.sumOf { it.price }
    
    // Silme işlemi için onay iletişim kutusu
    var showDeleteDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<ShoppingListItem?>(null) }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false 
                itemToDelete = null
                viewModel.clearSelection()
            },
            title = { Text(text = if (isSelectionMode) "Seçili Ürünleri Sil" else "Ürünü Sil") },
            text = { 
                Text(
                    text = if (isSelectionMode) {
                        "${selectedItems.size} ürünü silmek istediğinize emin misiniz?"
                    } else {
                        "\"${itemToDelete?.name}\" ürününü silmek istediğinize emin misiniz?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSelectionMode) {
                            viewModel.deleteSelectedItems()
                        } else {
                            itemToDelete?.let {
                                viewModel.deleteItem(it)
                            }
                        }
                        showDeleteDialog = false
                        itemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("Sil")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        itemToDelete = null
                        viewModel.clearSelection()
                    }
                ) {
                    Text("İptal")
                }
            }
        )
    }
    
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            ShoppingListTopBar(
                activeList = activeList,
                items = items,
                isSelectionMode = isSelectionMode,
                selectedItemCount = selectedItems.size,
                onClearSelection = { viewModel.clearSelection() },
                onDeleteSelected = { showDeleteDialog = true }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToMarket() },
                containerColor = PrimaryColor
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Ürün Ekle",
                    tint = Color.White
                )
            }
        },
        bottomBar = {
            if (items.isNotEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Toplam Tutar:",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "₺${String.format("%.2f", totalCost)}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (items.isEmpty()) {
                EmptyListView()
            } else {
                ShoppingListContent(
                    items = items,
                    selectedItems = selectedItems,
                    isSelectionMode = isSelectionMode,
                    onItemClick = { 
                        if (isSelectionMode) {
                            viewModel.toggleItemSelection(it)
                        } else {
                            viewModel.toggleItemCompletion(it)
                        }
                    },
                    onItemLongClick = {
                        viewModel.toggleItemSelection(it)
                    },
                    onItemDelete = { 
                        itemToDelete = it
                        showDeleteDialog = true
                    }
                )
            }
        }
    }
}

@Composable
fun ShoppingListTopBar(
    activeList: ShoppingList?,
    items: List<ShoppingListItem>,
    isSelectionMode: Boolean,
    selectedItemCount: Int,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp
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
                if (isSelectionMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClearSelection) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Seçimi İptal Et"
                            )
                        }
                        Text(
                            text = "$selectedItemCount öğe seçildi",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onDeleteSelected) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Seçilenleri Sil",
                                tint = Color.Red
                            )
                        }
                    }
                } else {
                    Column {
                        Text(
                            text = activeList?.name ?: "Alışveriş Listesi",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = activeList?.getFormattedDate() ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    if (items.isNotEmpty()) {
                        val completedItems = items.count { it.isCompleted }
                        val progress = completedItems.toFloat() / items.size
                        
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    progress = progress,
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$completedItems/${items.size}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyListView(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Alışveriş listeniz boş",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Ürün eklemek için + butonuna tıklayın",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ShoppingListContent(
    items: List<ShoppingListItem>,
    selectedItems: Set<ShoppingListItem>,
    isSelectionMode: Boolean,
    onItemClick: (ShoppingListItem) -> Unit,
    onItemLongClick: (ShoppingListItem) -> Unit,
    onItemDelete: (ShoppingListItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        contentPadding = PaddingValues(8.dp)
    ) {
        items(items) { item ->
            ShoppingListItemRow(
                item = item,
                isSelected = item in selectedItems,
                isSelectionMode = isSelectionMode,
                onItemClick = { onItemClick(item) },
                onItemLongClick = { onItemLongClick(item) },
                onDeleteClick = { onItemDelete(item) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShoppingListItemRow(
    item: ShoppingListItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> Color(0xFFE3F2FD)
                item.isCompleted -> Color(0xFFF5F5F5)
                else -> Color.White
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onItemClick,
                    onLongClick = onItemLongClick
                )
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol taraf: Checkbox, ürün görseli ve bilgileri
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onItemClick() },
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }

                // Ürün görseli
                Card(
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(item.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(id = R.drawable.placeholder_image),
                        error = painterResource(id = R.drawable.placeholder_image)
                    )
                }

                // Ürün bilgileri
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ürün adı
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.isCompleted) Color.Gray else Color.Black,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Miktar ve mağaza logosu
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Miktar gösterimi
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${item.quantity.toInt()} adet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Mağaza logosu
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            modifier = Modifier
                                .width(74.dp)
                                .height(49.dp)
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(item.merchantLogo)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(45.dp)
                                    .clip(RoundedCornerShape(8.dp)),

                                contentScale = ContentScale.Fit,
                                placeholder = painterResource(id = R.drawable.placeholder_image),
                                error = painterResource(id = R.drawable.placeholder_image)
                            )
                        }
                    }
                }
            }

            // Sağ taraf: Fiyat ve silme butonu
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fiyat
                Text(
                    text = "₺${String.format("%.2f", item.price)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (item.isCompleted) Color.Gray else PrimaryColor,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                // Silme butonu
                if (!isSelectionMode) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Sil",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
} 