package com.investhelp.app.ui.item

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.investhelp.app.data.remote.AnalysisInfo
import com.investhelp.app.data.remote.HistoricalPrice
import com.investhelp.app.ui.components.CollapsibleCard
import com.investhelp.app.ui.components.DateRangeSelector
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
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val priceHistory by viewModel.priceHistory.collectAsStateWithLifecycle()
    val isLoadingPriceHistory by viewModel.isLoadingPriceHistory.collectAsStateWithLifecycle()
    val priceHistoryError by viewModel.priceHistoryError.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    val watchListViewModel: WatchListViewModel = hiltViewModel()
    val watchLists by watchListViewModel.watchLists.collectAsStateWithLifecycle()
    var showAddToWatchList by remember { mutableStateOf(false) }
    var showCreateWatchList by remember { mutableStateOf(false) }
    var newWatchListName by remember { mutableStateOf("") }

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var statsExpanded by remember { mutableStateOf(false) }
    var transactionsExpanded by remember { mutableStateOf(false) }
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
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
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
            }

            when (selectedTab) {
                0 -> ItemDetailContent(
                    ticker = ticker,
                    item = item,
                    transactions = transactions,
                    analysisInfo = analysisInfo,
                    isLoadingAnalysis = isLoadingAnalysis,
                    analysisError = analysisError,
                    statistics = statistics,
                    currencyFormat = currencyFormat,
                    dateFormatter = dateFormatter,
                    statsExpanded = statsExpanded,
                    onStatsExpandedChange = { statsExpanded = it },
                    transactionsExpanded = transactionsExpanded,
                    onTransactionsExpandedChange = { transactionsExpanded = it },
                    statsStartDate = statsStartDate,
                    onStatsStartDateChange = { statsStartDate = it },
                    statsEndDate = statsEndDate,
                    onStatsEndDateChange = { statsEndDate = it },
                    onSimulate = onSimulate,
                    onAddToWatchList = { showAddToWatchList = true }
                )
                1 -> PriceHistoryTab(
                    ticker = ticker,
                    viewModel = viewModel,
                    priceHistory = priceHistory,
                    isLoading = isLoadingPriceHistory,
                    error = priceHistoryError,
                    currencyFormat = currencyFormat
                )
            }
        }
    }
}

@Composable
private fun ItemDetailContent(
    ticker: String,
    item: com.investhelp.app.data.local.entity.InvestmentItemEntity?,
    transactions: List<com.investhelp.app.data.local.entity.InvestmentTransactionEntity>,
    analysisInfo: AnalysisInfo?,
    isLoadingAnalysis: Boolean,
    analysisError: String?,
    statistics: com.investhelp.app.model.ItemStatistics,
    currencyFormat: NumberFormat,
    dateFormatter: DateTimeFormatter,
    statsExpanded: Boolean,
    onStatsExpandedChange: (Boolean) -> Unit,
    transactionsExpanded: Boolean,
    onTransactionsExpandedChange: (Boolean) -> Unit,
    statsStartDate: LocalDate,
    onStatsStartDateChange: (LocalDate) -> Unit,
    statsEndDate: LocalDate,
    onStatsEndDateChange: (LocalDate) -> Unit,
    onSimulate: (ticker: String, shares: Double) -> Unit,
    onAddToWatchList: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header card with item info
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
                        // Row 1: Total Shares, Total Value, Total Cost, Total G/L (big font)
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
                        // Row 2: Daily G/L, Daily G/L per Share, Daily Min, Daily Max (medium font)
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

        // Analysis Info collapsible panel
        item {
            Spacer(modifier = Modifier.height(4.dp))
            CollapsibleCard(
                title = "Analysis Info",
                pinned = false,
                onPinToggle = {}
            ) {
                if (isLoadingAnalysis) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        strokeWidth = 2.dp
                    )
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
                        currencyFormat = currencyFormat
                    )
                }
            }
        }

        // Yahoo Finance + Simulate buttons
        item {
            item?.let { inv ->
                Spacer(modifier = Modifier.height(4.dp))

                val context = LocalContext.current
                OutlinedButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://finance.yahoo.com/quote/$ticker")
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Yahoo Finance")
                }

                Button(
                    onClick = { onSimulate(ticker, inv.quantity) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Simulate")
                }

                OutlinedButton(
                    onClick = onAddToWatchList,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.PlaylistAdd,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Add to Watch List")
                }
            }
        }

        // Collapsible Stats section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStatsExpandedChange(!statsExpanded) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$ticker Stats",
                    style = MaterialTheme.typography.titleLarge
                )
                Icon(
                    if (statsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (statsExpanded) "Collapse" else "Expand"
                )
            }

            AnimatedVisibility(visible = statsExpanded) {
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
                }
            }
        }

        // Collapsible Transactions section
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTransactionsExpandedChange(!transactionsExpanded) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Transactions",
                    style = MaterialTheme.typography.titleLarge
                )
                Icon(
                    if (transactionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (transactionsExpanded) "Collapse" else "Expand"
                )
            }
        }

        if (transactionsExpanded) {
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
    val hourlyIntervals = listOf("Every Hour" to "1h", "30 Minutes" to "30m", "10 Minutes" to "10m", "5 Minutes" to "5m")
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
                        hourlyIntervals.forEach { (label, value) ->
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
private fun AnalysisInfoContent(
    ticker: String,
    info: AnalysisInfo,
    currencyFormat: NumberFormat
) {
    val percentFormat = NumberFormat.getPercentInstance(Locale.US).apply {
        maximumFractionDigits = 2
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

        info.marketCap?.let { InfoRow("Market Cap", formatMarketCap(it)) }
        info.trailingPE?.let { InfoRow("Trailing P/E", "%.2f".format(it)) }
        info.forwardPE?.let { InfoRow("Forward P/E", "%.2f".format(it)) }
        info.eps?.let { InfoRow("EPS", currencyFormat.format(it)) }
        info.dividendYield?.let { InfoRow("Dividend Yield", percentFormat.format(it)) }

        Spacer(modifier = Modifier.height(12.dp))

        Text("Price Range", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        info.fiftyTwoWeekHigh?.let { InfoRow("52-Week High", currencyFormat.format(it)) }
        info.fiftyTwoWeekLow?.let { InfoRow("52-Week Low", currencyFormat.format(it)) }
        info.fiftyDayAverage?.let { InfoRow("50-Day Avg", currencyFormat.format(it)) }
        info.twoHundredDayAverage?.let { InfoRow("200-Day Avg", currencyFormat.format(it)) }

        Spacer(modifier = Modifier.height(12.dp))

        if (info.targetMeanPrice != null || info.revenuePerShare != null ||
            info.profitMargins != null || info.returnOnEquity != null
        ) {
            Text("Financials", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            info.targetMeanPrice?.let { InfoRow("Analyst Target", currencyFormat.format(it)) }
            info.revenuePerShare?.let { InfoRow("Revenue/Share", currencyFormat.format(it)) }
            info.profitMargins?.let { InfoRow("Profit Margins", percentFormat.format(it)) }
            info.returnOnEquity?.let { InfoRow("Return on Equity", percentFormat.format(it)) }
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
