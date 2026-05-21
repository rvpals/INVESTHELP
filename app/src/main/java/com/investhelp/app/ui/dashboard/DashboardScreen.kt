package com.investhelp.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.local.entity.ChangeHistoryEntity
import com.investhelp.app.ui.components.CollapsibleCard
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToItem: (String) -> Unit = {},
    onNavigateToWatchList: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pinStates by viewModel.pinStates.collectAsStateWithLifecycle()
    val changeHistory by viewModel.changeHistoryRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val lastRefreshedAt by viewModel.lastRefreshedAt.collectAsStateWithLifecycle()
    val watchListCardVisible by viewModel.watchListCardVisible.collectAsStateWithLifecycle()
    val dashboardWatchLists by viewModel.dashboardWatchLists.collectAsStateWithLifecycle()
    val cardOrder by viewModel.dashboardCardOrder.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    var showChangeHistoryDialog by remember { mutableStateOf(false) }

    if (showChangeHistoryDialog) {
        ChangeHistoryFullScreenDialog(
            records = changeHistory,
            currencyFormat = currencyFormat,
            onDismiss = { showChangeHistoryDialog = false }
        )
    }

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            cardOrder.forEach { cardKey ->
                when (cardKey) {
                    "portfolio_summary" -> item(key = "portfolio_summary") {
                        CollapsibleCard(
                            title = "Portfolio Summary",
                            pinned = pinStates[DashboardViewModel.KEY_PIN_PORTFOLIO_SUMMARY] == true,
                            onPinToggle = { viewModel.setPinState(DashboardViewModel.KEY_PIN_PORTFOLIO_SUMMARY, it) }
                        ) {
                            PortfolioSummaryRow(
                                uiState = uiState,
                                currencyFormat = currencyFormat,
                                changeHistory = changeHistory,
                                lastRefreshedAt = lastRefreshedAt,
                                onChartClick = { showChangeHistoryDialog = true }
                            )
                        }
                    }

                    "market_indices" -> if (uiState.marketIndices.isNotEmpty()) {
                        item(key = "market_indices") {
                            CollapsibleCard(
                                title = "Market Indices",
                                pinned = pinStates[DashboardViewModel.KEY_PIN_MARKET_INDICES] == true,
                                onPinToggle = { viewModel.setPinState(DashboardViewModel.KEY_PIN_MARKET_INDICES, it) }
                            ) {
                                Column {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    MarketIndexCards(
                                        indices = uiState.marketIndices,
                                        onReorder = { viewModel.reorderMarketIndices(it) }
                                    )
                                }
                            }
                        }
                    }

                    "daily_glance" -> if (uiState.topGainers.isNotEmpty() || uiState.topLosers.isNotEmpty()) {
                        item(key = "daily_glance") {
                            CollapsibleCard(
                                title = "Daily Glance",
                                pinned = pinStates[DashboardViewModel.KEY_PIN_DAILY_GLANCE] == true,
                                onPinToggle = { viewModel.setPinState(DashboardViewModel.KEY_PIN_DAILY_GLANCE, it) }
                            ) {
                                DailyGlanceContent(
                                    topGainers = uiState.topGainers,
                                    topLosers = uiState.topLosers,
                                    overallDailyByType = uiState.overallDailyByType,
                                    currencyFormat = currencyFormat,
                                    onItemClick = onNavigateToItem
                                )
                            }
                        }
                    }

                    "watch_list" -> if (watchListCardVisible && dashboardWatchLists.isNotEmpty()) {
                        item(key = "watch_list") {
                            CollapsibleCard(
                                title = "Watch List",
                                pinned = pinStates[DashboardViewModel.KEY_PIN_WATCH_LIST] == true,
                                onPinToggle = { viewModel.setPinState(DashboardViewModel.KEY_PIN_WATCH_LIST, it) }
                            ) {
                                WatchListCardContent(
                                    watchLists = dashboardWatchLists,
                                    onNavigateToWatchList = onNavigateToWatchList
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
private fun MarketIndexCards(
    indices: List<MarketIndexQuote>,
    onReorder: (List<String>) -> Unit
) {
    val density = LocalDensity.current
    val items = remember(indices.map { it.symbol }) { indices.toMutableStateList() }
    val cardWidthDp = 140.dp
    val spacingDp = 8.dp
    val slotWidthPx = with(density) { (cardWidthDp + spacingDp).toPx() }

    var draggingIndex by remember { mutableIntStateOf(-1) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    // Sync when upstream list changes (e.g. after price refresh)
    if (items.map { it.symbol } != indices.map { it.symbol }) {
        items.clear()
        items.addAll(indices)
    } else {
        indices.forEachIndexed { i, updated ->
            if (items[i] != updated) items[i] = updated
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(spacingDp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        items.forEachIndexed { index, quote ->
            val isDragging = draggingIndex == index
            val offsetPx = if (isDragging) dragOffsetX.toInt() else 0
            val animatedOffset by animateIntOffsetAsState(
                targetValue = IntOffset(offsetPx, 0),
                label = "dragOffset"
            )

            Box(
                modifier = Modifier
                    .zIndex(if (isDragging) 1f else 0f)
                    .offset { animatedOffset }
                    .pointerInput(Unit) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                draggingIndex = index
                                dragOffsetX = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffsetX += dragAmount.x
                                val currentIdx = items.indexOfFirst { it.symbol == quote.symbol }
                                if (currentIdx < 0) return@detectDragGesturesAfterLongPress
                                if (dragOffsetX > slotWidthPx * 0.5f && currentIdx < items.lastIndex) {
                                    val item = items.removeAt(currentIdx)
                                    items.add(currentIdx + 1, item)
                                    draggingIndex = currentIdx + 1
                                    dragOffsetX -= slotWidthPx
                                } else if (dragOffsetX < -slotWidthPx * 0.5f && currentIdx > 0) {
                                    val item = items.removeAt(currentIdx)
                                    items.add(currentIdx - 1, item)
                                    draggingIndex = currentIdx - 1
                                    dragOffsetX += slotWidthPx
                                }
                            },
                            onDragEnd = {
                                draggingIndex = -1
                                dragOffsetX = 0f
                                onReorder(items.map { it.symbol })
                            },
                            onDragCancel = {
                                draggingIndex = -1
                                dragOffsetX = 0f
                            }
                        )
                    }
            ) {
                MarketIndexCard(index = quote)
            }
        }
    }
}

private fun marketIndexIcon(symbol: String): Pair<ImageVector, Color> = when (symbol) {
    "^IXIC" -> Icons.Default.StackedLineChart to Color(0xFF4285F4)
    "^GSPC" -> Icons.Default.ShowChart to Color(0xFFEA4335)
    "^DJI" -> Icons.Default.TrendingUp to Color(0xFF1565C0)
    "GC=F" -> Icons.Default.Toll to Color(0xFFFFB300)
    "^RUT" -> Icons.Default.Warehouse to Color(0xFF7B1FA2)
    "SI=F" -> Icons.Default.Diamond to Color(0xFF90A4AE)
    "CL=F" -> Icons.Default.LocalGasStation to Color(0xFF4E342E)
    "BTC-USD" -> Icons.Default.CurrencyBitcoin to Color(0xFFF7931A)
    else -> Icons.Default.ShowChart to Color(0xFF757575)
}

@Composable
private fun MarketIndexCard(index: MarketIndexQuote) {
    val context = LocalContext.current
    val isPositive = index.change >= 0
    val changeColor = if (index.price == 0.0) MaterialTheme.colorScheme.onSurfaceVariant
        else if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
    val priceFormat = if (index.price >= 1000) DecimalFormat("#,##0.00") else DecimalFormat("#,##0.00")
    val (icon, iconColor) = marketIndexIcon(index.symbol)

    Card(
        onClick = {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://finance.yahoo.com/quote/${index.symbol}")
            )
            context.startActivity(intent)
        },
        modifier = Modifier
            .width(140.dp)
            .height(IntrinsicSize.Min),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val gradient = Brush.linearGradient(
                    colors = listOf(
                        iconColor.copy(alpha = 0.85f),
                        iconColor,
                        iconColor.copy(
                            red = iconColor.red * 0.65f,
                            green = iconColor.green * 0.65f,
                            blue = iconColor.blue * 0.65f
                        )
                    )
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .shadow(2.dp, RoundedCornerShape(6.dp))
                        .background(gradient, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = index.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (index.price != 0.0) {
                Text(
                    text = priceFormat.format(index.price),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${if (isPositive) "+" else ""}${String.format("%.2f", index.change)} (${if (isPositive) "+" else ""}${String.format("%.2f", index.changePercent)}%)",
                    style = MaterialTheme.typography.labelSmall,
                    color = changeColor,
                    maxLines = 1
                )
            } else {
                Text(
                    text = "---",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DailyGlanceContent(
    topGainers: List<DailyGlanceItem>,
    topLosers: List<DailyGlanceItem>,
    overallDailyByType: List<OverallDailyByType>,
    currencyFormat: NumberFormat,
    onItemClick: (String) -> Unit
) {
    var byPerShare by rememberSaveable { mutableStateOf(false) }

    val sortedGainers = remember(topGainers, byPerShare) {
        if (byPerShare) topGainers.sortedByDescending { it.dayGainLossPerShare }
        else topGainers
    }
    val sortedLosers = remember(topLosers, byPerShare) {
        if (byPerShare) topLosers.sortedBy { it.dayGainLossPerShare }
        else topLosers
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(8.dp))

        if (overallDailyByType.isNotEmpty()) {
            Text(
                text = "Overall Daily",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            overallDailyByType.forEach { entry ->
                val color = if (entry.dayChange >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                val sign = if (entry.dayChange > 0) "+" else ""
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.type.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$sign${currencyFormat.format(entry.dayChange)}  ($sign${String.format("%.2f", entry.dayChangePercent)}%)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = color
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = byPerShare,
                onCheckedChange = { byPerShare = it },
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (byPerShare) "By Per Share" else "By Total Value",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clickable { byPerShare = !byPerShare }
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (sortedGainers.isNotEmpty()) {
            Text(
                text = "Top Gainers",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(4.dp))
            sortedGainers.forEach { item ->
                DailyGlanceRow(
                    item = item,
                    currencyFormat = currencyFormat,
                    byPerShare = byPerShare,
                    onClick = { onItemClick(item.ticker) }
                )
            }
        }

        if (sortedGainers.isNotEmpty() && sortedLosers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (sortedLosers.isNotEmpty()) {
            Text(
                text = "Top Losers",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC62828)
            )
            Spacer(modifier = Modifier.height(4.dp))
            sortedLosers.forEach { item ->
                DailyGlanceRow(
                    item = item,
                    currencyFormat = currencyFormat,
                    byPerShare = byPerShare,
                    onClick = { onItemClick(item.ticker) }
                )
            }
        }
    }
}

@Composable
private fun DailyGlanceRow(
    item: DailyGlanceItem,
    currencyFormat: NumberFormat,
    byPerShare: Boolean = false,
    onClick: () -> Unit
) {
    val displayValue = if (byPerShare) item.dayGainLossPerShare else item.dayGainLoss
    val color = if (displayValue >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
    val sign = if (displayValue > 0) "+" else ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.ticker,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$sign${currencyFormat.format(displayValue)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                text = "$sign${String.format("%.2f", item.dayGainLossPercent)}%",
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
private fun WatchListCardContent(
    watchLists: List<DashboardWatchList>,
    onNavigateToWatchList: () -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val sharesFormat = DecimalFormat("#,##0.##")
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = Modifier.fillMaxWidth()) {
        watchLists.forEachIndexed { listIndex, watchList ->
            if (listIndex > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
            }
            Text(
                text = watchList.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (watchList.items.isEmpty()) {
                Text(
                    "No items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                HorizontalDivider(color = dividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Ticker",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        "Shares",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.7f),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        "Added",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(thickness = 2.dp, color = dividerColor)

                watchList.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            item.ticker,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        VerticalDivider(color = dividerColor)
                        Text(
                            sharesFormat.format(item.shares),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.7f),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider(color = dividerColor)
                        Text(
                            currencyFormat.format(item.priceWhenAdded),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.8f),
                            textAlign = TextAlign.End
                        )
                    }
                    HorizontalDivider(color = dividerColor)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onNavigateToWatchList,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("View All Watch Lists")
        }
    }
}

@Composable
private fun PortfolioSummaryRow(
    uiState: DashboardUiState,
    currencyFormat: NumberFormat,
    changeHistory: List<ChangeHistoryEntity> = emptyList(),
    lastRefreshedAt: java.time.LocalDateTime? = null,
    onChartClick: () -> Unit = {}
) {
    val previousValue = uiState.totalPortfolioValue - uiState.totalDayGainLoss
    val dailyPct = if (previousValue != 0.0)
        uiState.totalDayGainLoss / previousValue * 100.0 else 0.0
    val allTimePct = if (uiState.totalCost != 0.0)
        (uiState.totalPortfolioValue - uiState.totalCost) / uiState.totalCost * 100.0 else 0.0
    val dailyColor = when {
        dailyPct > 0 -> Color(0xFF2E7D32)
        dailyPct < 0 -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val allTimeColor = when {
        allTimePct > 0 -> Color(0xFF2E7D32)
        allTimePct < 0 -> Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.onSurface
    }
    val sign = { v: Double -> if (v > 0) "+" else "" }

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        // Total portfolio value - large bold
        Text(
            text = currencyFormat.format(uiState.totalPortfolioValue),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Today's gain/loss line
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${sign(uiState.totalDayGainLoss)}${currencyFormat.format(uiState.totalDayGainLoss)} (${sign(dailyPct)}${"%.2f".format(dailyPct)}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = dailyColor
            )
            Text(
                text = "  Today's gain/loss",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Mini chart
        val sortedHistory = remember(changeHistory) {
            changeHistory.sortedBy { it.date }
        }
        if (sortedHistory.size >= 2) {
            Spacer(modifier = Modifier.height(16.dp))
            ChangeHistoryMiniChart(
                records = sortedHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clickable { onChartClick() }
            )
            Spacer(modifier = Modifier.height(8.dp))

            // All-time percentage below chart
            Text(
                text = "${sign(allTimePct)}${"%.2f".format(allTimePct)}% all time",
                style = MaterialTheme.typography.bodyMedium,
                color = allTimeColor,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
            // Show Day/All percentages inline if no chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Day: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${sign(dailyPct)}${"%.2f".format(dailyPct)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = dailyColor
                )
                Text(
                    text = "    All: ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${sign(allTimePct)}${"%.2f".format(allTimePct)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = allTimeColor
                )
            }
        }

        lastRefreshedAt?.let {
            Spacer(modifier = Modifier.height(8.dp))
            val refreshFormatter = DateTimeFormatter.ofPattern("MMM dd, h:mm a")
            Text(
                text = "Refreshed: ${it.format(refreshFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChangeHistoryMiniChart(
    records: List<ChangeHistoryEntity>,
    modifier: Modifier = Modifier
) {
    val lineColor = Color(0xFF4285F4)
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    val values = records.map { it.totalValue }
    val minVal = values.min() * 0.995
    val maxVal = values.max() * 1.005
    val valRange = (maxVal - minVal).let { if (it < 0.01) 1.0 else it }
    val minEpoch = records.first().date.toEpochDay()
    val maxEpoch = records.last().date.toEpochDay()
    val timeRange = (maxEpoch - minEpoch).let { if (it < 1L) 1L else it }

    val formatYLabel = { v: Double ->
        when {
            v >= 1_000_000 -> "$${"%,.1f".format(v / 1_000_000)}M"
            v >= 1_000 -> "$${"%,.1f".format(v / 1_000)}K"
            else -> "$${"%,.0f".format(v)}"
        }
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(end = 56.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height
            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)

            // Y-axis grid lines (3 levels: top, mid, bottom)
            val yLevels = listOf(maxVal, (maxVal + minVal) / 2, minVal)
            val paint = android.graphics.Paint().apply {
                color = labelColor.hashCode()
                textSize = 10.dp.toPx()
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.LEFT
            }

            yLevels.forEach { yVal ->
                val screenY = ((maxVal - yVal) / valRange * chartHeight).toFloat()
                drawLine(
                    color = gridColor,
                    start = Offset(0f, screenY),
                    end = Offset(chartWidth, screenY),
                    pathEffect = dashedEffect,
                    strokeWidth = 1f
                )
                drawContext.canvas.nativeCanvas.drawText(
                    formatYLabel(yVal),
                    chartWidth + 8.dp.toPx(),
                    screenY + 4.dp.toPx(),
                    paint
                )
            }

            // Draw line
            val path = Path()
            val fillPath = Path()
            records.forEachIndexed { i, record ->
                val normX = (record.date.toEpochDay() - minEpoch).toFloat() / timeRange
                val screenX = normX * chartWidth
                val screenY = ((maxVal - record.totalValue) / valRange * chartHeight).toFloat()
                if (i == 0) {
                    path.moveTo(screenX, screenY)
                    fillPath.moveTo(screenX, chartHeight)
                    fillPath.lineTo(screenX, screenY)
                } else {
                    path.lineTo(screenX, screenY)
                    fillPath.lineTo(screenX, screenY)
                }
            }
            fillPath.lineTo(
                (records.last().date.toEpochDay() - minEpoch).toFloat() / timeRange * chartWidth,
                chartHeight
            )
            fillPath.close()

            drawPath(fillPath, color = lineColor.copy(alpha = 0.08f))
            drawPath(path, color = lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        }

        // X-axis date labels
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(end = 56.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = records.first().date.format(dateFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
            Text(
                text = records.last().date.format(dateFormatter),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor
            )
        }
    }
}

@Composable
private fun ChangeHistoryFullScreenDialog(
    records: List<ChangeHistoryEntity>,
    currencyFormat: NumberFormat,
    onDismiss: () -> Unit
) {
    val sortedRecords = remember(records) { records.sortedBy { it.date } }
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")
    val dividerColor = MaterialTheme.colorScheme.outlineVariant

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                @OptIn(ExperimentalMaterial3Api::class)
                TopAppBar(
                    title = { Text("Change History") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (sortedRecords.size >= 2) {
                    ChangeHistoryFullChart(
                        records = sortedRecords,
                        currencyFormat = currencyFormat,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Weekly change summary
                val today = LocalDate.now()
                val startOfWeek = today.with(java.time.DayOfWeek.MONDAY)
                val weekRecords = remember(records) {
                    records.filter { it.date >= startOfWeek && it.date <= today }
                }
                val weeklyEtfChange = weekRecords.sumOf { it.dailyChangeEtf }
                val weeklyStockChange = weekRecords.sumOf { it.dailyChangeStock }
                val weeklyTotalChange = weekRecords.sumOf { it.dailyChangeTotal }
                val changeFormat = remember {
                    NumberFormat.getCurrencyInstance(Locale.US).apply {
                        minimumFractionDigits = 2
                        maximumFractionDigits = 2
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Change Value This Week So Far:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("ETF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    (if (weeklyEtfChange >= 0) "+" else "") + changeFormat.format(weeklyEtfChange),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (weeklyEtfChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Stock", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    (if (weeklyStockChange >= 0) "+" else "") + changeFormat.format(weeklyStockChange),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (weeklyStockChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                Text("Total", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    (if (weeklyTotalChange >= 0) "+" else "") + changeFormat.format(weeklyTotalChange),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (weeklyTotalChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Data table
                HorizontalDivider(color = dividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .horizontalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Date",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(90.dp).padding(horizontal = 6.dp)
                    )
                    VerticalDivider()
                    Text(
                        "ETF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp).padding(horizontal = 6.dp),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider()
                    Text(
                        "Stock",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp).padding(horizontal = 6.dp),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider()
                    Text(
                        "Total",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(80.dp).padding(horizontal = 6.dp),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider()
                    Text(
                        "Δ ETF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(75.dp).padding(horizontal = 6.dp),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider()
                    Text(
                        "Δ Stock",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(75.dp).padding(horizontal = 6.dp),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider()
                    Text(
                        "Δ Total",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(75.dp).padding(horizontal = 6.dp),
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(thickness = 2.dp, color = dividerColor)

                val displayRecords = remember(records) { records.sortedByDescending { it.date } }
                val altColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                displayRecords.forEachIndexed { index, record ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .horizontalScroll(rememberScrollState())
                            .background(if (index % 2 == 1) altColor else Color.Transparent)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            record.date.format(dateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(90.dp).padding(horizontal = 6.dp)
                        )
                        VerticalDivider()
                        Text(
                            currencyFormat.format(record.etfValue),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(80.dp).padding(horizontal = 6.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider()
                        Text(
                            currencyFormat.format(record.stockValue),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(80.dp).padding(horizontal = 6.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider()
                        Text(
                            currencyFormat.format(record.totalValue),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.width(80.dp).padding(horizontal = 6.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider()
                        Text(
                            (if (record.dailyChangeEtf >= 0) "+" else "") + currencyFormat.format(record.dailyChangeEtf),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (record.dailyChangeEtf >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.width(75.dp).padding(horizontal = 6.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider()
                        Text(
                            (if (record.dailyChangeStock >= 0) "+" else "") + currencyFormat.format(record.dailyChangeStock),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (record.dailyChangeStock >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.width(75.dp).padding(horizontal = 6.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider()
                        Text(
                            (if (record.dailyChangeTotal >= 0) "+" else "") + currencyFormat.format(record.dailyChangeTotal),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (record.dailyChangeTotal >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.width(75.dp).padding(horizontal = 6.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    HorizontalDivider(color = dividerColor)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ChangeHistoryFullChart(
    records: List<ChangeHistoryEntity>,
    currencyFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val totalColor = Color(0xFF4285F4)
    val etfColor = Color(0xFF34A853)
    val stockColor = Color(0xFFEA4335)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipTextColor = MaterialTheme.colorScheme.inverseOnSurface
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")

    val allValues = records.flatMap { listOf(it.totalValue, it.etfValue, it.stockValue) }
    val globalMin = allValues.min() * 0.998
    val globalMax = allValues.max() * 1.002
    val valRange = (globalMax - globalMin).let { if (it < 0.01) 1.0 else it }
    val minEpoch = records.first().date.toEpochDay()
    val maxEpoch = records.last().date.toEpochDay()
    val timeRange = (maxEpoch - minEpoch).let { if (it < 1L) 1L else it }

    var zoom by remember(records) { mutableStateOf(1f) }
    var scrollOffset by remember(records) { mutableStateOf(0f) }
    var chartWidthPx by remember { mutableStateOf(1f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val oldZoom = zoom
        val newZoom = (oldZoom * zoomChange).coerceIn(1f, 5f)
        val maxScroll = (chartWidthPx * newZoom - chartWidthPx).coerceAtLeast(0f)
        val newScroll = (scrollOffset + chartWidthPx / 2) * (newZoom / oldZoom) -
                chartWidthPx / 2 - panChange.x
        scrollOffset = newScroll.coerceIn(0f, maxScroll)
        zoom = newZoom
    }

    var selectedIdx by remember(records) { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) { drawCircle(color = totalColor) }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Total", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) { drawCircle(color = etfColor) }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ETF", style = MaterialTheme.typography.labelSmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(modifier = Modifier.size(10.dp)) { drawCircle(color = stockColor) }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stock", style = MaterialTheme.typography.labelSmall)
                }
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 24.dp, start = 48.dp, end = 12.dp)
                    .transformable(transformState)
                    .pointerInput(records) {
                        detectTapGestures(
                            onTap = { offset ->
                                val cw = size.width.toFloat()
                                val virtualWidth = cw * zoom
                                var bestDist = Float.MAX_VALUE
                                var bestIdx = -1

                                records.forEachIndexed { i, record ->
                                    val normX = (record.date.toEpochDay() - minEpoch).toFloat() / timeRange
                                    val screenX = normX * virtualWidth - scrollOffset
                                    val dist = abs(offset.x - screenX)
                                    if (dist < bestDist) {
                                        bestDist = dist
                                        bestIdx = i
                                    }
                                }

                                selectedIdx = if (bestDist < 30.dp.toPx()) {
                                    if (selectedIdx == bestIdx) null else bestIdx
                                } else null
                            },
                            onDoubleTap = {
                                zoom = 1f
                                scrollOffset = 0f
                                selectedIdx = null
                            }
                        )
                    }
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                chartWidthPx = chartWidth
                val virtualWidth = chartWidth * zoom

                // Grid lines + Y-axis labels
                for (i in 0..3) {
                    val y = chartHeight * i / 3f
                    drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), 1f)
                    val price = globalMax - (valRange * i / 3)
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
                    val seriesConfigs: List<Pair<Color, (ChangeHistoryEntity) -> Double>> = listOf(
                        totalColor to { r: ChangeHistoryEntity -> r.totalValue },
                        etfColor to { r: ChangeHistoryEntity -> r.etfValue },
                        stockColor to { r: ChangeHistoryEntity -> r.stockValue }
                    )

                    for ((seriesColor, getValue) in seriesConfigs) {
                        val path = Path()
                        val fillPath = Path()
                        var lastX = 0f

                        records.forEachIndexed { i, record ->
                            val normX = (record.date.toEpochDay() - minEpoch).toFloat() / timeRange
                            val screenX = normX * virtualWidth - scrollOffset
                            val screenY = ((globalMax - getValue(record)) / valRange * chartHeight).toFloat()
                            lastX = screenX
                            if (i == 0) {
                                path.moveTo(screenX, screenY)
                                fillPath.moveTo(screenX, chartHeight)
                                fillPath.lineTo(screenX, screenY)
                            } else {
                                path.lineTo(screenX, screenY)
                                fillPath.lineTo(screenX, screenY)
                            }
                        }
                        fillPath.lineTo(lastX, chartHeight)
                        fillPath.close()

                        drawPath(fillPath, color = seriesColor.copy(alpha = 0.05f))
                        drawPath(path, color = seriesColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

                        // Data points
                        records.forEach { record ->
                            val normX = (record.date.toEpochDay() - minEpoch).toFloat() / timeRange
                            val screenX = normX * virtualWidth - scrollOffset
                            val screenY = ((globalMax - getValue(record)) / valRange * chartHeight).toFloat()
                            drawCircle(color = seriesColor, radius = 3f, center = Offset(screenX, screenY))
                        }
                    }

                    // Tooltip for selected point
                    selectedIdx?.let { idx ->
                        if (idx in records.indices) {
                            val record = records[idx]
                            val normX = (record.date.toEpochDay() - minEpoch).toFloat() / timeRange
                            val screenX = normX * virtualWidth - scrollOffset
                            val totalY = ((globalMax - record.totalValue) / valRange * chartHeight).toFloat()

                            // Vertical line
                            drawLine(
                                color = labelColor.copy(alpha = 0.4f),
                                start = Offset(screenX, 0f),
                                end = Offset(screenX, chartHeight),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                            )

                            // Highlight points
                            drawCircle(color = Color.White, radius = 6f, center = Offset(screenX, totalY))
                            drawCircle(color = totalColor, radius = 4f, center = Offset(screenX, totalY))

                            // Tooltip
                            val tooltipDateFmt = DateTimeFormatter.ofPattern("MM/dd/yy")
                            val paint = android.graphics.Paint().apply {
                                textSize = 10.dp.toPx()
                                isAntiAlias = true
                            }
                            val line1 = "Total: ${currencyFormat.format(record.totalValue)}"
                            val line2 = "ETF: ${currencyFormat.format(record.etfValue)}  Stock: ${currencyFormat.format(record.stockValue)}"
                            val line3 = record.date.format(tooltipDateFmt)
                            val maxWidth = maxOf(paint.measureText(line1), paint.measureText(line2), paint.measureText(line3))
                            val tooltipPadH = 8.dp.toPx()
                            val tooltipPadV = 5.dp.toPx()
                            val lineH = paint.textSize * 1.3f
                            val tooltipW = maxWidth + tooltipPadH * 2
                            val tooltipH = tooltipPadV * 2 + lineH * 2 + paint.textSize
                            val tooltipX = (screenX - tooltipW / 2).coerceIn(0f, chartWidth - tooltipW)
                            val tooltipY = (totalY - tooltipH - 12.dp.toPx()).coerceAtLeast(0f)

                            drawRoundRect(
                                color = tooltipBg,
                                topLeft = Offset(tooltipX, tooltipY),
                                size = Size(tooltipW, tooltipH),
                                cornerRadius = CornerRadius(6.dp.toPx())
                            )
                            paint.color = tooltipTextColor.hashCode()
                            drawContext.canvas.nativeCanvas.drawText(
                                line1, tooltipX + tooltipPadH,
                                tooltipY + tooltipPadV + paint.textSize * 0.85f, paint
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                line2, tooltipX + tooltipPadH,
                                tooltipY + tooltipPadV + lineH + paint.textSize * 0.85f, paint
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                line3, tooltipX + tooltipPadH,
                                tooltipY + tooltipPadV + lineH * 2 + paint.textSize * 0.85f, paint
                            )
                        }
                    }
                }

                // X-axis labels
                val leftFraction = (scrollOffset / virtualWidth).coerceIn(0f, 1f)
                val centerFraction = ((scrollOffset + chartWidth / 2) / virtualWidth).coerceIn(0f, 1f)
                val rightFraction = ((scrollOffset + chartWidth) / virtualWidth).coerceIn(0f, 1f)

                listOf(
                    0f to leftFraction,
                    chartWidth / 2 to centerFraction,
                    chartWidth to rightFraction
                ).forEach { (screenX, fraction) ->
                    val epochDay = minEpoch + (fraction * timeRange).toLong()
                    val labelDate = LocalDate.ofEpochDay(epochDay)
                    drawContext.canvas.nativeCanvas.drawText(
                        labelDate.format(dateFormatter),
                        screenX,
                        chartHeight + 16.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = labelColor.hashCode()
                            textSize = 9.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }
    }
}


