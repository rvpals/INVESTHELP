package com.investhelp.app.ui.item

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.ui.components.ConfirmDeleteDialog
import com.investhelp.app.ui.settings.SettingsViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

private enum class SortOption(val label: String) {
    Ticker("Ticker"),
    TotalValue("Total Value"),
    CurrentPrice("Current Price")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    viewModel: ItemViewModel,
    onNavigateToItem: (String) -> Unit,
    onAddItem: () -> Unit
) {
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val refreshingTickers by viewModel.refreshingTickers.collectAsStateWithLifecycle()
    val isRefreshingAll by viewModel.isRefreshingAll.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val priceMessage by viewModel.priceMessage.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val context = LocalContext.current
    val warnBeforeDelete = remember {
        context.getSharedPreferences(SettingsViewModel.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getBoolean(SettingsViewModel.KEY_WARN_BEFORE_DELETE, true)
    }
    var deleteTarget by remember { mutableStateOf<InvestmentItemEntity?>(null) }
    var editingItem by remember { mutableStateOf<InvestmentItemEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var sortOption by rememberSaveable { mutableStateOf(SortOption.TotalValue) }

    val tabs = listOf("STOCK", "ETF")
    val filteredItems = when (selectedTab) {
        0 -> items.filter { it.type == InvestmentType.Stock }
        1 -> items.filter { it.type == InvestmentType.ETF }
        else -> items
    }

    val sortedItems = when (sortOption) {
        SortOption.Ticker -> filteredItems.sortedBy { it.ticker }
        SortOption.TotalValue -> filteredItems.sortedByDescending { it.value }
        SortOption.CurrentPrice -> filteredItems.sortedByDescending { it.currentPrice }
    }

    val tickerValues = filteredItems
        .filter { it.value > 0 }
        .map { it.ticker to it.value }
        .sortedByDescending { it.second }

    LaunchedEffect(priceMessage) {
        priceMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearPriceMessage()
        }
    }
    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    if (deleteTarget != null) {
        ConfirmDeleteDialog(
            title = "Delete Position",
            message = "Delete \"${deleteTarget!!.ticker}\"?",
            onConfirm = {
                viewModel.deleteItem(deleteTarget!!.ticker)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null }
        )
    }

    if (showForm || editingItem != null) {
        ItemFormDialog(
            existing = editingItem,
            items = items,
            warnBeforeDelete = warnBeforeDelete,
            onDismiss = {
                showForm = false
                editingItem = null
            },
            onSave = { ticker, quantity, cost, type ->
                viewModel.savePosition(ticker, quantity, cost, type)
                showForm = false
                editingItem = null
            },
            onDelete = { item ->
                editingItem = null
                showForm = false
                if (warnBeforeDelete) {
                    deleteTarget = item
                } else {
                    viewModel.deleteItem(item.ticker)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Items") },
                actions = {
                    TextButton(
                        onClick = { viewModel.refreshAllPositions() },
                        enabled = !isRefreshing && !isRefreshingAll
                    ) {
                        if (isRefreshing || isRefreshingAll) {
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
                        Text("Refresh All", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showForm = true }) {
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
                    modifier = Modifier.fillMaxSize(),
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
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (tickerValues.isNotEmpty()) {
                        item(key = "chart_section") {
                            Spacer(modifier = Modifier.height(8.dp))
                            ChartSection(tickerValues, currencyFormat)
                        }
                    }

                    item(key = "sort_bar") {
                        SortBar(
                            selected = sortOption,
                            onSelect = { sortOption = it }
                        )
                    }

                    item(key = "items_table") {
                        ItemsTable(
                            items = sortedItems,
                            refreshingTickers = refreshingTickers,
                            currencyFormat = currencyFormat,
                            onItemClick = onNavigateToItem,
                            onEdit = { editingItem = it }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TickerIcon3D(
    ticker: String,
    name: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hash = ticker.hashCode()
    val baseColor = chartColors[(hash and 0x7FFFFFFF) % chartColors.size]
    val gradient = Brush.linearGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.85f),
            baseColor,
            baseColor.copy(
                red = baseColor.red * 0.65f,
                green = baseColor.green * 0.65f,
                blue = baseColor.blue * 0.65f
            )
        )
    )

    Box(
        modifier = modifier
            .size(36.dp)
            .shadow(3.dp, RoundedCornerShape(10.dp))
            .background(gradient, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = (if (name != ticker) name else ticker).first().uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("https://companiesmarketcap.com/img/company-logos/64/${ticker}.webp")
                .crossfade(true)
                .build(),
            contentDescription = "$ticker logo",
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortBar(
    selected: SortOption,
    onSelect: (SortOption) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End
    ) {
        Text(
            text = "Sort by:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = selected.label,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .width(160.dp)
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                textStyle = MaterialTheme.typography.bodySmall,
                singleLine = true
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                SortOption.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemsTable(
    items: List<InvestmentItemEntity>,
    refreshingTickers: Set<String>,
    currencyFormat: NumberFormat,
    onItemClick: (String) -> Unit,
    onEdit: (InvestmentItemEntity) -> Unit
) {
    val sharesFormat = remember { DecimalFormat("#,##0.##") }
    val altRowColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column {
            items.forEachIndexed { index, item ->
                val gainColor = if (item.totalGainLoss >= 0)
                    Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                val dayColor = if (item.dayGainLoss >= 0)
                    Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                val rowBg = if (index % 2 == 1) altRowColor else Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(rowBg)
                        .clickable { onItemClick(item.ticker) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TickerIcon3D(ticker = item.ticker, name = item.name)
                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.ticker,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (item.name != item.ticker) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Normal,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = sharesFormat.format(item.quantity) + " shares",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = currencyFormat.format(item.totalGainLoss),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = gainColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Price: ${currencyFormat.format(item.currentPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Value: ${currencyFormat.format(item.value)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Day: ${currencyFormat.format(item.dayGainLoss)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = dayColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    if (item.ticker in refreshingTickers) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = { onEdit(item) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                if (index < items.lastIndex) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// --- Pie Chart ---

private val chartColors = listOf(
    Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC04), Color(0xFF34A853),
    Color(0xFFFF6D01), Color(0xFF46BDC6), Color(0xFF7B1FA2), Color(0xFFD81B60),
    Color(0xFF00897B), Color(0xFF5C6BC0), Color(0xFFFFA000), Color(0xFF8D6E63),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChartSection(
    tickerValues: List<Pair<String, Double>>,
    currencyFormat: NumberFormat
) {
    var expanded by remember { mutableStateOf(true) }
    val totalValue = tickerValues.sumOf { it.second }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Chart", style = MaterialTheme.typography.titleMedium)
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .aspectRatio(1f)
                    ) {
                        val diameter = size.minDimension
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )
                        var startAngle = -90f
                        tickerValues.forEachIndexed { index, (_, value) ->
                            val sweep = (value / totalValue * 360.0).toFloat()
                            drawArc(
                                color = chartColors[index % chartColors.size],
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = true,
                                topLeft = topLeft,
                                size = Size(diameter, diameter)
                            )
                            startAngle += sweep
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        tickerValues.forEachIndexed { index, (ticker, value) ->
                            val pct = if (totalValue > 0) value / totalValue * 100 else 0.0
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(chartColors[index % chartColors.size])
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$ticker ${"%.1f".format(pct)}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Total: ${currencyFormat.format(totalValue)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- Add/Edit Form Dialog ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemFormDialog(
    existing: InvestmentItemEntity?,
    items: List<InvestmentItemEntity>,
    warnBeforeDelete: Boolean = true,
    onDismiss: () -> Unit,
    onSave: (ticker: String, quantity: Double, cost: Double, type: InvestmentType) -> Unit,
    onDelete: (InvestmentItemEntity) -> Unit = {}
) {
    val distinctTickers = items.map { it.ticker }.toSet()

    var tickerInput by remember { mutableStateOf(existing?.ticker ?: "") }
    var quantity by remember { mutableStateOf(existing?.quantity?.toString() ?: "") }
    var cost by remember { mutableStateOf(existing?.cost?.toString() ?: "") }
    var selectedType by remember { mutableStateOf(existing?.type ?: InvestmentType.Stock) }
    var tickerExpanded by remember { mutableStateOf(false) }
    var typeExpanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isEditing = existing != null
    val filteredSuggestions = distinctTickers.filter {
        it.contains(tickerInput, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edit Item" else "Add Item") },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = tickerExpanded && !isEditing && filteredSuggestions.isNotEmpty(),
                    onExpandedChange = { if (!isEditing) tickerExpanded = it }
                ) {
                    OutlinedTextField(
                        value = tickerInput,
                        onValueChange = {
                            tickerInput = it.uppercase()
                            tickerExpanded = true
                            error = null
                        },
                        readOnly = isEditing,
                        label = { Text("Ticker") },
                        placeholder = { Text("e.g. AAPL, MSFT") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = tickerExpanded && !isEditing && filteredSuggestions.isNotEmpty(),
                        onDismissRequest = { tickerExpanded = false }
                    ) {
                        filteredSuggestions.forEach { ticker ->
                            DropdownMenuItem(
                                text = { Text(ticker) },
                                onClick = {
                                    tickerInput = ticker
                                    tickerExpanded = false
                                    error = null
                                    items.firstOrNull { it.ticker == ticker }?.type?.let {
                                        selectedType = it
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        InvestmentType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    selectedType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it; error = null },
                    label = { Text("Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = cost,
                    onValueChange = { cost = it; error = null },
                    label = { Text("Cost (USD)") },
                    singleLine = true,
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val ticker = tickerInput.trim().uppercase()
                val qty = quantity.toDoubleOrNull()
                val c = cost.toDoubleOrNull()
                when {
                    ticker.isBlank() -> error = "Enter a ticker"
                    !isEditing && ticker in distinctTickers -> error = "$ticker already exists"
                    qty == null || qty <= 0 -> error = "Enter a valid quantity"
                    c == null || c < 0 -> error = "Enter a valid cost"
                    else -> onSave(ticker, qty, c, selectedType)
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (isEditing && existing != null) {
                    TextButton(onClick = { onDelete(existing) }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}
