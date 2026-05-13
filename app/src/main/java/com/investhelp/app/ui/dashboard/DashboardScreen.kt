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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.nio.ByteBuffer
import com.investhelp.app.data.local.entity.ChangeHistoryEntity
import com.investhelp.app.ui.components.CollapsibleCard
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val pieColors = listOf(
    Color(0xFF4285F4),
    Color(0xFFEA4335),
    Color(0xFFFBBC04),
    Color(0xFF34A853),
    Color(0xFFFF6D00),
    Color(0xFF46BDC6),
    Color(0xFFAB47BC),
    Color(0xFF7CB342),
    Color(0xFFE91E63),
    Color(0xFF00ACC1),
    Color(0xFFFF7043),
    Color(0xFF5C6BC0)
)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToItem: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pinStates by viewModel.pinStates.collectAsStateWithLifecycle()
    val positionDetails by viewModel.positionDetails.collectAsStateWithLifecycle()
    val changeHistory by viewModel.changeHistoryRecords.collectAsStateWithLifecycle(initialValue = emptyList())
    val lastRefreshedAt by viewModel.lastRefreshedAt.collectAsStateWithLifecycle()
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
            item(key = "portfolio_summary") {
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

            if (uiState.marketIndices.isNotEmpty()) {
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

            if (uiState.topGainers.isNotEmpty() || uiState.topLosers.isNotEmpty()) {
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

            if (uiState.positions.isNotEmpty()) {
                item(key = "pie_chart") {
                    CollapsibleCard(
                        title = "Positions",
                        pinned = pinStates[DashboardViewModel.KEY_PIN_POSITIONS] == true,
                        onPinToggle = { viewModel.setPinState(DashboardViewModel.KEY_PIN_POSITIONS, it) }
                    ) {
                        PositionsPieChartContent(
                            positions = uiState.positions,
                            onItemClick = onNavigateToItem
                        )
                    }
                }
            }

            item(key = "position_details") {
                CollapsibleCard(
                    title = "Position Details",
                    pinned = pinStates[DashboardViewModel.KEY_PIN_POSITION_DETAILS] == true,
                    onPinToggle = { viewModel.setPinState(DashboardViewModel.KEY_PIN_POSITION_DETAILS, it) }
                ) {
                    PositionDetailsContent(
                        details = positionDetails,
                        currencyFormat = currencyFormat,
                        onItemClick = onNavigateToItem
                    )
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
private fun PositionsPieChartContent(
    positions: List<TickerPosition>,
    onItemClick: (String) -> Unit = {}
) {
    val totalValue = positions.sumOf { it.totalValue }
    val sharesFormat = DecimalFormat("#,##0.##")
    var showAll by remember { mutableStateOf(false) }
    val visibleLimit = 20
    val hasMore = positions.size > visibleLimit
    val visiblePositions = if (showAll) positions else positions.take(visibleLimit)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .aspectRatio(1f)
        ) {
            val diameter = size.minDimension
            val radius = diameter / 2f
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val topLeft = Offset(
                (size.width - diameter) / 2f,
                (size.height - diameter) / 2f
            )

            var startAngle = -90f

            // Draw slices
            positions.forEachIndexed { index, pos ->
                val sweep = (pos.totalValue / totalValue * 360.0).toFloat()
                drawArc(
                    color = pieColors[index % pieColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = topLeft,
                    size = Size(diameter, diameter)
                )
                startAngle += sweep
            }

            // Draw labels inside slices
            startAngle = -90f
            positions.forEachIndexed { index, pos ->
                val sweep = (pos.totalValue / totalValue * 360.0).toFloat()
                if (sweep > 15f) { // Only label slices > 15 degrees
                    val midAngle = Math.toRadians((startAngle + sweep / 2f).toDouble())
                    val labelRadius = radius * 0.65f
                    val lx = centerX + labelRadius * cos(midAngle).toFloat()
                    val ly = centerY + labelRadius * sin(midAngle).toFloat()

                    val label = sharesFormat.format(pos.totalQuantity)
                    drawContext.canvas.nativeCanvas.drawText(
                        label,
                        lx,
                        ly + 5.dp.toPx(),
                        android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 11.dp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                            setShadowLayer(3f, 1f, 1f, android.graphics.Color.BLACK)
                        }
                    )
                }
                startAngle += sweep
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend table with grid lines
        val legendDividerColor = MaterialTheme.colorScheme.outlineVariant
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row
            HorizontalDivider(color = legendDividerColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Ticker",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                VerticalDivider(color = legendDividerColor)
                Text(
                    text = "Shares",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                VerticalDivider(color = legendDividerColor)
                Text(
                    text = "%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.6f),
                    textAlign = TextAlign.End
                )
            }
            HorizontalDivider(thickness = 2.dp, color = legendDividerColor)

            // Data rows
            visiblePositions.forEachIndexed { index, pos ->
                val pct = if (totalValue > 0) pos.totalValue / totalValue * 100 else 0.0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .clickable { onItemClick(pos.ticker) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(pieColors[index % pieColors.size])
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = pos.ticker,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    VerticalDivider(color = legendDividerColor)
                    Text(
                        text = sharesFormat.format(pos.totalQuantity),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider(color = legendDividerColor)
                    Text(
                        text = String.format("%.1f%%", pct),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.6f),
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(color = legendDividerColor)
            }

            if (hasMore) {
                TextButton(
                    onClick = { showAll = !showAll },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (showAll) "Show Less"
                        else "More (${positions.size - visibleLimit} remaining)"
                    )
                }
            }
        }
    }
}

private val tickerIconColors = listOf(
    Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC04), Color(0xFF34A853),
    Color(0xFFFF6D01), Color(0xFF46BDC6), Color(0xFF7B1FA2), Color(0xFFD81B60),
    Color(0xFF00897B), Color(0xFF5C6BC0), Color(0xFFFFA000), Color(0xFF8D6E63)
)

@Composable
private fun DashboardTickerIcon(ticker: String, name: String, logo: ByteArray? = null) {
    val context = LocalContext.current
    val hash = ticker.hashCode()
    val baseColor = tickerIconColors[(hash and 0x7FFFFFFF) % tickerIconColors.size]
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
        modifier = Modifier
            .size(30.dp)
            .shadow(3.dp, RoundedCornerShape(8.dp))
            .background(gradient, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = (if (name != ticker) name else ticker).first().uppercase(),
            style = MaterialTheme.typography.labelMedium,
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
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
        )
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.totalDayGainLoss != 0.0) {
            val dayChangeColor = if (uiState.totalDayGainLoss > 0) Color(0xFF2E7D32) else Color(0xFFC62828)
            val dayChangeSign = if (uiState.totalDayGainLoss > 0) "+" else ""
            Text(
                text = "${dayChangeSign}${currencyFormat.format(uiState.totalDayGainLoss)}",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = dayChangeColor
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
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

        lastRefreshedAt?.let {
            Spacer(modifier = Modifier.height(6.dp))
            val refreshFormatter = DateTimeFormatter.ofPattern("MMM dd, h:mm a")
            Text(
                text = "Refreshed: ${it.format(refreshFormatter)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        val sortedHistory = remember(changeHistory) {
            changeHistory.sortedBy { it.date }
        }
        if (sortedHistory.size >= 2) {
            Spacer(modifier = Modifier.height(12.dp))
            ChangeHistoryMiniChart(
                records = sortedHistory,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clickable { onChartClick() }
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
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    val values = records.map { it.totalValue }
    val minVal = values.min() * 0.998
    val maxVal = values.max() * 1.002
    val valRange = (maxVal - minVal).let { if (it < 0.01) 1.0 else it }
    val minEpoch = records.first().date.toEpochDay()
    val maxEpoch = records.last().date.toEpochDay()
    val timeRange = (maxEpoch - minEpoch).let { if (it < 1L) 1L else it }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, bottom = 16.dp, start = 4.dp, end = 4.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height

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

            drawPath(fillPath, color = lineColor.copy(alpha = 0.1f))
            drawPath(path, color = lineColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

            // Data points
            records.forEach { record ->
                val normX = (record.date.toEpochDay() - minEpoch).toFloat() / timeRange
                val screenX = normX * chartWidth
                val screenY = ((maxVal - record.totalValue) / valRange * chartHeight).toFloat()
                drawCircle(color = lineColor, radius = 3f, center = Offset(screenX, screenY))
            }

            // X-axis labels (start and end)
            val paint = android.graphics.Paint().apply {
                color = labelColor.hashCode()
                textSize = 8.dp.toPx()
                isAntiAlias = true
            }
            drawContext.canvas.nativeCanvas.drawText(
                records.first().date.format(dateFormatter),
                0f,
                chartHeight + 12.dp.toPx(),
                paint.apply { textAlign = android.graphics.Paint.Align.LEFT }
            )
            drawContext.canvas.nativeCanvas.drawText(
                records.last().date.format(dateFormatter),
                chartWidth,
                chartHeight + 12.dp.toPx(),
                paint.apply { textAlign = android.graphics.Paint.Align.RIGHT }
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

                // Data table
                HorizontalDivider(color = dividerColor)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Date",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.2f).padding(horizontal = 6.dp)
                    )
                    VerticalDivider()
                    Text(
                        "ETF",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider()
                    Text(
                        "Stock",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider()
                    Text(
                        "Total",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
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
                            .background(if (index % 2 == 1) altColor else Color.Transparent)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            record.date.format(dateFormatter),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1.2f).padding(horizontal = 6.dp)
                        )
                        VerticalDivider()
                        Text(
                            currencyFormat.format(record.etfValue),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider()
                        Text(
                            currencyFormat.format(record.stockValue),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider()
                        Text(
                            currencyFormat.format(record.totalValue),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
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

private enum class PositionSortField {
    TICKER, SHARES, PRICE, COST, VALUE, CHANGE_AMT, CHANGE_PCT
}

@Composable
private fun PositionDetailsContent(
    details: List<PositionDetail>,
    currencyFormat: NumberFormat,
    onItemClick: (String) -> Unit
) {
    val sharesFormat = DecimalFormat("#,##0.##")
    val priceFormat = DecimalFormat("#,##0.00")
    val dividerColor = MaterialTheme.colorScheme.outlineVariant
    var sortField by rememberSaveable { mutableStateOf(PositionSortField.VALUE.name) }
    var sortAsc by rememberSaveable { mutableStateOf(false) }

    val sortedDetails = remember(details, sortField, sortAsc) {
        val field = PositionSortField.valueOf(sortField)
        val comparator: Comparator<PositionDetail> = when (field) {
            PositionSortField.TICKER -> compareBy { it.ticker }
            PositionSortField.SHARES -> compareBy { it.totalShares }
            PositionSortField.PRICE -> compareBy { it.currentPrice }
            PositionSortField.COST -> compareBy { it.totalCost }
            PositionSortField.VALUE -> compareBy { it.totalValue }
            PositionSortField.CHANGE_AMT -> compareBy { it.changeAmount }
            PositionSortField.CHANGE_PCT -> compareBy { it.changePercent }
        }
        if (sortAsc) details.sortedWith(comparator) else details.sortedWith(comparator.reversed())
    }

    fun onHeaderClick(field: PositionSortField) {
        if (sortField == field.name) {
            sortAsc = !sortAsc
        } else {
            sortField = field.name
            sortAsc = field == PositionSortField.TICKER
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(8.dp))

        if (sortedDetails.isNotEmpty()) {
            val horizontalScroll = rememberScrollState()
            val currentSortField = PositionSortField.valueOf(sortField)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScroll)
            ) {
                HorizontalDivider(color = dividerColor)
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    SortableHeader("Ticker", PositionSortField.TICKER, currentSortField, sortAsc, 100, TextAlign.Start) { onHeaderClick(it) }
                    VerticalDivider(color = dividerColor)
                    SortableHeader("Shares", PositionSortField.SHARES, currentSortField, sortAsc, 70, TextAlign.End) { onHeaderClick(it) }
                    VerticalDivider(color = dividerColor)
                    SortableHeader("Price", PositionSortField.PRICE, currentSortField, sortAsc, 80, TextAlign.End) { onHeaderClick(it) }
                    VerticalDivider(color = dividerColor)
                    SortableHeader("Cost", PositionSortField.COST, currentSortField, sortAsc, 90, TextAlign.End) { onHeaderClick(it) }
                    VerticalDivider(color = dividerColor)
                    SortableHeader("Value", PositionSortField.VALUE, currentSortField, sortAsc, 90, TextAlign.End) { onHeaderClick(it) }
                    VerticalDivider(color = dividerColor)
                    SortableHeader("Change $", PositionSortField.CHANGE_AMT, currentSortField, sortAsc, 90, TextAlign.End) { onHeaderClick(it) }
                    VerticalDivider(color = dividerColor)
                    SortableHeader("Change %", PositionSortField.CHANGE_PCT, currentSortField, sortAsc, 80, TextAlign.End) { onHeaderClick(it) }
                }
                HorizontalDivider(thickness = 2.dp, color = dividerColor)

                sortedDetails.forEach { detail ->
                    val changeColor = if (detail.changeAmount >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    val sign = if (detail.changeAmount > 0) "+" else ""

                    Row(
                        modifier = Modifier
                            .height(IntrinsicSize.Min)
                            .clickable { onItemClick(detail.ticker) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Row(
                            modifier = Modifier.width(100.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DashboardTickerIcon(ticker = detail.ticker, name = detail.name, logo = detail.logo)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = detail.ticker,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        VerticalDivider(color = dividerColor)
                        Text(
                            text = sharesFormat.format(detail.totalShares),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(70.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider(color = dividerColor)
                        Text(
                            text = priceFormat.format(detail.currentPrice),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(80.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider(color = dividerColor)
                        Text(
                            text = currencyFormat.format(detail.totalCost),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(90.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider(color = dividerColor)
                        Text(
                            text = currencyFormat.format(detail.totalValue),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(90.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider(color = dividerColor)
                        Text(
                            text = "$sign${currencyFormat.format(detail.changeAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = changeColor,
                            modifier = Modifier.width(90.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider(color = dividerColor)
                        Text(
                            text = "$sign${String.format("%.2f", detail.changePercent)}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = changeColor,
                            modifier = Modifier.width(80.dp).padding(end = 4.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    HorizontalDivider(color = dividerColor)
                }
            }
        }
    }
}

@Composable
private fun SortableHeader(
    label: String,
    field: PositionSortField,
    currentField: PositionSortField,
    ascending: Boolean,
    widthDp: Int,
    align: TextAlign,
    onClick: (PositionSortField) -> Unit
) {
    val isActive = field == currentField
    Row(
        modifier = Modifier
            .width(widthDp.dp)
            .clickable { onClick(field) }
            .padding(vertical = 4.dp),
        horizontalArrangement = if (align == TextAlign.End) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isActive && align == TextAlign.End) {
            Icon(
                imageVector = if (ascending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
        )
        if (isActive && align != TextAlign.End) {
            Icon(
                imageVector = if (ascending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

