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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.ui.components.CollapsibleCard
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
            .verticalScroll(rememberScrollState())
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

        Spacer(modifier = Modifier.height(12.dp))
        var explanationPinned by rememberSaveable { mutableStateOf(false) }
        CollapsibleCard(
            title = "Explanation",
            pinned = explanationPinned,
            onPinToggle = { explanationPinned = it }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExplanationSection(
                    title = "STOP LOSS",
                    color = actionColor(NextDayAction.STOP_LOSS),
                    text = "Triggered when the current price closes BELOW the 20-day Simple Moving Average (SMA). " +
                        "The 20-day SMA is calculated by averaging the closing prices of the last 20 trading days. " +
                        "When price drops below this level, it signals a technical breakdown — the short-term trend " +
                        "has turned bearish. Action: exit the position or tighten a protective stop-loss order immediately " +
                        "at next market open."
                )
                ExplanationSection(
                    title = "TRIM PROFITS",
                    color = actionColor(NextDayAction.TRIM_PROFIT),
                    text = "Triggered when Total Return % exceeds the Profit Target threshold (default 20%). " +
                        "Total Return is calculated as: ((Current Price - Cost Basis) / Cost Basis) x 100. " +
                        "Cost Basis is the weighted average price of all your Buy transactions for that ticker. " +
                        "When a position has gained significantly, taking partial profits locks in gains and reduces " +
                        "risk of giving back returns in a reversal. Action: sell a portion (e.g., 25-50%) at next open."
                )
                ExplanationSection(
                    title = "REBALANCE",
                    color = actionColor(NextDayAction.REBALANCE_TRIM),
                    text = "Triggered when a single position's Allocation % exceeds the concentration cap " +
                        "(default 10% for stocks, 25% for ETFs). Allocation % = (Position Value / Total Portfolio Value) x 100. " +
                        "Over-concentration in one asset increases portfolio risk — if that one position drops sharply, " +
                        "it disproportionately impacts your total wealth. Action: trim the position back to within the cap " +
                        "by selling excess shares."
                )
                ExplanationSection(
                    title = "STRONG BUY",
                    color = actionColor(NextDayAction.STRONG_BUY),
                    text = "Triggered when today's closing volume is 1.5x or more the 20-day average volume. " +
                        "A volume spike indicates institutional accumulation — large players are actively buying. " +
                        "When this occurs alongside a price holding above the 20-day SMA (not in breakdown), " +
                        "it signals strong conviction buying and potential for continued upward momentum. " +
                        "Action: consider adding to the position at next open, especially if price holds above yesterday's high."
                )
                ExplanationSection(
                    title = "HOLD",
                    color = actionColor(NextDayAction.HOLD),
                    text = "Default state when no threshold is violated. The position is trading above its 20-day SMA, " +
                        "return hasn't hit the profit target, allocation is within caps, and volume is normal. " +
                        "No action required — continue monitoring."
                )
            }
        }

        if (signals.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            var detailPinned by rememberSaveable { mutableStateOf(false) }
            CollapsibleCard(
                title = "Detail on the Analysis",
                pinned = detailPinned,
                onPinToggle = { detailPinned = it }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    signals.forEach { signal ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = actionColor(signal.action).copy(alpha = 0.05f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    "${signal.ticker} → ${signal.action.label}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = actionColor(signal.action)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = signal.detailLog,
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .padding(4.dp)
        ) {
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

            signals.forEachIndexed { index, signal ->
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
private fun ExplanationSection(title: String, color: Color, text: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
