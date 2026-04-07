package com.investhelp.app.ui.item

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.ui.components.ConfirmDeleteDialog
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    viewModel: ItemViewModel,
    onNavigateToItem: (Long) -> Unit,
    onAddItem: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToTransfers: () -> Unit
) {
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val refreshingIds by viewModel.refreshingItemIds.collectAsStateWithLifecycle()
    val isRefreshingAll by viewModel.isRefreshingAll.collectAsStateWithLifecycle()
    val isUpdatingAll by viewModel.isUpdatingAll.collectAsStateWithLifecycle()
    val priceMessage by viewModel.priceMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val sharesFormat = remember { DecimalFormat("#,##0.##") }
    var itemToDelete by remember { mutableStateOf<Long?>(null) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val tabs = listOf("STOCK", "ETF")
    val filteredItems = when (selectedTab) {
        0 -> items.filter { it.type == InvestmentType.Stock }
        1 -> items.filter { it.type == InvestmentType.ETF }
        else -> items
    }

    LaunchedEffect(priceMessage) {
        priceMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPriceMessage()
        }
    }

    val deleteTarget = itemToDelete?.let { id -> items.find { it.id == id } }
    if (deleteTarget != null) {
        ConfirmDeleteDialog(
            title = "Delete Investment Item",
            message = "Delete \"${deleteTarget.name}\"? All associated transactions will also be deleted.",
            onConfirm = {
                viewModel.deleteItem(deleteTarget)
                itemToDelete = null
            },
            onDismiss = { itemToDelete = null }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Investment Items") },
                actions = {
                    IconButton(onClick = onNavigateToTransfers) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = "Transfers"
                        )
                    }
                    IconButton(onClick = onNavigateToTransactions) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = "Transactions"
                        )
                    }
                    TextButton(
                        onClick = { viewModel.updateAllItems() },
                        enabled = !isUpdatingAll && !isRefreshingAll
                    ) {
                        if (isUpdatingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            "Update All",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    TextButton(
                        onClick = { viewModel.refreshAllPrices() },
                        enabled = !isRefreshingAll && !isUpdatingAll
                    ) {
                        if (isRefreshingAll) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            "Refresh Price",
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddItem) {
                Icon(Icons.Default.Add, contentDescription = "Add Item")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            if (filteredItems.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No ${tabs[selectedTab].lowercase()} items yet", style = MaterialTheme.typography.bodyLarge)
                    Text("Tap + to add one", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredItems, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            isRefreshing = item.id in refreshingIds,
                            currencyFormat = currencyFormat,
                            sharesFormat = sharesFormat,
                            onItemClick = { onNavigateToItem(item.id) },
                            onRefresh = { viewModel.refreshPrice(item) },
                            onDelete = { itemToDelete = item.id }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(
    item: InvestmentItemEntity,
    isRefreshing: Boolean,
    currencyFormat: NumberFormat,
    sharesFormat: DecimalFormat,
    onItemClick: () -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    if (!item.ticker.isNullOrBlank()) {
                        Text(
                            text = item.ticker,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = currencyFormat.format(item.currentPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (item.numShares > 0) {
                        Text(
                            text = "${sharesFormat.format(item.numShares)} shares",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing && !item.ticker.isNullOrBlank()
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh Price",
                        tint = if (!item.ticker.isNullOrBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
