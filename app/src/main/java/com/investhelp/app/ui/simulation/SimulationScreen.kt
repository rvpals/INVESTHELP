package com.investhelp.app.ui.simulation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.remote.HistoricalPrice
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(
    viewModel: SimulationViewModel,
    onBack: () -> Unit
) {
    val currentPrice by viewModel.currentPrice.collectAsStateWithLifecycle()
    val isLoadingPrice by viewModel.isLoadingPrice.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val result by viewModel.result.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    var ticker by remember { mutableStateOf("") }
    var shares by remember { mutableStateOf("") }
    var costPerShare by remember { mutableStateOf("") }

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Simulation") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Ticker
            OutlinedTextField(
                value = ticker,
                onValueChange = { ticker = it.uppercase() },
                label = { Text("Ticker") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Number of shares
            OutlinedTextField(
                value = shares,
                onValueChange = { shares = it },
                label = { Text("Number of Shares") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cost per share with fetch button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = costPerShare,
                    onValueChange = { costPerShare = it },
                    label = { Text("Cost per Share") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        if (ticker.isNotBlank()) {
                            viewModel.fetchCurrentPrice(ticker.trim())
                        }
                    },
                    enabled = ticker.isNotBlank() && !isLoadingPrice
                ) {
                    if (isLoadingPrice) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Get Price")
                }
            }

            // Auto-fill fetched price
            currentPrice?.let { price ->
                androidx.compose.runtime.LaunchedEffect(price) {
                    costPerShare = "%.2f".format(price)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Run Sim button
            Button(
                onClick = {
                    val sharesVal = shares.toDoubleOrNull()
                    val costVal = costPerShare.toDoubleOrNull()
                    if (ticker.isNotBlank() && sharesVal != null && costVal != null) {
                        viewModel.runSimulation(ticker.trim(), sharesVal, costVal)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = ticker.isNotBlank() &&
                        shares.toDoubleOrNull() != null &&
                        costPerShare.toDoubleOrNull() != null &&
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

            // Error
            if (error != null) {
                Text(
                    text = error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Results
            result?.let { sim ->
                Spacer(modifier = Modifier.height(16.dp))

                // Summary card
                val isProfit = sim.profitLoss >= 0
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isProfit)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "If you bought ${sim.shares} shares of ${sim.ticker} 2 weeks ago",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        ResultRow("Buy Price (2w ago)", currencyFormat.format(sim.costPerShare))
                        ResultRow("Current Price", currencyFormat.format(sim.currentPrice))
                        ResultRow("Total Cost", currencyFormat.format(sim.totalCost))
                        ResultRow("Current Value", currencyFormat.format(sim.currentValue))

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "${if (isProfit) "Profit" else "Loss"}: ${currencyFormat.format(sim.profitLoss)} (${"%.2f".format(sim.profitLossPct)}%)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isProfit)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Price chart
                Text("Price Trend (Last 2 Weeks)", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                PriceChart(
                    prices = sim.prices,
                    buyPrice = sim.costPerShare,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

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
private fun PriceChart(
    prices: List<HistoricalPrice>,
    buyPrice: Double,
    modifier: Modifier = Modifier
) {
    if (prices.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val buyLineColor = MaterialTheme.colorScheme.error
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)

    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    val closes = prices.map { it.close }
    val minPrice = (closes.min()).coerceAtMost(buyPrice) * 0.998
    val maxPrice = (closes.max()).coerceAtLeast(buyPrice) * 1.002
    val priceRange = maxPrice - minPrice

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
        ) {
            val chartWidth = size.width
            val chartHeight = size.height

            // Grid lines (3 horizontal)
            for (i in 0..3) {
                val y = chartHeight * i / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1f
                )
                // Price labels
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

            // Buy price dashed line
            val buyY = ((maxPrice - buyPrice) / priceRange * chartHeight).toFloat()
            drawLine(
                color = buyLineColor,
                start = Offset(0f, buyY),
                end = Offset(chartWidth, buyY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
            )

            // Price line path
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

            // Fill area under curve
            fillPath.lineTo(chartWidth, chartHeight)
            fillPath.close()
            drawPath(fillPath, color = fillColor)

            // Draw the line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )

            // Dots at data points
            prices.forEachIndexed { i, hp ->
                val x = i * stepX
                val y = ((maxPrice - hp.close) / priceRange * chartHeight).toFloat()
                drawCircle(
                    color = lineColor,
                    radius = 4f,
                    center = Offset(x, y)
                )
            }

            // Date labels (first, middle, last)
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
