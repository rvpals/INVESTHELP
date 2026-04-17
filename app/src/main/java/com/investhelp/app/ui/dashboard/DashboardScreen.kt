package com.investhelp.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.ui.components.CollapsibleCard
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
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
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Scaffold { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.marketIndices.isNotEmpty()) {
                item(key = "market_indices") {
                    CollapsibleCard(
                        title = "Market Indices",
                        pinned = pinStates[DashboardViewModel.KEY_PIN_MARKET_INDICES] == true,
                        onPinToggle = { viewModel.setPinState(DashboardViewModel.KEY_PIN_MARKET_INDICES, it) }
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(8.dp))
                            MarketIndexCards(indices = uiState.marketIndices)
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

        }
    }
}

@Composable
private fun MarketIndexCards(indices: List<MarketIndexQuote>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(indices, key = { it.symbol }) { index ->
            MarketIndexCard(index = index)
        }
    }
}

@Composable
private fun MarketIndexCard(index: MarketIndexQuote) {
    val context = LocalContext.current
    val isPositive = index.change >= 0
    val changeColor = if (index.price == 0.0) MaterialTheme.colorScheme.onSurfaceVariant
        else if (isPositive) Color(0xFF2E7D32) else Color(0xFFC62828)
    val priceFormat = if (index.price >= 1000) DecimalFormat("#,##0.00") else DecimalFormat("#,##0.00")

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
            Text(
                text = index.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
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

        if (topGainers.isNotEmpty()) {
            Text(
                text = "Top Gainers",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(4.dp))
            topGainers.forEach { item ->
                DailyGlanceRow(
                    item = item,
                    currencyFormat = currencyFormat,
                    onClick = { onItemClick(item.ticker) }
                )
            }
        }

        if (topGainers.isNotEmpty() && topLosers.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (topLosers.isNotEmpty()) {
            Text(
                text = "Top Losers",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFC62828)
            )
            Spacer(modifier = Modifier.height(4.dp))
            topLosers.forEach { item ->
                DailyGlanceRow(
                    item = item,
                    currencyFormat = currencyFormat,
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
    onClick: () -> Unit
) {
    val color = if (item.dayGainLoss >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
    val sign = if (item.dayGainLoss > 0) "+" else ""

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
                text = "$sign${currencyFormat.format(item.dayGainLoss)}",
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
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header row
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                Text(
                    text = "Shares",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                Text(
                    text = "%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.6f),
                    textAlign = TextAlign.End
                )
            }
            HorizontalDivider()

            // Data rows
            visiblePositions.forEachIndexed { index, pos ->
                val pct = if (totalValue > 0) pos.totalValue / totalValue * 100 else 0.0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
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
                    Text(
                        text = sharesFormat.format(pos.totalQuantity),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = String.format("%.1f%%", pct),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.6f),
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider()
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

