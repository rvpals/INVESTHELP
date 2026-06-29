package com.investhelp.app.ui.sharpe

import android.graphics.Paint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.util.RollingRiskEngine
import com.investhelp.app.util.SharpeCalculator
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.gestures.detectTapGestures
import kotlin.math.abs

private val COLOR_GAIN = Color(0xFF2E7D32)
private val COLOR_LOSS = Color(0xFFC62828)
private val COLOR_GOOD = Color(0xFF2E7D32)
private val COLOR_VERY_GOOD = Color(0xFF0D47A1)
private val COLOR_EXCEPTIONAL = Color(0xFF6A1B9A)

private val COLOR_ROLLING_30D = Color(0xFF1565C0)  // blue
private val COLOR_ROLLING_90D = Color(0xFF6A1B9A)  // purple

private val LOOKBACK_OPTIONS = listOf(
    "6M" to 180,
    "1Y" to 365,
    "2Y" to 730,
    "5Y" to 1825,
    "10Y" to 3650
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharpeRatioScreen(
    viewModel: SharpeRatioViewModel,
    showExplanation: Boolean = true,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val riskFreeRatePercent by viewModel.riskFreeRatePercent.collectAsStateWithLifecycle()
    val lookbackCalendarDays by viewModel.lookbackCalendarDays.collectAsStateWithLifecycle()
    val rollingRiskState by viewModel.rollingRiskState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sharpe Ratio") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.compute() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recalculate")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ConfigCard(
                    riskFreeRatePercent = riskFreeRatePercent,
                    onRiskFreeRateChange = { viewModel.riskFreeRatePercent.value = it },
                    lookbackCalendarDays = lookbackCalendarDays,
                    onLookbackChange = { viewModel.lookbackCalendarDays.value = it },
                    onCalculate = { viewModel.compute() },
                    hasResult = uiState is SharpeRatioUiState.Success
                )
            }

            when (val state = uiState) {
                is SharpeRatioUiState.Idle -> { /* waiting for user to press Calculate */ }
                is SharpeRatioUiState.Loading -> {
                    item { LoadingCard(progressMessage = state.progressMessage) }
                }
                is SharpeRatioUiState.Error -> {
                    item {
                        ErrorCard(message = state.message, onRetry = { viewModel.compute() })
                    }
                }
                is SharpeRatioUiState.Success -> {
                    if (state.isFromCache) {
                        item { CacheBanner(cachedAt = state.cachedAt) }
                    }
                    item { ResultCard(result = state.result) }
                    item { MetricsCard(result = state.result) }
                    if (state.portfolioReturnSeries.size >= 2) {
                        item {
                            DailyReturnsChartCard(returnSeries = state.portfolioReturnSeries)
                        }
                    }
                    if (showExplanation) {
                        item { AboutSharpeCard() }
                    }
                    if (showExplanation && state.result.tickerDetails.isNotEmpty()) {
                        item { CalculationDetailCard(result = state.result) }
                    }
                    if (state.result.skippedTickers.isNotEmpty()) {
                        item {
                            SkippedTickersCard(
                                tickers = state.result.skippedTickers,
                                reasons = state.result.skipReasons
                            )
                        }
                    }
                }
            }

            item {
                RollingRiskCard(
                    uiState = rollingRiskState,
                    onCalculate = { viewModel.computeRollingRisk() }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Config card
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ConfigCard(
    riskFreeRatePercent: String,
    onRiskFreeRateChange: (String) -> Unit,
    lookbackCalendarDays: Int,
    onLookbackChange: (Int) -> Unit,
    onCalculate: () -> Unit,
    hasResult: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Parameters",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = riskFreeRatePercent,
                onValueChange = onRiskFreeRateChange,
                label = { Text("Risk-Free Rate") },
                suffix = { Text("%") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.width(160.dp)
            )

            Column {
                Text(
                    "Lookback Period",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LOOKBACK_OPTIONS.forEach { (label, days) ->
                        FilterChip(
                            selected = lookbackCalendarDays == days,
                            onClick = { onLookbackChange(days) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            Button(
                onClick = onCalculate,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(if (hasResult) "Recalculate" else "Calculate")
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Cache banner
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CacheBanner(cachedAt: Long) {
    val formatter = remember {
        DateTimeFormatter.ofPattern("MMM d, yyyy  h:mm a").withZone(ZoneId.systemDefault())
    }
    val dateString = remember(cachedAt) {
        formatter.format(Instant.ofEpochSecond(cachedAt))
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Cached result from $dateString",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "Press ↻ to refresh",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Loading card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LoadingCard(progressMessage: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                progressMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Error card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(36.dp)
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onRetry) { Text("Retry") }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Result card — large Sharpe value + interpretation label
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ResultCard(result: SharpeCalculator.SharpeResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val sharpeRatio = result.sharpeRatio
            if (sharpeRatio != null) {
                val label = SharpeCalculator.interpretSharpeRatio(sharpeRatio)
                val labelColor = when (label) {
                    "Subpar"      -> COLOR_LOSS
                    "Good"        -> COLOR_GOOD
                    "Very Good"   -> COLOR_VERY_GOOD
                    else          -> COLOR_EXCEPTIONAL
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = sharpeRatio.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = labelColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = labelColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Text(
                    "Sharpe Ratio",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Unable to compute",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                result.insufficientDataReason?.let { reason ->
                    Text(
                        reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Metrics card — supporting figures below the Sharpe value
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MetricsCard(result: SharpeCalculator.SharpeResult) {
    val percentFormat = NumberFormat.getPercentInstance(Locale.US).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            MetricRow("Annualized Return", percentFormat.format(result.annualizedReturn))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            MetricRow("Annualized Volatility", percentFormat.format(result.annualizedVolatility))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            MetricRow("Risk-Free Rate Used", percentFormat.format(result.riskFreeRate))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            MetricRow("Period", "Last ${result.lookbackDays} calendar days")
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            MetricRow("As of Date", result.calculationDate.format(dateFormatter))
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Daily returns chart
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DailyReturnsChartCard(returnSeries: List<Pair<Long, Double>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Daily Portfolio Returns",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Each bar is one trading day's weighted return",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            DailyReturnsChart(
                returnSeries = returnSeries,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
        }
    }
}

@Composable
private fun DailyReturnsChart(
    returnSeries: List<Pair<Long, Double>>,
    modifier: Modifier = Modifier
) {
    if (returnSeries.size < 2) return

    val returnsPercent = remember(returnSeries) { returnSeries.map { it.second * 100.0 } }
    val yRange = remember(returnsPercent) { SharpeCalculator.chartYRange(returnsPercent) }

    var selectedIndex by remember(returnSeries) { mutableIntStateOf(-1) }

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("MMM d").withZone(ZoneId.systemDefault())
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val zeroLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 48.dp, end = 8.dp, top = 8.dp, bottom = 28.dp)
                .pointerInput(returnSeries) {
                    detectTapGestures { offset ->
                        val canvasWidth = size.width.toFloat()
                        val step = canvasWidth / (returnSeries.size - 1)
                        selectedIndex = (offset.x / step)
                            .toInt()
                            .coerceIn(0, returnSeries.size - 1)
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            fun yOf(pct: Double): Float =
                (h * (1.0 - (pct + yRange) / (2.0 * yRange))).toFloat()

            val zeroY = yOf(0.0)
            val xStep = w / (returnSeries.size - 1)

            val gridLevels = listOf(-yRange, -yRange / 2, 0.0, yRange / 2, yRange)
            gridLevels.forEach { level ->
                val y = yOf(level)
                drawLine(
                    color = if (level == 0.0) zeroLineColor else gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (level == 0.0) 1.5f else 1f
                )
            }

            val fillPath = Path()
            fillPath.moveTo(0f, zeroY)
            returnSeries.forEachIndexed { i, (_, ret) ->
                fillPath.lineTo(i * xStep, yOf(ret * 100.0))
            }
            fillPath.lineTo((returnSeries.size - 1) * xStep, zeroY)
            fillPath.close()

            clipRect(left = 0f, top = 0f, right = w, bottom = zeroY) {
                drawPath(fillPath, color = COLOR_GAIN.copy(alpha = 0.3f))
            }
            clipRect(left = 0f, top = zeroY, right = w, bottom = h) {
                drawPath(fillPath, color = COLOR_LOSS.copy(alpha = 0.3f))
            }

            val linePath = Path()
            returnSeries.forEachIndexed { i, (_, ret) ->
                val x = i * xStep
                val y = yOf(ret * 100.0)
                if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            drawPath(linePath, color = primaryColor, style = Stroke(width = 1.5f))

            val yLabelPaint = Paint().apply {
                color = labelTextColor
                textSize = 26f
                textAlign = Paint.Align.RIGHT
            }
            gridLevels.forEach { level ->
                val y = yOf(level)
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.1f%%", level),
                    -6f,
                    y + 9f,
                    yLabelPaint
                )
            }

            val xLabelPaint = Paint().apply {
                color = labelTextColor
                textSize = 24f
                textAlign = Paint.Align.CENTER
            }
            val labelIndices = listOf(
                0,
                returnSeries.size / 4,
                returnSeries.size / 2,
                returnSeries.size * 3 / 4,
                returnSeries.size - 1
            ).distinct()
            labelIndices.forEach { i ->
                val timestamp = returnSeries[i].first
                val label = dateFormatter.format(Instant.ofEpochSecond(timestamp))
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    i * xStep,
                    h + 22f,
                    xLabelPaint
                )
            }

            if (selectedIndex >= 0 && selectedIndex < returnSeries.size) {
                val sx = selectedIndex * xStep
                val sr = returnSeries[selectedIndex].second * 100.0
                val sy = yOf(sr)
                val dotColor = if (sr >= 0) COLOR_GAIN else COLOR_LOSS

                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(sx, 0f),
                    end = Offset(sx, h),
                    strokeWidth = 1f
                )
                drawCircle(color = dotColor, radius = 5f, center = Offset(sx, sy))

                val tooltipDate = dateFormatter.format(
                    Instant.ofEpochSecond(returnSeries[selectedIndex].first)
                )
                val tooltipText = "$tooltipDate:  ${String.format("%+.3f%%", sr)}"
                val tooltipPaint = Paint().apply {
                    color = labelTextColor
                    textSize = 28f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    tooltipText,
                    sx.coerceIn(80f, w - 80f),
                    18f,
                    tooltipPaint
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// About Sharpe Ratio collapsible card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AboutSharpeCard() {
    SimpleCollapsibleCard(title = "About Sharpe Ratio") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Formula
            Text("Formula", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    "SR = (Rp − Rf) / σp",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            // Components
            Text("Components", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            listOf(
                "Rp" to "Portfolio annualized return  (mean daily return × 252)",
                "Rf" to "Annual risk-free rate  (e.g., US 10-yr Treasury yield)",
                "σp" to "Annualized std dev of excess returns  (sample, × √252)"
            ).forEachIndexed { i, (symbol, desc) ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Row(modifier = Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(symbol, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Interpretation table
            Text("Interpretation", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            // Header
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("Range", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.8f))
                Text("Rating", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("Meaning", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.8f))
            }
            listOf(
                Triple("< 1.0", "Subpar", "Risk may outweigh the return"),
                Triple("1.0 – 2.0", "Good", "Acceptable risk-adjusted performance"),
                Triple("2.0 – 3.0", "Very Good", "Strong risk-adjusted performance"),
                Triple("≥ 3.0", "Exceptional", "Outstanding risk-adjusted return")
            ).forEachIndexed { i, (range, rating, meaning) ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                val rowColor = when (rating) {
                    "Subpar"      -> COLOR_LOSS
                    "Good"        -> COLOR_GOOD
                    "Very Good"   -> COLOR_VERY_GOOD
                    else          -> COLOR_EXCEPTIONAL
                }
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(range, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(0.8f))
                    Text(rating, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = rowColor, modifier = Modifier.weight(1f))
                    Text(meaning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1.8f))
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Calculation Detail collapsible card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CalculationDetailCard(result: SharpeCalculator.SharpeResult) {
    val pct = NumberFormat.getPercentInstance(Locale.US).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 4
    }
    val pct2 = NumberFormat.getPercentInstance(Locale.US).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }

    SimpleCollapsibleCard(title = "Calculation Detail") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {

            // ── Section 1: Inputs ────────────────────────────────────────────
            Text("Inputs Used", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            listOf(
                "Risk-free rate" to "${pct2.format(result.riskFreeRate)}  (${pct.format(result.dailyRiskFreeRateUsed)} / day)",
                "Lookback period" to "${result.lookbackDays} calendar days",
                "Aligned trading days" to "${result.alignedTradingDays}",
                "Mean daily return" to pct.format(result.meanDailyReturn)
            ).forEachIndexed { i, (label, value) ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, textAlign = TextAlign.End)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // ── Section 2: Per-Ticker Breakdown ──────────────────────────────
            Text("Per-Ticker Breakdown", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            // Header row
            Row(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 8.dp, vertical = 4.dp)) {
                Text("Ticker", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("Weight", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                Text("Ann.Return", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                Text("Ann.Vol", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
            }
            result.tickerDetails.forEachIndexed { i, d ->
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                val rowBg = if (i % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent
                Row(modifier = Modifier.fillMaxWidth().background(rowBg).padding(horizontal = 8.dp, vertical = 5.dp)) {
                    Text(d.ticker, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text(String.format("%.1f%%", d.weight * 100.0), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    val retColor = if (d.annualizedReturn >= 0) COLOR_GAIN else COLOR_LOSS
                    Text(pct2.format(d.annualizedReturn), style = MaterialTheme.typography.bodySmall, color = retColor, modifier = Modifier.weight(1.2f), textAlign = TextAlign.End)
                    Text(pct2.format(d.annualizedVolatility), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // ── Section 3: Step-by-Step ──────────────────────────────────────
            Text("Step-by-Step", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            val rfPct = result.riskFreeRate * 100.0
            val annRetPct = result.annualizedReturn * 100.0
            val annVolPct = result.annualizedVolatility * 100.0
            val steps = listOf(
                "1. Mean daily portfolio return" to "${pct.format(result.meanDailyReturn)}",
                "2. Annualized return  (× 252)" to "${String.format("%.2f", annRetPct)}%",
                "3. Daily risk-free rate  (${String.format("%.1f", rfPct)}% ÷ 252)" to pct.format(result.dailyRiskFreeRateUsed),
                "4. Excess returns computed" to "${result.alignedTradingDays} daily obs − Rf",
                "5. Annualized volatility  (σ × √252)" to "${String.format("%.2f", annVolPct)}%",
                "6. Sharpe Ratio  = (${String.format("%.2f", annRetPct)}% − ${String.format("%.1f", rfPct)}%) / ${String.format("%.2f", annVolPct)}%" to
                    (result.sharpeRatio?.toString() ?: "N/A")
            )
            steps.forEachIndexed { i, (label, value) ->
                if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    val isLastStep = i == steps.size - 1
                    Text(
                        value,
                        style = if (isLastStep) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodySmall,
                        fontWeight = if (isLastStep) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.End,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SimpleCollapsibleCard — no pin persistence; local expand/collapse state only
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SimpleCollapsibleCard(
    title: String,
    defaultExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(defaultExpanded) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Box(modifier = Modifier.padding(16.dp)) { content() }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Rolling Risk Metrics card — 30d / 90d rolling Sharpe Ratio
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RollingRiskCard(
    uiState: RollingRiskUiState,
    onCalculate: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "Rolling Risk Metrics",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "30-day and 90-day annualized Sharpe Ratio computed from actual transaction history",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            when (val state = uiState) {
                is RollingRiskUiState.Idle -> {
                    Button(
                        onClick = onCalculate,
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Calculate") }
                }
                is RollingRiskUiState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is RollingRiskUiState.Error -> {
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedButton(
                        onClick = onCalculate,
                        modifier = Modifier.align(Alignment.End)
                    ) { Text("Retry") }
                }
                is RollingRiskUiState.Success -> {
                    val points = state.points
                    val last30 = points.lastOrNull { it.rolling30SharpeRatio != null }?.rolling30SharpeRatio
                    val last90 = points.lastOrNull { it.rolling90SharpeRatio != null }?.rolling90SharpeRatio

                    // Legend + latest values
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 24.dp, height = 3.dp)
                                    .background(COLOR_ROLLING_30D, RoundedCornerShape(2.dp))
                            )
                            Text("30-Day", style = MaterialTheme.typography.labelSmall)
                            if (last30 != null) {
                                Text(
                                    String.format("%.2f", last30),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = COLOR_ROLLING_30D
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 24.dp, height = 3.dp)
                                    .background(COLOR_ROLLING_90D, RoundedCornerShape(2.dp))
                            )
                            Text("90-Day", style = MaterialTheme.typography.labelSmall)
                            if (last90 != null) {
                                Text(
                                    String.format("%.2f", last90),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = COLOR_ROLLING_90D
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = onCalculate,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recalculate", modifier = Modifier.size(16.dp))
                        }
                    }

                    RollingRiskChart(
                        points = points,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    )

                    Text(
                        "${points.size} trading days  ·  Tap chart to inspect",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RollingRiskChart(
    points: List<RollingRiskEngine.RollingRiskPoint>,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val allValues = points.mapNotNull { it.rolling30SharpeRatio } +
                    points.mapNotNull { it.rolling90SharpeRatio }
    if (allValues.isEmpty()) return

    val peak = allValues.maxOf { abs(it) }.coerceAtLeast(1.0)
    val yRange = peak * 1.15

    var selectedIndex by remember(points) { mutableIntStateOf(-1) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MM/dd") }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    val zeroLineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.70f)
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, end = 8.dp, top = 8.dp, bottom = 28.dp)
                .pointerInput(points) {
                    detectTapGestures { offset ->
                        val canvasWidth = size.width.toFloat()
                        selectedIndex = ((offset.x / canvasWidth) * (points.size - 1))
                            .toInt()
                            .coerceIn(0, points.size - 1)
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            fun yOf(v: Double): Float = (h * (1.0 - (v + yRange) / (2.0 * yRange))).toFloat()
            fun xOf(i: Int): Float = i * w / (points.size - 1)

            val zeroY = yOf(0.0)

            // Grid lines at integer Sharpe values within range
            val gridStep = if (yRange <= 2.0) 0.5 else if (yRange <= 4.0) 1.0 else 2.0
            val gridLevels = mutableListOf<Double>()
            var v = -(Math.ceil(yRange / gridStep) * gridStep)
            while (v <= yRange + gridStep * 0.5) {
                gridLevels.add(v)
                v += gridStep
            }

            gridLevels.forEach { level ->
                val y = yOf(level)
                if (y < 0 || y > h) return@forEach
                drawLine(
                    color = if (level == 0.0) zeroLineColor else gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = if (level == 0.0) 1.5f else 1f
                )
            }

            // 30d rolling Sharpe line — multiple subpaths for null gaps
            val path30 = Path()
            var in30 = false
            points.forEachIndexed { i, pt ->
                val sv = pt.rolling30SharpeRatio
                if (sv != null) {
                    if (!in30) { path30.moveTo(xOf(i), yOf(sv)); in30 = true }
                    else path30.lineTo(xOf(i), yOf(sv))
                } else in30 = false
            }
            drawPath(path30, color = COLOR_ROLLING_30D, style = Stroke(width = 4f))

            // 90d rolling Sharpe line
            val path90 = Path()
            var in90 = false
            points.forEachIndexed { i, pt ->
                val sv = pt.rolling90SharpeRatio
                if (sv != null) {
                    if (!in90) { path90.moveTo(xOf(i), yOf(sv)); in90 = true }
                    else path90.lineTo(xOf(i), yOf(sv))
                } else in90 = false
            }
            drawPath(path90, color = COLOR_ROLLING_90D, style = Stroke(width = 4f))

            // Y-axis labels
            val yLabelPaint = Paint().apply {
                color = labelTextColor
                textSize = 24f
                textAlign = android.graphics.Paint.Align.RIGHT
            }
            gridLevels.forEach { level ->
                val y = yOf(level)
                if (y < 0 || y > h) return@forEach
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%.1f", level),
                    -4f, y + 8f, yLabelPaint
                )
            }

            // X-axis date labels
            val xLabelPaint = Paint().apply {
                color = labelTextColor
                textSize = 22f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            listOf(0, points.size / 4, points.size / 2, points.size * 3 / 4, points.size - 1)
                .distinct()
                .forEach { i ->
                    drawContext.canvas.nativeCanvas.drawText(
                        points[i].date.format(dateFormatter),
                        xOf(i), h + 22f, xLabelPaint
                    )
                }

            // Selection overlay
            if (selectedIndex in points.indices) {
                val sx = xOf(selectedIndex)
                val pt = points[selectedIndex]

                drawLine(
                    color = Color.Gray.copy(alpha = 0.5f),
                    start = Offset(sx, 0f),
                    end = Offset(sx, h),
                    strokeWidth = 1f
                )
                pt.rolling30SharpeRatio?.let {
                    drawCircle(color = COLOR_ROLLING_30D, radius = 5f, center = Offset(sx, yOf(it)))
                }
                pt.rolling90SharpeRatio?.let {
                    drawCircle(color = COLOR_ROLLING_90D, radius = 5f, center = Offset(sx, yOf(it)))
                }

                val tooltipPaint = Paint().apply {
                    color = labelTextColor
                    textSize = 26f
                    textAlign = android.graphics.Paint.Align.LEFT
                    isFakeBoldText = true
                }
                val txClamped = sx.coerceIn(4f, w - 100f)
                val dateLabel = pt.date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                drawContext.canvas.nativeCanvas.drawText(dateLabel, txClamped, 18f, tooltipPaint)
                pt.rolling30SharpeRatio?.let {
                    drawContext.canvas.nativeCanvas.drawText(
                        "30d: ${String.format("%.2f", it)}",
                        txClamped, 34f, tooltipPaint
                    )
                }
                pt.rolling90SharpeRatio?.let {
                    drawContext.canvas.nativeCanvas.drawText(
                        "90d: ${String.format("%.2f", it)}",
                        txClamped, 50f, tooltipPaint
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Skipped tickers card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SkippedTickersCard(
    tickers: List<String>,
    reasons: Map<String, String>
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Excluded from calculation (${tickers.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            tickers.forEachIndexed { index, ticker ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        ticker,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        reasons[ticker] ?: "Unknown reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.weight(1f, fill = false).padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
