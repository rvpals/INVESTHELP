package com.investhelp.app.ui.transaction

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzePriceScreen(
    ticker: String,
    viewModel: AnalyzePriceViewModel,
    onSelectPrice: (Double) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    LaunchedEffect(ticker) {
        viewModel.loadData(ticker)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analyzing Stock Price $ticker") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Current Price
                Text(
                    "Current Price",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                uiState.currentPrice?.let {
                    PriceRow("Current Price", it, currencyFormat, onSelectPrice)
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Transaction-based prices
                Text(
                    "Transaction History Prices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.avgTransactionPrice != null) {
                    PriceRow("Average Price", uiState.avgTransactionPrice!!, currencyFormat, onSelectPrice)
                    Spacer(modifier = Modifier.height(6.dp))
                    PriceRow("Max Price", uiState.maxTransactionPrice!!, currencyFormat, onSelectPrice)
                    Spacer(modifier = Modifier.height(6.dp))
                    PriceRow("Min Price", uiState.minTransactionPrice!!, currencyFormat, onSelectPrice)
                } else {
                    Text(
                        "No transactions found for $ticker",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // Historic Prices - 2 column layout
                Text(
                    "Historic Prices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                val hasAnyHistoric = uiState.highLastWeek != null || uiState.lowLastWeek != null ||
                        uiState.highLastMonth != null || uiState.lowLastMonth != null ||
                        uiState.highLastYear != null || uiState.lowLastYear != null ||
                        uiState.highMax != null || uiState.lowMax != null

                if (hasAnyHistoric) {
                    val histDividerColor = MaterialTheme.colorScheme.outlineVariant
                    // Column headers
                    HorizontalDivider(color = histDividerColor)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        VerticalDivider(color = histDividerColor)
                        Text(
                            "High",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                        VerticalDivider(color = histDividerColor)
                        Text(
                            "Low",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 6.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                    HorizontalDivider(thickness = 2.dp, color = histDividerColor)

                    HistoricPriceRow("Last Week", uiState.highLastWeek, uiState.lowLastWeek, currencyFormat, onSelectPrice, histDividerColor)
                    HistoricPriceRow("Last Month", uiState.highLastMonth, uiState.lowLastMonth, currencyFormat, onSelectPrice, histDividerColor)
                    HistoricPriceRow("Last Year", uiState.highLastYear, uiState.lowLastYear, currencyFormat, onSelectPrice, histDividerColor)
                    HistoricPriceRow("Max", uiState.highMax, uiState.lowMax, currencyFormat, onSelectPrice, histDividerColor)
                } else {
                    Text(
                        "Historical data unavailable",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceRow(
    label: String,
    price: Double,
    currencyFormat: NumberFormat,
    onSelectPrice: (Double) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(
                currencyFormat.format(price),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        OutlinedButton(onClick = { onSelectPrice(price) }) {
            Text("Use")
        }
    }
}

@Composable
private fun HistoricPriceRow(
    label: String,
    high: Double?,
    low: Double?,
    currencyFormat: NumberFormat,
    onSelectPrice: (Double) -> Unit,
    dividerColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.outlineVariant
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 6.dp)
        )
        VerticalDivider(color = dividerColor)
        ClickablePrice(
            price = high,
            currencyFormat = currencyFormat,
            onSelectPrice = onSelectPrice,
            modifier = Modifier.weight(1f)
        )
        VerticalDivider(color = dividerColor)
        ClickablePrice(
            price = low,
            currencyFormat = currencyFormat,
            onSelectPrice = onSelectPrice,
            modifier = Modifier.weight(1f)
        )
    }
    HorizontalDivider(color = dividerColor)
}

@Composable
private fun ClickablePrice(
    price: Double?,
    currencyFormat: NumberFormat,
    onSelectPrice: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    if (price != null) {
        Text(
            text = currencyFormat.format(price),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
            modifier = modifier
                .clickable { onSelectPrice(price) }
                .padding(vertical = 8.dp)
        )
    } else {
        Text(
            text = "N/A",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = modifier.padding(vertical = 8.dp)
        )
    }
}
