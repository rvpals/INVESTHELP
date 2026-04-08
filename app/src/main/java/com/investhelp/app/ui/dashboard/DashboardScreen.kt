package com.investhelp.app.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.model.AccountWithValue
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
    onNavigateToAccount: (Long) -> Unit,
    onAddAccount: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddAccount) {
                Icon(Icons.Default.Add, contentDescription = "Add Account")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (uiState.positions.isNotEmpty()) {
                item(key = "pie_chart") {
                    PositionsPieChart(positions = uiState.positions)
                }
            }

            item {
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (uiState.accounts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No accounts yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap + to add your first investment account",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(uiState.accounts, key = { it.account.id }) { accountWithValue ->
                AccountCard(
                    accountWithValue = accountWithValue,
                    currencyFormat = currencyFormat,
                    onClick = { onNavigateToAccount(accountWithValue.account.id) }
                )
            }
        }
    }
}

@Composable
private fun PositionsPieChart(positions: List<TickerPosition>) {
    val totalValue = positions.sumOf { it.totalValue }
    val sharesFormat = DecimalFormat("#,##0.##")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Positions", style = MaterialTheme.typography.titleMedium)
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

            // Legend
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                positions.forEachIndexed { index, pos ->
                    val pct = if (totalValue > 0) pos.totalValue / totalValue * 100 else 0.0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(pieColors[index % pieColors.size])
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = pos.ticker,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${sharesFormat.format(pos.totalQuantity)} shares · ${String.format("%.1f%%", pct)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    accountWithValue: AccountWithValue,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = accountWithValue.account.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = currencyFormat.format(accountWithValue.currentValue),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (accountWithValue.account.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = accountWithValue.account.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
