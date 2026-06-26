package com.investhelp.app.ui.sharpe

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.util.SharpeCalculator
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.graphics.toArgb

private val COLOR_GAIN = Color(0xFF2E7D32)
private val COLOR_LOSS = Color(0xFFC62828)
private val COLOR_GOOD = Color(0xFF2E7D32)
private val COLOR_VERY_GOOD = Color(0xFF0D47A1)
private val COLOR_EXCEPTIONAL = Color(0xFF6A1B9A)

private val LOOKBACK_OPTIONS = listOf(
    "6 months" to 180,
    "1 year" to 365,
    "2 years" to 730
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharpeRatioScreen(
    viewModel: SharpeRatioViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val riskFreeRatePercent by viewModel.riskFreeRatePercent.collectAsStateWithLifecycle()
    val lookbackCalendarDays by viewModel.lookbackCalendarDays.collectAsStateWithLifecycle()

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
                    onCalculate = { viewModel.compute() }
                )
            }

            when (val state = uiState) {
                is SharpeRatioUiState.Idle -> { /* auto-computes on init; nothing to show yet */ }
                is SharpeRatioUiState.Loading -> {
                    item { LoadingCard(progressMessage = state.progressMessage) }
                }
                is SharpeRatioUiState.Error -> {
                    item {
                        ErrorCard(message = state.message, onRetry = { viewModel.compute() })
                    }
                }
                is SharpeRatioUiState.Success -> {
                    item { ResultCard(result = state.result) }
                    item { MetricsCard(result = state.result) }
                    if (state.portfolioReturnSeries.size >= 2) {
                        item {
                            DailyReturnsChartCard(returnSeries = state.portfolioReturnSeries)
                        }
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
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Config card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ConfigCard(
    riskFreeRatePercent: String,
    onRiskFreeRateChange: (String) -> Unit,
    lookbackCalendarDays: Int,
    onLookbackChange: (Int) -> Unit,
    onCalculate: () -> Unit
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                Text("Calculate")
            }
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

            // Horizontal grid lines at –range, –range/2, 0, +range/2, +range
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

            // Build a single fill path that covers the area between the returns line and zero
            val fillPath = Path()
            fillPath.moveTo(0f, zeroY)
            returnSeries.forEachIndexed { i, (_, ret) ->
                fillPath.lineTo(i * xStep, yOf(ret * 100.0))
            }
            fillPath.lineTo((returnSeries.size - 1) * xStep, zeroY)
            fillPath.close()

            // Clip to the upper half for the green (positive) fill
            clipRect(left = 0f, top = 0f, right = w, bottom = zeroY) {
                drawPath(fillPath, color = COLOR_GAIN.copy(alpha = 0.3f))
            }
            // Clip to the lower half for the red (negative) fill
            clipRect(left = 0f, top = zeroY, right = w, bottom = h) {
                drawPath(fillPath, color = COLOR_LOSS.copy(alpha = 0.3f))
            }

            // Returns line
            val linePath = Path()
            returnSeries.forEachIndexed { i, (_, ret) ->
                val x = i * xStep
                val y = yOf(ret * 100.0)
                if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
            }
            drawPath(linePath, color = primaryColor, style = Stroke(width = 1.5f))

            // Y-axis labels — drawn in the left padding via nativeCanvas
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

            // X-axis date labels — 5 evenly spaced, drawn below chart via nativeCanvas
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

            // Tap-to-select: vertical crosshair + dot + tooltip
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
