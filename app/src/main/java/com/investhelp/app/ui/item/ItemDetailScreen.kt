package com.investhelp.app.ui.item

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.remote.AnalysisInfo
import com.investhelp.app.ui.components.DateRangeSelector
import java.text.NumberFormat
import java.time.LocalDate
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

    val itemRows by viewModel.selectedItemRows.collectAsStateWithLifecycle()
    val transactions by viewModel.itemTransactions.collectAsStateWithLifecycle()
    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
    val analysisInfo by viewModel.analysisInfo.collectAsStateWithLifecycle()
    val isLoadingAnalysis by viewModel.isLoadingAnalysis.collectAsStateWithLifecycle()
    val analysisError by viewModel.analysisError.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    val accountMap = accounts.associateBy { it.id }
    val firstRow = itemRows.firstOrNull()
    val totalQuantity = itemRows.sumOf { it.quantity }
    val totalValue = itemRows.sumOf { it.value }
    val totalCost = itemRows.sumOf { it.cost }
    val totalGainLoss = itemRows.sumOf { it.totalGainLoss }
    val totalDayGainLoss = itemRows.sumOf { it.dayGainLoss }

    var showAnalysisSheet by remember { mutableStateOf(false) }
    var statsExpanded by remember { mutableStateOf(false) }
    var transactionsExpanded by remember { mutableStateOf(false) }
    var statsStartDate by remember { mutableStateOf(LocalDate.now().minusYears(1)) }
    var statsEndDate by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(ticker, statsStartDate, statsEndDate) {
        viewModel.loadStatistics(ticker, statsStartDate, statsEndDate)
    }

    LaunchedEffect(analysisInfo) {
        if (analysisInfo != null) showAnalysisSheet = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(firstRow?.name ?: ticker) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header card with aggregate info
            item {
                firstRow?.let { inv ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(inv.type.name) }
                                )
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
                                        "%.4f".format(totalQuantity),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Total Value", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        currencyFormat.format(totalValue),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Total Cost", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        currencyFormat.format(totalCost),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Total G/L", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        currencyFormat.format(totalGainLoss),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (totalGainLoss >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            // Row 2: Daily G/L, Daily G/L per Share, Daily Min, Daily Max (medium font)
                            val dailyChangePerShare = if (totalQuantity > 0) totalDayGainLoss / totalQuantity else 0.0
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Daily G/L", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        currencyFormat.format(totalDayGainLoss),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (totalDayGainLoss >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
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

            // Per-account breakdown
            if (itemRows.size > 1) {
                item {
                    Text("Per Account", style = MaterialTheme.typography.titleMedium)
                }
                items(itemRows, key = { "${it.ticker}:${it.accountId}" }) { row ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = accountMap[row.accountId]?.name ?: "Account ${row.accountId}",
                                style = MaterialTheme.typography.labelLarge
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column {
                                    Text("Qty", style = MaterialTheme.typography.labelSmall)
                                    Text("%.4f".format(row.quantity), style = MaterialTheme.typography.bodySmall)
                                }
                                Column {
                                    Text("Value", style = MaterialTheme.typography.labelSmall)
                                    Text(currencyFormat.format(row.value), style = MaterialTheme.typography.bodySmall)
                                }
                                Column {
                                    Text("Cost", style = MaterialTheme.typography.labelSmall)
                                    Text(currencyFormat.format(row.cost), style = MaterialTheme.typography.bodySmall)
                                }
                                Column {
                                    Text("G/L", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        currencyFormat.format(row.totalGainLoss),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (row.totalGainLoss >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Action buttons row: Analysis Info + Yahoo Finance
            item {
                firstRow?.let { inv ->
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val context = LocalContext.current
                        Button(
                            onClick = { viewModel.fetchAnalysisInfo(ticker) },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoadingAnalysis
                        ) {
                            if (isLoadingAnalysis) {
                                CircularProgressIndicator(
                                    modifier = Modifier.padding(end = 8.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                            Text("Analysis Info")
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://finance.yahoo.com/quote/$ticker")
                                )
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Yahoo Finance")
                        }
                    }

                    if (analysisError != null) {
                        Text(
                            text = analysisError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Button(
                        onClick = { onSimulate(ticker, totalQuantity) },
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
                }
            }

            // Collapsible Stats section
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { statsExpanded = !statsExpanded }
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
                            onStartDateChanged = { statsStartDate = it },
                            onEndDateChanged = { statsEndDate = it }
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
                        .clickable { transactionsExpanded = !transactionsExpanded }
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
                                    text = transaction.date.format(dateFormatter),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${transaction.numberOfShares} shares",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "@ ${currencyFormat.format(transaction.pricePerShare)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Analysis Info Bottom Sheet
    if (showAnalysisSheet && analysisInfo != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showAnalysisSheet = false
                viewModel.clearAnalysisInfo()
            },
            sheetState = sheetState
        ) {
            AnalysisInfoContent(
                ticker = ticker,
                info = analysisInfo!!,
                currencyFormat = currencyFormat
            )
        }
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
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
            .verticalScroll(rememberScrollState())
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
