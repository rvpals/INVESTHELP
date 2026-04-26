package com.investhelp.app.ui.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.ui.components.DateRangeSelector
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemStatisticsScreen(
    ticker: String,
    viewModel: ItemViewModel,
    onBack: () -> Unit
) {
    var startDate by remember { mutableStateOf(LocalDate.now().minusYears(1)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    val selectedItem by viewModel.selectedItem.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    val itemName = selectedItem?.name ?: ticker

    LaunchedEffect(ticker) {
        viewModel.loadItem(ticker)
    }

    LaunchedEffect(ticker, startDate, endDate) {
        viewModel.loadStatistics(ticker, startDate, endDate)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$itemName Statistics") },
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
        ) {
            DateRangeSelector(
                startDate = startDate,
                endDate = endDate,
                onStartDateChanged = { startDate = it },
                onEndDateChanged = { endDate = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Buy Statistics", style = MaterialTheme.typography.titleLarge)

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

                Spacer(modifier = Modifier.height(8.dp))

                Text("Sell Statistics", style = MaterialTheme.typography.titleLarge)

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
