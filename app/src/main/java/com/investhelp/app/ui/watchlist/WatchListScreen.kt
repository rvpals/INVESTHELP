package com.investhelp.app.ui.watchlist

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
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
import com.investhelp.app.data.local.entity.WatchListEntity
import com.investhelp.app.data.local.entity.WatchListItemEntity
import com.investhelp.app.ui.components.ConfirmDeleteDialog
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun WatchListScreen(
    viewModel: WatchListViewModel,
    onNavigateToItem: (String) -> Unit = {}
) {
    val watchLists by viewModel.watchLists.collectAsStateWithLifecycle()
    val itemsByWatchList by viewModel.itemsByWatchList.collectAsStateWithLifecycle()
    val fetchedPrice by viewModel.fetchedPrice.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val sharesFormat = DecimalFormat("#,##0.##")
    val priceFormat = DecimalFormat("#,##0.00")
    val dateFormat = DateTimeFormatter.ofPattern("MM/dd/yy")

    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var addItemWatchListId by remember { mutableStateOf<Long?>(null) }
    var renameWatchList by remember { mutableStateOf<WatchListEntity?>(null) }
    var watchListToDelete by remember { mutableStateOf<Long?>(null) }
    var itemToDelete by remember { mutableStateOf<WatchListItemUi?>(null) }
    var itemForReminder by remember { mutableStateOf<WatchListItemEntity?>(null) }

    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    LaunchedEffect(watchLists) {
        watchLists.forEach { wl ->
            viewModel.loadItemsForWatchList(wl.id)
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
            item(key = "header") {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Watch Lists",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        IconButton(onClick = { viewModel.refreshAllWatchListPrices() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh all")
                        }
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New list")
                        }
                    }
                }
            }

            if (watchLists.isEmpty()) {
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

            items(watchLists, key = { it.id }) { wl ->
                val wlItems = itemsByWatchList[wl.id] ?: emptyList()
                WatchListPanel(
                    watchList = wl,
                    items = wlItems,
                    currencyFormat = currencyFormat,
                    sharesFormat = sharesFormat,
                    priceFormat = priceFormat,
                    dateFormat = dateFormat,
                    onAddTicker = { addItemWatchListId = wl.id },
                    onRename = { renameWatchList = wl },
                    onDelete = { watchListToDelete = wl.id },
                    onDeleteItem = { itemToDelete = it },
                    onReminder = { itemForReminder = it.entity },
                    onTickerClick = { onNavigateToItem(it) }
                )
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

    renameWatchList?.let { wl ->
        TextInputDialog(
            title = "Rename Watch List",
            label = "Name",
            initialValue = wl.name,
            onConfirm = { name ->
                renameWatchList = null
                if (name.isNotBlank()) viewModel.renameWatchList(wl, name)
            },
            onDismiss = { renameWatchList = null }
        )
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

    addItemWatchListId?.let { wlId ->
        AddTickerDialog(
            fetchedPrice = fetchedPrice,
            onFetchPrice = { viewModel.fetchPrice(it) },
            onConfirm = { ticker, shares, price, reminderDt, reminderMsg ->
                addItemWatchListId = null
                viewModel.clearFetchedPrice()
                if (reminderDt != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
                viewModel.addItem(wlId, ticker, shares, price, reminderDt, reminderMsg)
            },
            onDismiss = { addItemWatchListId = null; viewModel.clearFetchedPrice() }
        )
    }

    itemForReminder?.let { item ->
        ReminderDialog(
            ticker = item.ticker,
            initialDateTime = item.reminderDateTime,
            initialMessage = item.reminderMessage,
            onConfirm = { dateTime, message ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                viewModel.updateItemReminder(item, dateTime, message)
                itemForReminder = null
            },
            onClear = {
                viewModel.updateItemReminder(item, null, null)
                itemForReminder = null
            },
            onDismiss = { itemForReminder = null }
        )
    }
}

@Composable
private fun WatchListPanel(
    watchList: WatchListEntity,
    items: List<WatchListItemUi>,
    currencyFormat: NumberFormat,
    sharesFormat: DecimalFormat,
    priceFormat: DecimalFormat,
    dateFormat: DateTimeFormatter,
    onAddTicker: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDeleteItem: (WatchListItemUi) -> Unit,
    onReminder: (WatchListItemUi) -> Unit,
    onTickerClick: (String) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = watchList.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${items.size} items",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onAddTicker,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Ticker", style = MaterialTheme.typography.labelSmall)
                        }
                        IconButton(onClick = onRename, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = "Rename", modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (items.isNotEmpty()) {
                        WatchListTable(
                            items = items,
                            currencyFormat = currencyFormat,
                            sharesFormat = sharesFormat,
                            priceFormat = priceFormat,
                            dateFormat = dateFormat,
                            onDelete = onDeleteItem,
                            onReminder = onReminder,
                            onTickerClick = onTickerClick
                        )
                    } else {
                        Text(
                            text = "No tickers yet. Tap \"Add Ticker\" to get started.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchListTable(
    items: List<WatchListItemUi>,
    currencyFormat: NumberFormat,
    sharesFormat: DecimalFormat,
    priceFormat: DecimalFormat,
    dateFormat: DateTimeFormatter,
    onDelete: (WatchListItemUi) -> Unit,
    onReminder: (WatchListItemUi) -> Unit,
    onTickerClick: (String) -> Unit
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
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .width(80.dp)
                            .padding(start = 4.dp)
                            .clickable { onTickerClick(item.entity.ticker) }
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
                    VerticalDivider(color = dividerColor)
                    IconButton(
                        onClick = { onReminder(item) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Reminder",
                            tint = if (item.entity.reminderDateTime != null)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTickerDialog(
    fetchedPrice: Double?,
    onFetchPrice: (String) -> Unit,
    onConfirm: (ticker: String, shares: Double, price: Double, reminderDateTime: LocalDateTime?, reminderMessage: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var ticker by rememberSaveable { mutableStateOf("") }
    var shares by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var hasReminder by rememberSaveable { mutableStateOf(false) }
    var reminderMessage by rememberSaveable { mutableStateOf("") }
    var reminderDate by remember { mutableStateOf(LocalDate.now().plusDays(1)) }
    var reminderTime by remember { mutableStateOf(LocalTime.of(9, 0)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    LaunchedEffect(fetchedPrice) {
        if (fetchedPrice != null) {
            price = String.format("%.2f", fetchedPrice)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Ticker") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
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

                HorizontalDivider()

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = hasReminder,
                        onCheckedChange = { hasReminder = it }
                    )
                    Text("Set Reminder", style = MaterialTheme.typography.bodyMedium)
                }

                if (hasReminder) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(reminderDate.format(dateFormatter), style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { showTimePicker = true }
                        ) {
                            Text(reminderTime.format(timeFormatter), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    OutlinedTextField(
                        value = reminderMessage,
                        onValueChange = { reminderMessage = it },
                        label = { Text("Reminder Message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val s = shares.toDoubleOrNull() ?: return@TextButton
                    val p = price.toDoubleOrNull() ?: return@TextButton
                    if (ticker.isNotBlank()) {
                        val dt = if (hasReminder && reminderMessage.isNotBlank())
                            LocalDateTime.of(reminderDate, reminderTime) else null
                        val msg = if (hasReminder && reminderMessage.isNotBlank())
                            reminderMessage.trim() else null
                        onConfirm(ticker, s, p, dt, msg)
                    }
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = reminderDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        reminderDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderTime.hour,
            initialMinute = reminderTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    reminderTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDialog(
    ticker: String,
    initialDateTime: LocalDateTime?,
    initialMessage: String?,
    onConfirm: (LocalDateTime, String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    var reminderDate by remember {
        mutableStateOf(initialDateTime?.toLocalDate() ?: LocalDate.now().plusDays(1))
    }
    var reminderTime by remember {
        mutableStateOf(initialDateTime?.toLocalTime() ?: LocalTime.of(9, 0))
    }
    var message by rememberSaveable { mutableStateOf(initialMessage ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reminder: $ticker") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(reminderDate.format(dateFormatter), style = MaterialTheme.typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true }
                    ) {
                        Text(reminderTime.format(timeFormatter), style = MaterialTheme.typography.labelSmall)
                    }
                }
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (message.isNotBlank()) {
                        onConfirm(LocalDateTime.of(reminderDate, reminderTime), message.trim())
                    }
                },
                enabled = message.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (initialDateTime != null) {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = reminderDate.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        reminderDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = reminderTime.hour,
            initialMinute = reminderTime.minute
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    reminderTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}
