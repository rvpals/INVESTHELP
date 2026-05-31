package com.investhelp.app.ui.nextday

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.NumberFormat
import java.util.Locale

@Composable
fun NextDayActionsScreen(
    viewModel: NextDayActionsViewModel
) {
    val signals by viewModel.signals.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val scanProgress by viewModel.scanProgress.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Next-Day Actions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Button(
                onClick = { viewModel.runScan() },
                enabled = !isScanning
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                Text("Run Scan")
            }
        }

        if (isScanning) {
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                scanProgress,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        if (signals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))

            val actionCount = signals.groupBy { it.action }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                actionCount.forEach { (action, list) ->
                    if (action != NextDayAction.HOLD) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = actionColor(action).copy(alpha = 0.15f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "${list.size}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = actionColor(action)
                                )
                                Text(
                                    action.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = actionColor(action)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            ActionGrid(signals = signals, currencyFormat = currencyFormat)
        }
    }
}

@Composable
private fun ActionGrid(
    signals: List<ActionableSignal>,
    currencyFormat: NumberFormat
) {
    val dividerColor = MaterialTheme.colorScheme.outline
    val horizontalScroll = rememberScrollState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .padding(4.dp)
        ) {
            item(key = "header") {
                HorizontalDivider(color = dividerColor)
                Row(
                    modifier = Modifier.height(IntrinsicSize.Min).padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HeaderCell("Ticker", 70)
                    VerticalDivider(color = dividerColor)
                    HeaderCell("Shares", 60)
                    VerticalDivider(color = dividerColor)
                    HeaderCell("Price", 80)
                    VerticalDivider(color = dividerColor)
                    HeaderCell("Value", 90)
                    VerticalDivider(color = dividerColor)
                    HeaderCell("Alloc %", 65)
                    VerticalDivider(color = dividerColor)
                    HeaderCell("Return %", 70)
                    VerticalDivider(color = dividerColor)
                    HeaderCell("Action", 110)
                    VerticalDivider(color = dividerColor)
                    HeaderCell("Reasoning", 220)
                }
                HorizontalDivider(thickness = 2.dp, color = dividerColor)
            }

            itemsIndexed(signals, key = { _, s -> s.ticker }) { index, signal ->
                val altColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                val rowBg = when {
                    signal.action == NextDayAction.STOP_LOSS -> Color(0xFFC62828).copy(alpha = 0.08f)
                    signal.action == NextDayAction.STRONG_BUY -> Color(0xFF2E7D32).copy(alpha = 0.08f)
                    signal.action == NextDayAction.TRIM_PROFIT -> Color(0xFFFF8F00).copy(alpha = 0.08f)
                    signal.action == NextDayAction.REBALANCE_TRIM -> Color(0xFF1565C0).copy(alpha = 0.08f)
                    index % 2 == 1 -> altColor
                    else -> Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .background(rowBg)
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DataCell(signal.ticker, 70, FontWeight.Bold)
                    VerticalDivider(color = dividerColor)
                    DataCell(String.format(Locale.US, "%.1f", signal.shares), 60, textAlign = TextAlign.End)
                    VerticalDivider(color = dividerColor)
                    DataCell(currencyFormat.format(signal.currentPrice), 80, textAlign = TextAlign.End)
                    VerticalDivider(color = dividerColor)
                    DataCell(currencyFormat.format(signal.totalValue), 90, textAlign = TextAlign.End)
                    VerticalDivider(color = dividerColor)
                    DataCell(String.format(Locale.US, "%.1f%%", signal.allocationPct), 65, textAlign = TextAlign.End)
                    VerticalDivider(color = dividerColor)
                    val returnColor = if (signal.totalReturnPct >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    DataCell(
                        String.format(Locale.US, "%+.1f%%", signal.totalReturnPct),
                        70,
                        textAlign = TextAlign.End,
                        color = returnColor
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = signal.action.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = actionColor(signal.action),
                        modifier = Modifier.width(110.dp).padding(horizontal = 4.dp)
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = signal.reasoning,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.width(220.dp).padding(horizontal = 4.dp),
                        maxLines = 3
                    )
                }
                HorizontalDivider(color = dividerColor)
            }
        }
    }
}

@Composable
private fun HeaderCell(text: String, widthDp: Int) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
        fontWeight = FontWeight.Bold,
        modifier = Modifier.width(widthDp.dp).padding(horizontal = 4.dp),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun DataCell(
    text: String,
    widthDp: Int,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = Color.Unspecified
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        fontWeight = fontWeight,
        modifier = Modifier.width(widthDp.dp).padding(horizontal = 4.dp),
        textAlign = textAlign,
        color = color
    )
}

@Composable
private fun actionColor(action: NextDayAction): Color {
    return when (action) {
        NextDayAction.STRONG_BUY -> Color(0xFF2E7D32)
        NextDayAction.TRIM_PROFIT -> Color(0xFFFF8F00)
        NextDayAction.REBALANCE_TRIM -> Color(0xFF1565C0)
        NextDayAction.STOP_LOSS -> Color(0xFFC62828)
        NextDayAction.HOLD -> Color(0xFF616161)
    }
}
