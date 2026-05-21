package com.investhelp.app.ui.item

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.nio.ByteBuffer
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.hilt.navigation.compose.hiltViewModel
import com.investhelp.app.data.local.entity.DefinitionEntity
import com.investhelp.app.data.remote.AnalysisInfo
import com.investhelp.app.data.remote.HistoricalPrice
import com.investhelp.app.ui.components.ConfirmDeleteDialog
import com.investhelp.app.ui.components.DateRangeSelector
import com.investhelp.app.ui.settings.SettingsViewModel
import com.investhelp.app.ui.watchlist.WatchListViewModel
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    ticker: String,
    viewModel: ItemViewModel,
    onEditItem: () -> Unit,
    onSimulate: (ticker: String, shares: Double) -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(ticker) {
        viewModel.loadItem(ticker)
    }

    val item by viewModel.selectedItem.collectAsStateWithLifecycle()
    val transactions by viewModel.itemTransactions.collectAsStateWithLifecycle()
    val analysisInfo by viewModel.analysisInfo.collectAsStateWithLifecycle()
    val isLoadingAnalysis by viewModel.isLoadingAnalysis.collectAsStateWithLifecycle()
    val analysisError by viewModel.analysisError.collectAsStateWithLifecycle()
    val definitions by viewModel.definitions.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val priceHistory by viewModel.priceHistory.collectAsStateWithLifecycle()
    val isLoadingPriceHistory by viewModel.isLoadingPriceHistory.collectAsStateWithLifecycle()
    val priceHistoryError by viewModel.priceHistoryError.collectAsStateWithLifecycle()
    val investingPerformance by viewModel.investingPerformance.collectAsStateWithLifecycle()
    val isLoadingInvestingPerf by viewModel.isLoadingInvestingPerf.collectAsStateWithLifecycle()
    val investingPerfError by viewModel.investingPerfError.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    val watchListViewModel: WatchListViewModel = hiltViewModel()
    val watchLists by watchListViewModel.watchLists.collectAsStateWithLifecycle()
    var showAddToWatchList by remember { mutableStateOf(false) }
    var showCreateWatchList by remember { mutableStateOf(false) }
    var newWatchListName by remember { mutableStateOf("") }

    val context = LocalContext.current
    val warnBeforeDelete = remember {
        context.getSharedPreferences(SettingsViewModel.PREFS_NAME, android.content.Context.MODE_PRIVATE)
            .getBoolean(SettingsViewModel.KEY_WARN_BEFORE_DELETE, true)
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var statsStartDate by remember { mutableStateOf(LocalDate.now().minusYears(1)) }
    var statsEndDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(ticker) {
        viewModel.fetchAnalysisInfo(ticker)
    }

    LaunchedEffect(ticker, statsStartDate, statsEndDate) {
        viewModel.loadStatistics(ticker, statsStartDate, statsEndDate)
    }

    if (showAddToWatchList) {
        AlertDialog(
            onDismissRequest = { showAddToWatchList = false },
            title = { Text("Add $ticker to Watch List") },
            text = {
                Column {
                    if (watchLists.isEmpty()) {
                        Text(
                            "No watch lists yet. Create one to get started.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    } else {
                        watchLists.forEach { wl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val price = item?.currentPrice ?: 0.0
                                        val shares = item?.quantity ?: 0.0
                                        watchListViewModel.addItem(
                                            watchListId = wl.id,
                                            ticker = ticker,
                                            shares = shares,
                                            priceWhenAdded = price
                                        )
                                        showAddToWatchList = false
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(wl.name, style = MaterialTheme.typography.bodyLarge)
                            }
                            HorizontalDivider()
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showAddToWatchList = false
                                showCreateWatchList = true
                            }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Create New Watch List",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showAddToWatchList = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCreateWatchList) {
        AlertDialog(
            onDismissRequest = {
                showCreateWatchList = false
                newWatchListName = ""
            },
            title = { Text("New Watch List") },
            text = {
                OutlinedTextField(
                    value = newWatchListName,
                    onValueChange = { newWatchListName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val name = newWatchListName.trim()
                        if (name.isNotEmpty()) {
                            watchListViewModel.createWatchList(name)
                            showCreateWatchList = false
                            newWatchListName = ""
                            showAddToWatchList = true
                        }
                    },
                    enabled = newWatchListName.trim().isNotEmpty()
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateWatchList = false
                    newWatchListName = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showDeleteConfirm) {
        ConfirmDeleteDialog(
            title = "Delete $ticker",
            message = "Are you sure you want to delete $ticker and all its data? This action cannot be undone.",
            onConfirm = {
                showDeleteConfirm = false
                viewModel.deleteItem(ticker)
                onBack()
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.name ?: ticker) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditItem) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = {
                        if (warnBeforeDelete) {
                            showDeleteConfirm = true
                        } else {
                            viewModel.deleteItem(ticker)
                            onBack()
                        }
                    }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    VerticalDivider(
                        modifier = Modifier
                            .height(24.dp)
                            .padding(horizontal = 4.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    val context = LocalContext.current
                    IconButton(onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://finance.yahoo.com/quote/$ticker")
                        )
                        context.startActivity(intent)
                    }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Yahoo Finance")
                    }
                    IconButton(onClick = { onSimulate(ticker, item?.quantity ?: 0.0) }) {
                        Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = "Simulate")
                    }
                    IconButton(onClick = { showAddToWatchList = true }) {
                        Icon(Icons.Default.PlaylistAdd, contentDescription = "Add to Watch List")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 0.dp) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Details") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Price History") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Analysis Info") }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Transactions") }
                )
            }

            when (selectedTab) {
                0 -> ItemDetailContent(
                    ticker = ticker,
                    item = item,
                    currencyFormat = currencyFormat
                )
                1 -> PriceHistoryTab(
                    ticker = ticker,
                    viewModel = viewModel,
                    priceHistory = priceHistory,
                    isLoading = isLoadingPriceHistory,
                    error = priceHistoryError,
                    currencyFormat = currencyFormat
                )
                2 -> AnalysisInfoTab(
                    ticker = ticker,
                    analysisInfo = analysisInfo,
                    isLoadingAnalysis = isLoadingAnalysis,
                    analysisError = analysisError,
                    currencyFormat = currencyFormat,
                    definitions = definitions
                )
                3 -> TransactionDetailsTab(
                    ticker = ticker,
                    item = item,
                    transactions = transactions,
                    statistics = statistics,
                    currencyFormat = currencyFormat,
                    dateFormatter = dateFormatter,
                    statsStartDate = statsStartDate,
                    onStatsStartDateChange = { statsStartDate = it },
                    statsEndDate = statsEndDate,
                    onStatsEndDateChange = { statsEndDate = it },
                    viewModel = viewModel,
                    investingPerformance = investingPerformance,
                    isLoadingInvestingPerf = isLoadingInvestingPerf,
                    investingPerfError = investingPerfError
                )
            }
        }
    }
}

@Composable
private fun ItemDetailContent(
    ticker: String,
    item: com.investhelp.app.data.local.entity.InvestmentItemEntity?,
    currencyFormat: NumberFormat
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            item?.let { inv ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DetailTickerIcon(ticker = inv.ticker, name = inv.name, logo = inv.logo)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = inv.ticker,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                if (inv.name != inv.ticker) {
                                    Text(
                                        text = inv.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            AssistChip(
                                onClick = {},
                                label = { Text(inv.type.name) }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Current Price: ${currencyFormat.format(inv.currentPrice)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Shares", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    "%.4f".format(inv.quantity),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Value", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(inv.value),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total Cost", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(inv.cost),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total G/L", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(inv.totalGainLoss),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (inv.totalGainLoss >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        val dailyChangePerShare = if (inv.quantity > 0) inv.dayGainLoss / inv.quantity else 0.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily G/L", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(inv.dayGainLoss),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (inv.dayGainLoss >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily G/L/Share", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(dailyChangePerShare),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (dailyChangePerShare >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily Min", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(inv.dayLow),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Daily Max", style = MaterialTheme.typography.labelSmall)
                                Text(
                                    currencyFormat.format(inv.dayHigh),
                                    style = MaterialTheme.typography.bodyMedium
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
private fun TransactionDetailsTab(
    ticker: String,
    item: com.investhelp.app.data.local.entity.InvestmentItemEntity?,
    transactions: List<com.investhelp.app.data.local.entity.InvestmentTransactionEntity>,
    statistics: com.investhelp.app.model.ItemStatistics,
    currencyFormat: NumberFormat,
    dateFormatter: DateTimeFormatter,
    statsStartDate: LocalDate,
    onStatsStartDateChange: (LocalDate) -> Unit,
    statsEndDate: LocalDate,
    onStatsEndDateChange: (LocalDate) -> Unit,
    viewModel: ItemViewModel,
    investingPerformance: List<ItemViewModel.InvestingPerfPoint>,
    isLoadingInvestingPerf: Boolean,
    investingPerfError: String?
) {
    var expanded by remember { mutableStateOf(true) }
    var perfExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(ticker) {
        viewModel.loadInvestingPerformance(ticker)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Transactions & Stats",
                    style = MaterialTheme.typography.titleLarge
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
        }

        if (expanded) {
            // Stats section
            item {
                Column {
                    DateRangeSelector(
                        startDate = statsStartDate,
                        endDate = statsEndDate,
                        onStartDateChanged = { onStatsStartDateChange(it) },
                        onEndDateChanged = { onStatsEndDateChange(it) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Buy Statistics", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Average",
                            value = statistics.avgBuyPrice?.let { currencyFormat.format(it) } ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Max",
                            value = statistics.maxBuyPrice?.let { currencyFormat.format(it) } ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Min",
                            value = statistics.minBuyPrice?.let { currencyFormat.format(it) } ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Sell Statistics", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "Average",
                            value = statistics.avgSellPrice?.let { currencyFormat.format(it) } ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Max",
                            value = statistics.maxSellPrice?.let { currencyFormat.format(it) } ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "Min",
                            value = statistics.minSellPrice?.let { currencyFormat.format(it) } ?: "N/A",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Transactions (${transactions.size})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (transactions.isEmpty()) {
                item {
                    Text(
                        "No transactions for this item",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(transactions, key = { it.id }) { transaction ->
                val daysSince = ChronoUnit.DAYS.between(transaction.date, LocalDate.now())
                val currentPrice = item?.currentPrice ?: 0.0
                val gl = (currentPrice - transaction.pricePerShare) * transaction.numberOfShares
                val glColor = if (gl >= 0) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = transaction.action.name,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (transaction.action.name == "Buy")
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${transaction.date.format(dateFormatter)}  (${daysSince}d)",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${transaction.numberOfShares} shares @ ${currencyFormat.format(transaction.pricePerShare)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "G/L: ${currencyFormat.format(gl)}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = glColor
                            )
                        }
                    }
                }
            }
        }

        // Investing Performance panel
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { perfExpanded = !perfExpanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Investing Performance for $ticker",
                    style = MaterialTheme.typography.titleLarge
                )
                Icon(
                    if (perfExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (perfExpanded) "Collapse" else "Expand"
                )
            }
        }

        if (perfExpanded) {
            item {
                if (isLoadingInvestingPerf) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                } else if (investingPerfError != null) {
                    Text(
                        text = investingPerfError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(16.dp)
                    )
                } else if (investingPerformance.isNotEmpty()) {
                    InvestingPerformanceChart(
                        points = investingPerformance,
                        currencyFormat = currencyFormat
                    )
                } else {
                    Text(
                        "No transaction data to show performance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (!isLoadingInvestingPerf && investingPerformance.isNotEmpty()) {
                item {
                    InvestingPerformanceTable(
                        points = investingPerformance,
                        currencyFormat = currencyFormat,
                        dateFormatter = dateFormatter
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun InvestingPerformanceChart(
    points: List<ItemViewModel.InvestingPerfPoint>,
    currencyFormat: NumberFormat
) {
    if (points.size < 2) return

    val lineColor = MaterialTheme.colorScheme.outline
    val txColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipText = MaterialTheme.colorScheme.inverseOnSurface

    val prices = remember(points) { points.map { it.price } }
    val minPrice = remember(prices) { prices.min() }
    val maxPrice = remember(prices) { prices.max() }
    val priceRange = remember(minPrice, maxPrice) { (maxPrice - minPrice).coerceAtLeast(0.01) }

    var selectedIdx by remember { mutableStateOf<Int?>(null) }

    val dateFormat = remember { DateTimeFormatter.ofPattern("MMM dd") }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(start = 48.dp, end = 8.dp, top = 8.dp, bottom = 24.dp)
                .pointerInput(points) {
                    detectTapGestures(
                        onTap = { offset ->
                            val chartWidth = size.width.toFloat()
                            val spacing = chartWidth / (points.size - 1).coerceAtLeast(1)
                            val idx = ((offset.x + spacing / 2) / spacing).toInt()
                                .coerceIn(0, points.size - 1)
                            selectedIdx = if (selectedIdx == idx) null else idx
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartWidth = size.width
                val chartHeight = size.height
                val spacing = chartWidth / (points.size - 1).coerceAtLeast(1)

                // Y-axis grid lines and labels
                for (i in 0..3) {
                    val y = chartHeight * i / 3f
                    drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), 1f)
                    val price = maxPrice - (priceRange * i / 3)
                    drawContext.canvas.nativeCanvas.drawText(
                        currencyFormat.format(price),
                        -44.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = labelColor.hashCode()
                            textSize = 9.dp.toPx()
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }

                // Draw line path connecting all points
                val path = Path()
                for (i in points.indices) {
                    val x = i * spacing
                    val y = chartHeight * (1f - ((prices[i] - minPrice) / priceRange).toFloat())
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, lineColor, style = Stroke(2.dp.toPx()))

                // Draw data points
                for (i in points.indices) {
                    val x = i * spacing
                    val y = chartHeight * (1f - ((prices[i] - minPrice) / priceRange).toFloat())
                    if (points[i].isTransaction) {
                        drawCircle(txColor, 7.dp.toPx(), Offset(x, y))
                        drawCircle(Color.White, 4.dp.toPx(), Offset(x, y))
                        drawCircle(txColor, 3.dp.toPx(), Offset(x, y))
                    } else {
                        drawCircle(lineColor, 4.dp.toPx(), Offset(x, y))
                    }
                }

                // Selected point indicator
                selectedIdx?.let { idx ->
                    val x = idx * spacing
                    val y = chartHeight * (1f - ((prices[idx] - minPrice) / priceRange).toFloat())
                    drawLine(gridColor, Offset(x, 0f), Offset(x, chartHeight), 1f)
                    drawCircle(
                        if (points[idx].isTransaction) txColor else lineColor,
                        8.dp.toPx(), Offset(x, y), style = Stroke(2.dp.toPx())
                    )
                }

                // X-axis labels
                val labelCount = minOf(points.size, 5)
                for (i in 0 until labelCount) {
                    val dataIdx = (i * (points.size - 1) / (labelCount - 1).coerceAtLeast(1))
                        .coerceIn(0, points.size - 1)
                    val x = dataIdx * spacing
                    if (x in 0f..chartWidth) {
                        drawContext.canvas.nativeCanvas.drawText(
                            points[dataIdx].date.format(dateFormat),
                            x,
                            chartHeight + 14.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = labelColor.hashCode()
                                textSize = 9.dp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }

            // Tooltip
            selectedIdx?.let { idx ->
                val pt = points[idx]
                val label = "${currencyFormat.format(pt.price)} — ${pt.date.format(dateFormat)}${if (pt.isTransaction) " (TX)" else ""}"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .background(tooltipBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = tooltipText,
                        fontWeight = if (pt.isTransaction) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun InvestingPerformanceTable(
    points: List<ItemViewModel.InvestingPerfPoint>,
    currencyFormat: NumberFormat,
    dateFormatter: DateTimeFormatter
) {
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    val txBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(4.dp)
        ) {
            HorizontalDivider(color = dividerColor)
            // Header
            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(36.dp).padding(start = 4.dp),
                    textAlign = TextAlign.Center
                )
                VerticalDivider(color = dividerColor)
                Text(
                    text = "Date",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(100.dp).padding(horizontal = 4.dp)
                )
                VerticalDivider(color = dividerColor)
                Text(
                    text = "Price",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(90.dp).padding(horizontal = 4.dp),
                    textAlign = TextAlign.End
                )
                VerticalDivider(color = dividerColor)
                Text(
                    text = "Type",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.width(70.dp).padding(horizontal = 4.dp),
                    textAlign = TextAlign.Center
                )
            }
            HorizontalDivider(thickness = 2.dp, color = dividerColor)

            points.forEachIndexed { index, pt ->
                val bgMod = if (pt.isTransaction) Modifier.background(txBgColor) else Modifier
                Row(
                    modifier = bgMod
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.width(36.dp).padding(start = 4.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = pt.date.format(dateFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (pt.isTransaction) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(100.dp).padding(horizontal = 4.dp)
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = currencyFormat.format(pt.price),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (pt.isTransaction) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(90.dp).padding(horizontal = 4.dp),
                        textAlign = TextAlign.End,
                        color = if (pt.isTransaction) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = if (pt.isTransaction) "BUY/SELL" else "Market",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (pt.isTransaction) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.width(70.dp).padding(horizontal = 4.dp),
                        textAlign = TextAlign.Center,
                        color = if (pt.isTransaction) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                HorizontalDivider(color = dividerColor)
            }
        }
    }
}

@Composable
private fun PriceHistoryTab(
    ticker: String,
    viewModel: ItemViewModel,
    priceHistory: List<HistoricalPrice>,
    isLoading: Boolean,
    error: String?,
    currencyFormat: NumberFormat
) {
    val timeframes = listOf("Hourly", "Daily", "Monthly", "Yearly")
    var selectedTimeframe by rememberSaveable { mutableStateOf("Daily") }
    val hourlyIntervals = listOf("Every Hour" to "1h", "30 Minutes" to "30m", "15 Minutes" to "15m", "5 Minutes" to "5m", "1 Minute" to "1m")
    var selectedHourlyInterval by rememberSaveable { mutableStateOf("1h") }
    val priceFormat = remember { NumberFormat.getCurrencyInstance(Locale.US) }

    LaunchedEffect(ticker, selectedTimeframe, selectedHourlyInterval) {
        viewModel.loadPriceHistory(ticker, selectedTimeframe, selectedHourlyInterval)
    }

    val dateTimeFormat = remember(selectedTimeframe) {
        when (selectedTimeframe) {
            "Hourly" -> DateTimeFormatter.ofPattern("HH:mm")
            "Daily" -> DateTimeFormatter.ofPattern("MMM dd, yyyy")
            "Monthly" -> DateTimeFormatter.ofPattern("MMM yyyy")
            "Yearly" -> DateTimeFormatter.ofPattern("MMM yyyy")
            else -> DateTimeFormatter.ofPattern("MMM dd, yyyy")
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("Timeframe", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                timeframes.forEach { tf ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { selectedTimeframe = tf }
                    ) {
                        RadioButton(
                            selected = selectedTimeframe == tf,
                            onClick = { selectedTimeframe = tf }
                        )
                        Text(tf, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(visible = selectedTimeframe == "Hourly") {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Interval", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        hourlyIntervals.take(3).forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { selectedHourlyInterval = value }
                            ) {
                                RadioButton(
                                    selected = selectedHourlyInterval == value,
                                    onClick = { selectedHourlyInterval = value }
                                )
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        hourlyIntervals.drop(3).forEach { (label, value) ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { selectedHourlyInterval = value }
                            ) {
                                RadioButton(
                                    selected = selectedHourlyInterval == value,
                                    onClick = { selectedHourlyInterval = value }
                                )
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
            Text(
                text = when (selectedTimeframe) {
                    "Hourly" -> "Today's market hours (${hourlyIntervals.first { it.second == selectedHourlyInterval }.first})"
                    "Daily" -> "Last 60 days (1d interval)"
                    "Monthly" -> "Last 13 months (1mo interval)"
                    "Yearly" -> "Last 15 years (1mo interval)"
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }

        if (isLoading) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }
        } else if (error != null) {
            item {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else if (priceHistory.isNotEmpty()) {
            // Summary: Average, Max, Min
            item {
                val prices = priceHistory.map { it.close }
                val avg = prices.average()
                val max = prices.max()
                val min = prices.min()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "Average",
                        value = currencyFormat.format(avg),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Max",
                        value = currencyFormat.format(max),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Min",
                        value = currencyFormat.format(min),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Line chart
            item {
                Spacer(modifier = Modifier.height(8.dp))
                PriceLineChart(
                    priceHistory = priceHistory,
                    selectedTimeframe = selectedTimeframe,
                    currencyFormat = currencyFormat
                )
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${priceHistory.size} records",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Price history table
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(4.dp)
                    ) {
                        val dividerColor = MaterialTheme.colorScheme.outlineVariant
                        HorizontalDivider(color = dividerColor)
                        Row(
                            modifier = Modifier
                                .height(IntrinsicSize.Min)
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(40.dp).padding(start = 4.dp),
                                textAlign = TextAlign.Center
                            )
                            VerticalDivider(color = dividerColor)
                            Text(
                                text = if (selectedTimeframe == "Hourly") "Time" else "Date",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(110.dp).padding(horizontal = 4.dp)
                            )
                            VerticalDivider(color = dividerColor)
                            Text(
                                text = "Price",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(100.dp).padding(horizontal = 4.dp),
                                textAlign = TextAlign.End
                            )
                        }
                        HorizontalDivider(thickness = 2.dp, color = dividerColor)

                        priceHistory.forEachIndexed { index, hp ->
                            val dateTime = Instant.ofEpochSecond(hp.timestamp)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDateTime()
                            Row(
                                modifier = Modifier
                                    .height(IntrinsicSize.Min)
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(40.dp).padding(start = 4.dp),
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                VerticalDivider(color = dividerColor)
                                Text(
                                    text = dateTime.format(dateTimeFormat),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(110.dp).padding(horizontal = 4.dp)
                                )
                                VerticalDivider(color = dividerColor)
                                Text(
                                    text = priceFormat.format(hp.close),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.width(100.dp).padding(horizontal = 4.dp),
                                    textAlign = TextAlign.End
                                )
                            }
                            HorizontalDivider(color = dividerColor)
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    "No price history available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun AnalysisInfoTab(
    ticker: String,
    analysisInfo: AnalysisInfo?,
    isLoadingAnalysis: Boolean,
    analysisError: String?,
    currencyFormat: NumberFormat,
    definitions: List<DefinitionEntity>
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            if (isLoadingAnalysis) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            } else if (analysisError != null) {
                Text(
                    text = analysisError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(16.dp)
                )
            } else if (analysisInfo != null) {
                AnalysisInfoContent(
                    ticker = ticker,
                    info = analysisInfo,
                    currencyFormat = currencyFormat,
                    definitions = definitions
                )
            } else {
                Text(
                    "No analysis info available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun PriceLineChart(
    priceHistory: List<HistoricalPrice>,
    selectedTimeframe: String,
    currencyFormat: NumberFormat
) {
    if (priceHistory.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipText = MaterialTheme.colorScheme.inverseOnSurface

    var zoom by remember { mutableStateOf(1f) }
    var scrollOffset by remember { mutableStateOf(0f) }
    var selectedIdx by remember { mutableStateOf<Int?>(null) }
    var chartWidthPx by remember { mutableStateOf(0f) }

    val dateTimeFormat = remember(selectedTimeframe) {
        when (selectedTimeframe) {
            "Hourly" -> DateTimeFormatter.ofPattern("HH:mm")
            "Daily" -> DateTimeFormatter.ofPattern("MMM dd")
            "Monthly" -> DateTimeFormatter.ofPattern("MMM yy")
            "Yearly" -> DateTimeFormatter.ofPattern("MMM yy")
            else -> DateTimeFormatter.ofPattern("MMM dd")
        }
    }

    val prices = remember(priceHistory) { priceHistory.map { it.close } }
    val minPrice = remember(prices) { prices.min() }
    val maxPrice = remember(prices) { prices.max() }
    val priceRange = remember(minPrice, maxPrice) {
        (maxPrice - minPrice).coerceAtLeast(0.01)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(start = 48.dp, end = 8.dp, top = 8.dp, bottom = 24.dp)
                .pointerInput(priceHistory) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(1f, 5f)
                        val maxScroll = chartWidthPx * (zoom - 1f)
                        scrollOffset = (scrollOffset - pan.x).coerceIn(0f, maxScroll)
                        selectedIdx = null
                    }
                }
                .pointerInput(priceHistory) {
                    detectTapGestures(
                        onTap = { offset ->
                            val virtualWidth = chartWidthPx * zoom
                            val virtualX = offset.x + scrollOffset
                            val pointCount = priceHistory.size
                            val spacing = virtualWidth / (pointCount - 1).coerceAtLeast(1)
                            val idx = ((virtualX + spacing / 2) / spacing).toInt()
                                .coerceIn(0, pointCount - 1)
                            selectedIdx = if (selectedIdx == idx) null else idx
                        },
                        onDoubleTap = {
                            zoom = 1f
                            scrollOffset = 0f
                            selectedIdx = null
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartWidth = size.width
                val chartHeight = size.height
                chartWidthPx = chartWidth
                val virtualWidth = chartWidth * zoom
                val pointCount = priceHistory.size
                val spacing = virtualWidth / (pointCount - 1).coerceAtLeast(1)

                // Y-axis grid lines and labels
                for (i in 0..3) {
                    val y = chartHeight * i / 3f
                    drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), 1f)
                    val price = maxPrice - (priceRange * i / 3)
                    drawContext.canvas.nativeCanvas.drawText(
                        currencyFormat.format(price),
                        -44.dp.toPx(),
                        y + 4.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = labelColor.hashCode()
                            textSize = 9.dp.toPx()
                            textAlign = android.graphics.Paint.Align.LEFT
                        }
                    )
                }

                clipRect(0f, 0f, chartWidth, chartHeight) {
                    // Draw line path
                    val path = Path()
                    for (i in priceHistory.indices) {
                        val x = i * spacing - scrollOffset
                        val y = chartHeight * (1f - ((prices[i] - minPrice) / priceRange).toFloat())
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, lineColor, style = Stroke(2.dp.toPx()))

                    // Fill under curve
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo((pointCount - 1) * spacing - scrollOffset, chartHeight)
                        lineTo(0f * spacing - scrollOffset, chartHeight)
                        close()
                    }
                    drawPath(fillPath, lineColor.copy(alpha = 0.1f))

                    // Selected point
                    selectedIdx?.let { idx ->
                        val x = idx * spacing - scrollOffset
                        val y = chartHeight * (1f - ((prices[idx] - minPrice) / priceRange).toFloat())
                        if (x in 0f..chartWidth) {
                            drawCircle(lineColor, 5.dp.toPx(), Offset(x, y))
                            drawLine(gridColor, Offset(x, 0f), Offset(x, chartHeight), 1f)
                        }
                    }
                }

                // X-axis labels
                val labelCount = 4
                for (i in 0 until labelCount) {
                    val dataIdx = (i * (pointCount - 1) / (labelCount - 1).coerceAtLeast(1))
                        .coerceIn(0, pointCount - 1)
                    val x = dataIdx * spacing - scrollOffset
                    if (x in 0f..chartWidth) {
                        val dt = Instant.ofEpochSecond(priceHistory[dataIdx].timestamp)
                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
                        drawContext.canvas.nativeCanvas.drawText(
                            dt.format(dateTimeFormat),
                            x,
                            chartHeight + 14.dp.toPx(),
                            android.graphics.Paint().apply {
                                color = labelColor.hashCode()
                                textSize = 9.dp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }

            // Tooltip
            selectedIdx?.let { idx ->
                val dt = Instant.ofEpochSecond(priceHistory[idx].timestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDateTime()
                val tooltipLabel = "${currencyFormat.format(prices[idx])} — ${dt.format(dateTimeFormat)}"
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .background(tooltipBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tooltipLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = tooltipText
                    )
                }
            }
        }
    }
}

@Composable
private fun AnalysisInfoContent(
    ticker: String,
    info: AnalysisInfo,
    currencyFormat: NumberFormat,
    definitions: List<DefinitionEntity>
) {
    val percentFormat = NumberFormat.getPercentInstance(Locale.US).apply {
        maximumFractionDigits = 2
    }
    val definitionMap = remember(definitions) {
        definitions.associateBy { it.name.lowercase() }
    }
    var showDefinitionPopup by remember { mutableStateOf<DefinitionEntity?>(null) }

    if (showDefinitionPopup != null) {
        AlertDialog(
            onDismissRequest = { showDefinitionPopup = null },
            title = { Text(showDefinitionPopup!!.name) },
            text = {
                Text(
                    text = showDefinitionPopup!!.description,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(onClick = { showDefinitionPopup = null }) {
                    Text("OK")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Text(
            text = info.shortName ?: ticker,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (info.sector != null || info.industry != null) {
            Text(
                text = listOfNotNull(info.sector, info.industry).joinToString(" - "),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Key Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        info.marketCap?.let { InfoRow("Market Cap", formatMarketCap(it), definitionMap) { showDefinitionPopup = it } }
        info.trailingPE?.let { InfoRow("Trailing P/E", "%.2f".format(it), definitionMap) { showDefinitionPopup = it } }
        info.forwardPE?.let { InfoRow("Forward P/E", "%.2f".format(it), definitionMap) { showDefinitionPopup = it } }
        info.eps?.let { InfoRow("EPS", currencyFormat.format(it), definitionMap) { showDefinitionPopup = it } }
        info.dividendYield?.let { InfoRow("Dividend Yield", percentFormat.format(it), definitionMap) { showDefinitionPopup = it } }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Price Range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        info.fiftyTwoWeekHigh?.let { InfoRow("52-Week High", currencyFormat.format(it), definitionMap) { showDefinitionPopup = it } }
        info.fiftyTwoWeekLow?.let { InfoRow("52-Week Low", currencyFormat.format(it), definitionMap) { showDefinitionPopup = it } }
        info.fiftyDayAverage?.let { InfoRow("50-Day Avg", currencyFormat.format(it), definitionMap) { showDefinitionPopup = it } }
        info.twoHundredDayAverage?.let { InfoRow("200-Day Avg", currencyFormat.format(it), definitionMap) { showDefinitionPopup = it } }

        Spacer(modifier = Modifier.height(12.dp))

        if (info.targetMeanPrice != null || info.revenuePerShare != null ||
            info.profitMargins != null || info.returnOnEquity != null
        ) {
            Text("Financials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            info.targetMeanPrice?.let { InfoRow("Analyst Target", currencyFormat.format(it), definitionMap) { showDefinitionPopup = it } }
            info.revenuePerShare?.let { InfoRow("Revenue/Share", currencyFormat.format(it), definitionMap) { showDefinitionPopup = it } }
            info.profitMargins?.let { InfoRow("Profit Margins", percentFormat.format(it), definitionMap) { showDefinitionPopup = it } }
            info.returnOnEquity?.let { InfoRow("Return on Equity", percentFormat.format(it), definitionMap) { showDefinitionPopup = it } }
        }

        if (!info.longBusinessSummary.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = info.longBusinessSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    definitionMap: Map<String, DefinitionEntity>,
    onShowDefinition: (DefinitionEntity) -> Unit
) {
    val definition = definitionMap[label.lowercase()]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (definition != null) {
            TextButton(
                onClick = { onShowDefinition(definition) },
                modifier = Modifier.padding(0.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatMarketCap(value: Long): String {
    return when {
        value >= 1_000_000_000_000 -> "${"%.2f".format(value / 1_000_000_000_000.0)}T"
        value >= 1_000_000_000 -> "${"%.2f".format(value / 1_000_000_000.0)}B"
        value >= 1_000_000 -> "${"%.2f".format(value / 1_000_000.0)}M"
        else -> NumberFormat.getNumberInstance(Locale.US).format(value)
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

private val detailIconColors = listOf(
    Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC04), Color(0xFF34A853),
    Color(0xFFFF6D01), Color(0xFF46BDC6), Color(0xFF7B1FA2), Color(0xFFD81B60),
    Color(0xFF00897B), Color(0xFF5C6BC0), Color(0xFFFFA000), Color(0xFF8D6E63),
)

@Composable
private fun DetailTickerIcon(
    ticker: String,
    name: String,
    logo: ByteArray? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val hash = ticker.hashCode()
    val baseColor = detailIconColors[(hash and 0x7FFFFFFF) % detailIconColors.size]
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
            .size(48.dp)
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .background(gradient, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = (if (name != ticker) name else ticker).first().uppercase(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        val imageData: Any = if (logo != null) ByteBuffer.wrap(logo) else "https://companiesmarketcap.com/img/company-logos/64/${ticker.lowercase()}.webp"
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageData)
                .crossfade(true)
                .build(),
            contentDescription = "$ticker logo",
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    }
}
