package com.investhelp.app.ui.watchlist

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.ui.components.ConfirmDeleteDialog
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WatchListScreen(
    viewModel: WatchListViewModel
) {
    val watchLists by viewModel.watchLists.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedWatchListId.collectAsStateWithLifecycle()
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val fetchedPrice by viewModel.fetchedPrice.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val sharesFormat = DecimalFormat("#,##0.##")
    val priceFormat = DecimalFormat("#,##0.00")
    val dateFormat = DateTimeFormatter.ofPattern("MM/dd/yy")

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var showAddItemDialog by rememberSaveable { mutableStateOf(false) }
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var watchListToDelete by remember { mutableStateOf<Long?>(null) }
    var itemToDelete by remember { mutableStateOf<WatchListItemUi?>(null) }

    LaunchedEffect(watchLists, selectedId) {
        if (selectedId == null && watchLists.isNotEmpty()) {
            viewModel.selectWatchList(watchLists.first().id)
        }
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "chips") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(watchLists, key = { it.id }) { wl ->
                            FilterChip(
                                selected = selectedId == wl.id,
                                onClick = { viewModel.selectWatchList(wl.id) },
                                label = { Text(wl.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "New list")
                    }
                }
            }

            val selectedWatchList = watchLists.find { it.id == selectedId }

            if (selectedWatchList != null) {
                item(key = "actions") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedWatchList.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showRenameDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { watchListToDelete = selectedWatchList.id }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete list", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { viewModel.refreshPrices() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh prices", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                item(key = "add_btn") {
                    OutlinedButton(
                        onClick = { showAddItemDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Ticker")
                    }
                }

                if (isLoading && items.isEmpty()) {
                    item(key = "loading") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                        }
                    }
                } else if (items.isNotEmpty()) {
                    item(key = "table") {
                        WatchListTable(
                            items = items,
                            currencyFormat = currencyFormat,
                            sharesFormat = sharesFormat,
                            priceFormat = priceFormat,
                            dateFormat = dateFormat,
                            onDelete = { itemToDelete = it }
                        )
                    }
                } else {
                    item(key = "empty") {
                        Text(
                            text = "No tickers in this watch list. Tap \"Add Ticker\" to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                }
            } else if (watchLists.isEmpty()) {
                item(key = "no_lists") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No watch lists yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { showCreateDialog = true }) {
                            Text("Create Watch List")
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    if (showCreateDialog) {
        TextInputDialog(
            title = "New Watch List",
            label = "Name",
            onConfirm = { name ->
                showCreateDialog = false
                if (name.isNotBlank()) viewModel.createWatchList(name)
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    if (showRenameDialog) {
        val wl = watchLists.find { it.id == selectedId }
        if (wl != null) {
            TextInputDialog(
                title = "Rename Watch List",
                label = "Name",
                initialValue = wl.name,
                onConfirm = { name ->
                    showRenameDialog = false
                    if (name.isNotBlank()) viewModel.renameWatchList(wl, name)
                },
                onDismiss = { showRenameDialog = false }
            )
        }
    }

    watchListToDelete?.let { id ->
        val wl = watchLists.find { it.id == id }
        if (wl != null) {
            if (viewModel.warnBeforeDelete()) {
                ConfirmDeleteDialog(
                    title = "Delete Watch List",
                    message = "Delete \"${wl.name}\" and all its items?",
                    onConfirm = { viewModel.deleteWatchList(wl); watchListToDelete = null },
                    onDismiss = { watchListToDelete = null }
                )
            } else {
                LaunchedEffect(id) {
                    viewModel.deleteWatchList(wl)
                    watchListToDelete = null
                }
            }
        } else {
            watchListToDelete = null
        }
    }

    itemToDelete?.let { item ->
        if (viewModel.warnBeforeDelete()) {
            ConfirmDeleteDialog(
                title = "Remove Ticker",
                message = "Remove ${item.entity.ticker} from this watch list?",
                onConfirm = { viewModel.deleteItem(item.entity); itemToDelete = null },
                onDismiss = { itemToDelete = null }
            )
        } else {
            LaunchedEffect(item) {
                viewModel.deleteItem(item.entity)
                itemToDelete = null
            }
        }
    }

    if (showAddItemDialog && selectedId != null) {
        AddTickerDialog(
            fetchedPrice = fetchedPrice,
            onFetchPrice = { viewModel.fetchPrice(it) },
            onConfirm = { ticker, shares, price ->
                showAddItemDialog = false
                viewModel.clearFetchedPrice()
                viewModel.addItem(selectedId!!, ticker, shares, price)
            },
            onDismiss = { showAddItemDialog = false; viewModel.clearFetchedPrice() }
        )
    }
}

@Composable
private fun WatchListTable(
    items: List<WatchListItemUi>,
    currencyFormat: NumberFormat,
    sharesFormat: DecimalFormat,
    priceFormat: DecimalFormat,
    dateFormat: DateTimeFormatter,
    onDelete: (WatchListItemUi) -> Unit
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val horizontalScroll = rememberScrollState()

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .padding(4.dp)
        ) {
            HorizontalDivider(color = dividerColor)
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCell("Ticker", 80)
                VerticalDivider(color = dividerColor)
                HeaderCell("Shares", 65, TextAlign.End)
                VerticalDivider(color = dividerColor)
                HeaderCell("Price", 80, TextAlign.End)
                VerticalDivider(color = dividerColor)
                HeaderCell("Added @", 80, TextAlign.End)
                VerticalDivider(color = dividerColor)
                HeaderCell("Change $", 90, TextAlign.End)
                VerticalDivider(color = dividerColor)
                HeaderCell("Change %", 80, TextAlign.End)
                VerticalDivider(color = dividerColor)
                HeaderCell("Date", 70, TextAlign.Center)
                VerticalDivider(color = dividerColor)
                Spacer(modifier = Modifier.width(36.dp))
            }
            HorizontalDivider(thickness = 2.dp, color = dividerColor)

            items.forEach { item ->
                val changeColor = if (item.changeAmount >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                val sign = if (item.changeAmount > 0) "+" else ""

                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.entity.ticker,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(80.dp).padding(start = 4.dp)
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = sharesFormat.format(item.entity.shares),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(65.dp),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = priceFormat.format(item.currentPrice),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = priceFormat.format(item.entity.priceWhenAdded),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = "$sign${currencyFormat.format(item.changeAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = changeColor,
                        modifier = Modifier.width(90.dp),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = "$sign${String.format("%.2f", item.changePercent)}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = changeColor,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = item.entity.addedDate.format(dateFormat),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(70.dp),
                        textAlign = TextAlign.Center
                    )
                    VerticalDivider(color = dividerColor)
                    IconButton(
                        onClick = { onDelete(item) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                HorizontalDivider(color = dividerColor)
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, widthDp: Int, align: TextAlign = TextAlign.Start) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .width(widthDp.dp)
            .padding(horizontal = 4.dp),
        textAlign = align
    )
}

@Composable
private fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by rememberSaveable { mutableStateOf(initialValue) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun AddTickerDialog(
    fetchedPrice: Double?,
    onFetchPrice: (String) -> Unit,
    onConfirm: (ticker: String, shares: Double, price: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var ticker by rememberSaveable { mutableStateOf("") }
    var shares by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(fetchedPrice) {
        if (fetchedPrice != null) {
            price = String.format("%.2f", fetchedPrice)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Ticker") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = ticker,
                        onValueChange = { ticker = it.uppercase() },
                        label = { Text("Ticker") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = { if (ticker.isNotBlank()) onFetchPrice(ticker.trim()) },
                        enabled = ticker.isNotBlank()
                    ) {
                        Text("Fetch", style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedTextField(
                    value = shares,
                    onValueChange = { shares = it },
                    label = { Text("# of Shares") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price When Added") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val s = shares.toDoubleOrNull() ?: return@TextButton
                    val p = price.toDoubleOrNull() ?: return@TextButton
                    if (ticker.isNotBlank()) onConfirm(ticker, s, p)
                },
                enabled = ticker.isNotBlank()
                        && shares.toDoubleOrNull() != null
                        && price.toDoubleOrNull() != null
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
