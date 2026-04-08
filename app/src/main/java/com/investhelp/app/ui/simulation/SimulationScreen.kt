package com.investhelp.app.ui.simulation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.remote.HistoricalPrice
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private fun TimeRange.chartColor(): Color = when (this) {
    TimeRange.ONE_WEEK -> Color(0xFF1E88E5)
    TimeRange.TWO_WEEKS -> Color(0xFF00ACC1)
    TimeRange.ONE_MONTH -> Color(0xFF43A047)
    TimeRange.THREE_MONTHS -> Color(0xFF7CB342)
    TimeRange.SIX_MONTHS -> Color(0xFFF57C00)
    TimeRange.ONE_YEAR -> Color(0xFFE53935)
    TimeRange.TWO_YEARS -> Color(0xFFD81B60)
    TimeRange.FIVE_YEARS -> Color(0xFF8E24AA)
    TimeRange.TEN_YEARS -> Color(0xFF5E35B1)
    TimeRange.MAX -> Color(0xFF546E7A)
}

private data class ChartSeries(
    val label: String,
    val color: Color,
    val prices: List<HistoricalPrice>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(
    viewModel: SimulationViewModel,
    initialTicker: String = "",
    initialShares: String = ""
) {
    var ticker by remember { mutableStateOf(initialTicker) }
    var shares by remember { mutableStateOf(initialShares) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Main", "Detail Simulation")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simulation") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> MainSimulationTab(
                    viewModel = viewModel,
                    ticker = ticker,
                    onTickerChange = { ticker = it },
                    shares = shares,
                    onSharesChange = { shares = it }
                )
                1 -> DetailSimulationTab(
                    viewModel = viewModel,
                    ticker = ticker,
                    onTickerChange = { ticker = it }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MainSimulationTab(
    viewModel: SimulationViewModel,
    ticker: String,
    onTickerChange: (String) -> Unit,
    shares: String,
    onSharesChange: (String) -> Unit
) {
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var selectedRanges by remember { mutableStateOf(setOf(TimeRange.TWO_WEEKS)) }
    var showChartDialog by remember { mutableStateOf(false) }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = ticker,
            onValueChange = { onTickerChange(it.uppercase()) },
            label = { Text("Ticker") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = shares,
            onValueChange = { onSharesChange(it) },
            label = { Text("Number of Shares") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Time Ranges (select multiple)", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(4.dp))
        listOf("Week", "Month", "Year").forEach { group ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TimeRange.entries.filter { it.group == group }.forEach { range ->
                    FilterChip(
                        selected = range in selectedRanges,
                        onClick = {
                            selectedRanges = if (range in selectedRanges) {
                                selectedRanges - range
                            } else {
                                selectedRanges + range
                            }
                        },
                        label = { Text(range.label) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val sharesVal = shares.toDoubleOrNull()
                if (ticker.isNotBlank() && sharesVal != null && selectedRanges.isNotEmpty()) {
                    viewModel.runSimulation(ticker.trim(), sharesVal, selectedRanges)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = ticker.isNotBlank() &&
                    shares.toDoubleOrNull() != null &&
                    selectedRanges.isNotEmpty() &&
                    !isRunning
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Run Sim")
        }

        if (error != null) {
            Text(
                text = error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (results.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            // Summary cards
            results.forEach { sim ->
                val rangeLabel = sim.timeRange.label
                val isProfit = sim.profitLoss >= 0

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProfit)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "${sim.ticker} — $rangeLabel ago",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ResultRow("Buy Price", currencyFormat.format(sim.startPrice))
                        ResultRow("Current Price", currencyFormat.format(sim.currentPrice))
                        ResultRow("Total Cost", currencyFormat.format(sim.totalCost))
                        ResultRow("Current Value", currencyFormat.format(sim.currentValue))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${if (isProfit) "Profit" else "Loss"}: ${currencyFormat.format(sim.profitLoss)} (${"%.2f".format(sim.profitLossPct)}%)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isProfit)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Chart header with legend and zoom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Price Overlay", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { showChartDialog = true }) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Enlarge chart",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Legend
            val seriesList = results.map { sim ->
                ChartSeries(
                    label = sim.timeRange.label,
                    color = sim.timeRange.chartColor(),
                    prices = sim.prices
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                seriesList.forEach { series ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(series.color)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(series.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            MultiLinePriceChart(
                seriesList = seriesList,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clickable { showChartDialog = true }
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (showChartDialog) {
                MultiLineChartZoomDialog(
                    title = "${results.first().ticker} — Price Overlay",
                    seriesList = seriesList,
                    onDismiss = { showChartDialog = false }
                )
            }
        }
    }
}

@Composable
private fun DetailSimulationTab(
    viewModel: SimulationViewModel,
    ticker: String,
    onTickerChange: (String) -> Unit
) {
    val isRunningDetail by viewModel.isRunningDetail.collectAsStateWithLifecycle()
    val detailCharts by viewModel.detailCharts.collectAsStateWithLifecycle()
    val detailError by viewModel.detailError.collectAsStateWithLifecycle()

    var zoomedChartIndex by remember { mutableStateOf<Int?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = ticker,
            onValueChange = { onTickerChange(it.uppercase()) },
            label = { Text("Ticker") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (ticker.isNotBlank()) {
                    viewModel.runDetailSimulation(ticker.trim())
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = ticker.isNotBlank() && !isRunningDetail
        ) {
            if (isRunningDetail) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Run Detail Simulation")
        }

        if (detailError != null) {
            Text(
                text = detailError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        detailCharts.forEachIndexed { index, chartData ->
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(chartData.label, style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = { zoomedChartIndex = index }) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Zoom in",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            PriceChart(
                prices = chartData.prices,
                startPrice = chartData.startPrice,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clickable { zoomedChartIndex = index }
            )
        }

        if (detailCharts.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    zoomedChartIndex?.let { idx ->
        val chartData = detailCharts.getOrNull(idx)
        if (chartData != null) {
            SingleLineChartZoomDialog(
                title = "${ticker.trim().uppercase()} — ${chartData.label}",
                prices = chartData.prices,
                startPrice = chartData.startPrice,
                onDismiss = { zoomedChartIndex = null }
            )
        }
    }
}

// region Zoom Dialogs

@Composable
private fun SingleLineChartZoomDialog(
    title: String,
    prices: List<HistoricalPrice>,
    startPrice: Double,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                PriceChart(
                    prices = prices,
                    startPrice = startPrice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MultiLineChartZoomDialog(
    title: String,
    seriesList: List<ChartSeries>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close")
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    seriesList.forEach { series ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Canvas(modifier = Modifier.size(10.dp)) {
                                drawCircle(series.color)
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(series.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
                MultiLinePriceChart(
                    seriesList = seriesList,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )
            }
        }
    }
}

// endregion

// region Chart composables

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MultiLinePriceChart(
    seriesList: List<ChartSeries>,
    modifier: Modifier = Modifier
) {
    val validSeries = seriesList.filter { it.prices.size >= 2 }
    if (validSeries.isEmpty()) return

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipTextColor = MaterialTheme.colorScheme.inverseOnSurface

    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yy")
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    val allCloses = validSeries.flatMap { s -> s.prices.map { it.close } }
    val minPrice = allCloses.min() * 0.998
    val maxPrice = allCloses.max() * 1.002
    val priceRange = maxPrice - minPrice

    var selectedNormX by remember { mutableStateOf<Float?>(null) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, bottom = 24.dp, start = 48.dp, end = 12.dp)
                .pointerInput(validSeries) {
                    detectTapGestures { offset ->
                        val normX = (offset.x / size.width).coerceIn(0f, 1f)
                        selectedNormX =
                            if (selectedNormX != null && abs(selectedNormX!! - normX) < 0.02f) null
                            else normX
                    }
                }
        ) {
            val chartWidth = size.width
            val chartHeight = size.height

            // Grid lines + Y axis labels
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

            // Draw each series line
            validSeries.forEach { series ->
                val stepX = chartWidth / (series.prices.size - 1).toFloat()
                val path = Path()
                series.prices.forEachIndexed { i, hp ->
                    val x = i * stepX
                    val y = ((maxPrice - hp.close) / priceRange * chartHeight).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = series.color,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }

            // Crosshair + tooltip on tap
            selectedNormX?.let { normX ->
                val crosshairX = normX * chartWidth
                drawLine(
                    color = labelColor.copy(alpha = 0.4f),
                    start = Offset(crosshairX, 0f),
                    end = Offset(crosshairX, chartHeight),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )

                data class TooltipEntry(
                    val text: String,
                    val color: Color,
                    val y: Float
                )

                val entries = validSeries.map { series ->
                    val idx = (normX * (series.prices.size - 1)).toInt()
                        .coerceIn(0, series.prices.size - 1)
                    val hp = series.prices[idx]
                    val y = ((maxPrice - hp.close) / priceRange * chartHeight).toFloat()

                    // Dot on the line
                    drawCircle(Color.White, 6f, Offset(crosshairX, y))
                    drawCircle(series.color, 4f, Offset(crosshairX, y))

                    val date = Instant.ofEpochSecond(hp.timestamp)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    TooltipEntry(
                        text = "${series.label}: ${currencyFormat.format(hp.close)}  ${date.format(dateFormatter)}",
                        color = series.color,
                        y = y
                    )
                }

                // Draw tooltip box
                val paint = android.graphics.Paint().apply {
                    textSize = 10.dp.toPx()
                    isAntiAlias = true
                }
                val lineHeight = paint.textSize * 1.4f
                val tooltipPadH = 8.dp.toPx()
                val tooltipPadV = 6.dp.toPx()
                val dotRadius = 4.dp.toPx()
                val dotGap = 6.dp.toPx()
                val maxTextW = entries.maxOf { paint.measureText(it.text) }
                val tooltipW = maxTextW + dotRadius * 2 + dotGap + tooltipPadH * 2
                val tooltipH = lineHeight * entries.size + tooltipPadV * 2

                // Place tooltip on opposite side of tap to avoid occlusion
                val tooltipX = if (crosshairX < chartWidth / 2) {
                    (chartWidth - tooltipW).coerceAtLeast(0f)
                } else {
                    0f
                }
                val tooltipY = 0f

                drawRoundRect(
                    color = tooltipBg,
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipW, tooltipH),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )

                entries.forEachIndexed { i, entry ->
                    val textY = tooltipY + tooltipPadV + lineHeight * i + paint.textSize * 0.8f
                    val dotCenterY = textY - paint.textSize * 0.3f
                    drawCircle(
                        entry.color,
                        dotRadius,
                        Offset(tooltipX + tooltipPadH + dotRadius, dotCenterY)
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        entry.text,
                        tooltipX + tooltipPadH + dotRadius * 2 + dotGap,
                        textY,
                        paint.apply { color = tooltipTextColor.hashCode() }
                    )
                }
            }

            // X axis date labels from the longest series
            val longestSeries = validSeries.maxByOrNull { it.prices.size }
            longestSeries?.let { series ->
                val stepX = chartWidth / (series.prices.size - 1).toFloat()
                val labelIndices = listOf(0, series.prices.size / 2, series.prices.size - 1)
                labelIndices.forEach { i ->
                    val x = i * stepX
                    val date = Instant.ofEpochSecond(series.prices[i].timestamp)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    drawContext.canvas.nativeCanvas.drawText(
                        date.format(DateTimeFormatter.ofPattern("MM/dd")),
                        x,
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

@Composable
private fun PriceChart(
    prices: List<HistoricalPrice>,
    startPrice: Double,
    modifier: Modifier = Modifier
) {
    if (prices.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val startLineColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipText = MaterialTheme.colorScheme.inverseOnSurface

    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    val closes = prices.map { it.close }
    val minPrice = (closes.min()).coerceAtMost(startPrice) * 0.998
    val maxPrice = (closes.max()).coerceAtLeast(startPrice) * 1.002
    val priceRange = maxPrice - minPrice

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp, bottom = 24.dp, start = 48.dp, end = 12.dp)
                .pointerInput(prices) {
                    detectTapGestures { offset ->
                        val stepX = size.width / (prices.size - 1).toFloat()
                        val tappedIndex = ((offset.x + stepX / 2) / stepX)
                            .toInt()
                            .coerceIn(0, prices.size - 1)
                        selectedIndex = if (selectedIndex == tappedIndex) null else tappedIndex
                    }
                }
        ) {
            val chartWidth = size.width
            val chartHeight = size.height

            for (i in 0..3) {
                val y = chartHeight * i / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1f
                )
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

            val startY = ((maxPrice - startPrice) / priceRange * chartHeight).toFloat()
            drawLine(
                color = startLineColor,
                start = Offset(0f, startY),
                end = Offset(chartWidth, startY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )

            val path = Path()
            val fillPath = Path()
            val stepX = chartWidth / (prices.size - 1).toFloat()

            prices.forEachIndexed { i, hp ->
                val x = i * stepX
                val y = ((maxPrice - hp.close) / priceRange * chartHeight).toFloat()
                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, chartHeight)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }

            fillPath.lineTo(chartWidth, chartHeight)
            fillPath.close()
            drawPath(fillPath, color = fillColor)

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            prices.forEachIndexed { i, hp ->
                val x = i * stepX
                val y = ((maxPrice - hp.close) / priceRange * chartHeight).toFloat()
                drawCircle(
                    color = lineColor,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }

            selectedIndex?.let { idx ->
                val hp = prices[idx]
                val x = idx * stepX
                val y = ((maxPrice - hp.close) / priceRange * chartHeight).toFloat()

                drawLine(
                    color = labelColor.copy(alpha = 0.4f),
                    start = Offset(x, 0f),
                    end = Offset(x, chartHeight),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                )

                drawCircle(color = Color.White, radius = 7f, center = Offset(x, y))
                drawCircle(color = lineColor, radius = 5f, center = Offset(x, y))

                val date = Instant.ofEpochSecond(hp.timestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                val tooltipStr = "${currencyFormat.format(hp.close)}  ${date.format(dateFormatter)}"
                val paint = android.graphics.Paint().apply {
                    textSize = 11.dp.toPx()
                    isAntiAlias = true
                }
                val textWidth = paint.measureText(tooltipStr)
                val tooltipPadH = 8.dp.toPx()
                val tooltipPadV = 5.dp.toPx()
                val tooltipW = textWidth + tooltipPadH * 2
                val tooltipH = paint.textSize + tooltipPadV * 2
                val tooltipX = (x - tooltipW / 2).coerceIn(0f, chartWidth - tooltipW)
                val tooltipY = (y - tooltipH - 12.dp.toPx()).coerceAtLeast(0f)

                drawRoundRect(
                    color = tooltipBg,
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipW, tooltipH),
                    cornerRadius = CornerRadius(6.dp.toPx())
                )
                drawContext.canvas.nativeCanvas.drawText(
                    tooltipStr,
                    tooltipX + tooltipPadH,
                    tooltipY + tooltipPadV + paint.textSize * 0.85f,
                    paint.apply {
                        color = tooltipText.hashCode()
                    }
                )
            }

            val labelIndices = listOf(0, prices.size / 2, prices.size - 1)
            labelIndices.forEach { i ->
                val x = i * stepX
                val date = Instant.ofEpochSecond(prices[i].timestamp)
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                drawContext.canvas.nativeCanvas.drawText(
                    date.format(dateFormatter),
                    x,
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

// endregion
