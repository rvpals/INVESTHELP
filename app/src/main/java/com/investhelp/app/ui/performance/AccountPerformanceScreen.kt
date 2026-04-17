package com.investhelp.app.ui.performance

import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import com.investhelp.app.ui.settings.SettingsViewModel
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountPerformanceScreen(
    viewModel: AccountPerformanceViewModel
) {
    val allRecords by viewModel.allRecords.collectAsStateWithLifecycle()
    val accounts by viewModel.allAccounts.collectAsStateWithLifecycle()
    val chartData by viewModel.chartData.collectAsStateWithLifecycle()
    val pulledValue by viewModel.pulledValue.collectAsStateWithLifecycle()

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm")

    var selectedAccountId by remember { mutableStateOf<Long?>(null) }
    var totalValueText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }
    var accountDropdownExpanded by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<AccountPerformanceEntity?>(null) }
    var editTarget by remember { mutableStateOf<AccountPerformanceEntity?>(null) }
    var editNoteText by remember { mutableStateOf("") }
    var chartSelectedAccountIds by remember { mutableStateOf(setOf<Long>()) }

    val context = LocalContext.current
    val warnBeforeDelete = remember {
        context.getSharedPreferences(SettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SettingsViewModel.KEY_WARN_BEFORE_DELETE, true)
    }

    // Auto-select first account
    LaunchedEffect(accounts) {
        if (selectedAccountId == null && accounts.isNotEmpty()) {
            selectedAccountId = accounts.first().id
        }
        if (chartSelectedAccountIds.isEmpty() && accounts.isNotEmpty()) {
            chartSelectedAccountIds = setOf(accounts.first().id)
        }
    }

    // Load chart data when selected accounts change
    LaunchedEffect(chartSelectedAccountIds) {
        viewModel.loadChartData(chartSelectedAccountIds)
    }

    // Apply pulled value
    LaunchedEffect(pulledValue) {
        pulledValue?.let {
            totalValueText = "%.2f".format(it)
            viewModel.clearPulledValue()
        }
    }

    // Delete confirmation dialog
    deleteTarget?.let { record ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Record") },
            text = { Text("Delete this performance record?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRecord(record)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Edit note dialog
    editTarget?.let { record ->
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Edit Note") },
            text = {
                OutlinedTextField(
                    value = editNoteText,
                    onValueChange = { editNoteText = it },
                    label = { Text("Note") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateRecord(record.copy(note = editNoteText.trim()))
                    editTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Account Performance") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Add Record section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Add Record",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Account selector
                        ExposedDropdownMenuBox(
                            expanded = accountDropdownExpanded,
                            onExpandedChange = { accountDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = accounts.find { it.id == selectedAccountId }?.name ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Account") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountDropdownExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = accountDropdownExpanded,
                                onDismissRequest = { accountDropdownExpanded = false }
                            ) {
                                accounts.forEach { account ->
                                    DropdownMenuItem(
                                        text = { Text(account.name) },
                                        onClick = {
                                            selectedAccountId = account.id
                                            accountDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Total Value + Pull from App
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = totalValueText,
                                onValueChange = { totalValueText = it },
                                label = { Text("Total Value") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    selectedAccountId?.let { viewModel.pullValueFromApp(it) }
                                },
                                enabled = selectedAccountId != null
                            ) {
                                Text("Pull from App")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Note field
                        OutlinedTextField(
                            value = noteText,
                            onValueChange = { noteText = it },
                            label = { Text("Note (optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val value = totalValueText.toDoubleOrNull()
                                if (selectedAccountId != null && value != null) {
                                    viewModel.saveRecord(selectedAccountId!!, value, noteText)
                                    totalValueText = ""
                                    noteText = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = selectedAccountId != null && totalValueText.toDoubleOrNull() != null
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Record")
                        }
                    }
                }
            }

            // Performance Chart section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Performance Chart",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Multi-account selector
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    accounts.forEachIndexed { index, account ->
                        val color = CHART_COLORS[index % CHART_COLORS.size]
                        val selected = account.id in chartSelectedAccountIds
                        FilterChip(
                            selected = selected,
                            onClick = {
                                chartSelectedAccountIds = if (selected) {
                                    chartSelectedAccountIds - account.id
                                } else {
                                    chartSelectedAccountIds + account.id
                                }
                            },
                            label = { Text(account.name) },
                            leadingIcon = if (selected) {
                                {
                                    Canvas(modifier = Modifier.size(8.dp)) {
                                        drawCircle(color = color)
                                    }
                                }
                            } else null
                        )
                    }
                }
            }

            item {
                val seriesList = remember(chartData, accounts) {
                    chartData.entries
                        .filter { (_, records) -> records.size >= 2 }
                        .map { (accountId, records) ->
                            val accountIndex = accounts.indexOfFirst { it.id == accountId }
                            val color = CHART_COLORS[
                                (if (accountIndex >= 0) accountIndex else 0) % CHART_COLORS.size
                            ]
                            ChartSeries(
                                accountId = accountId,
                                accountName = accounts.find { it.id == accountId }?.name ?: "Unknown",
                                color = color,
                                records = records
                            )
                        }
                }

                if (seriesList.isNotEmpty()) {
                    PerformanceLineChart(
                        seriesList = seriesList,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    )
                } else if (chartSelectedAccountIds.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                "Need at least 2 records per account to show chart",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Records list
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Records (${allRecords.size})",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (allRecords.isEmpty()) {
                item {
                    Text(
                        "No performance records yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(allRecords, key = { it.id }) { record ->
                val accountName = accounts.find { it.id == record.accountId }?.name ?: "Unknown"
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = accountName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = record.dateTime.format(dateTimeFormatter),
                                style = MaterialTheme.typography.bodySmall
                            )
                            if (record.note.isNotBlank()) {
                                Text(
                                    text = record.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            text = currencyFormat.format(record.totalValue),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = {
                            editNoteText = record.note
                            editTarget = record
                        }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit note",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            if (warnBeforeDelete) {
                                deleteTarget = record
                            } else {
                                viewModel.deleteRecord(record)
                            }
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

private val CHART_COLORS = listOf(
    Color(0xFF4285F4), // Blue
    Color(0xFFEA4335), // Red
    Color(0xFF34A853), // Green
    Color(0xFFFBBC04), // Amber
    Color(0xFF9C27B0), // Purple
    Color(0xFFFF6D00), // Orange
    Color(0xFF00ACC1), // Cyan
    Color(0xFF795548), // Brown
)

private data class ChartSeries(
    val accountId: Long,
    val accountName: String,
    val color: Color,
    val records: List<AccountPerformanceEntity>
)

@Composable
private fun PerformanceLineChart(
    seriesList: List<ChartSeries>,
    modifier: Modifier = Modifier
) {
    if (seriesList.isEmpty()) return

    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipTextColor = MaterialTheme.colorScheme.inverseOnSurface

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")

    // Global value range across all series
    val allValues = seriesList.flatMap { s -> s.records.map { it.totalValue } }
    val globalMin = allValues.min() * 0.998
    val globalMax = allValues.max() * 1.002
    val valRange = (globalMax - globalMin).let { if (it < 0.01) 1.0 else it }

    // Global time range across all series
    val allEpochs = seriesList.flatMap { s -> s.records.map { it.dateTime.toEpochSecond(ZoneOffset.UTC) } }
    val minEpoch = allEpochs.min()
    val maxEpoch = allEpochs.max()
    val timeRange = (maxEpoch - minEpoch).let { if (it < 1L) 1L else it }

    // Zoom and pan state
    var zoom by remember(seriesList) { mutableStateOf(1f) }
    var scrollOffset by remember(seriesList) { mutableStateOf(0f) }
    var chartWidthPx by remember { mutableStateOf(1f) }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val oldZoom = zoom
        val newZoom = (oldZoom * zoomChange).coerceIn(1f, 5f)
        val maxScroll = (chartWidthPx * newZoom - chartWidthPx).coerceAtLeast(0f)
        val newScroll = (scrollOffset + chartWidthPx / 2) * (newZoom / oldZoom) -
                chartWidthPx / 2 - panChange.x
        scrollOffset = newScroll.coerceIn(0f, maxScroll)
        zoom = newZoom
    }

    // Selection state
    var selectedSeriesIdx by remember(seriesList) { mutableStateOf<Int?>(null) }
    var selectedPointIdx by remember(seriesList) { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                seriesList.forEach { series ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            drawCircle(color = series.color)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            series.accountName,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 24.dp, start = 48.dp, end = 12.dp)
                    .transformable(transformState)
                    .pointerInput(seriesList) {
                        detectTapGestures(
                            onTap = { offset ->
                                val cw = size.width.toFloat()
                                val virtualWidth = cw * zoom
                                var bestDist = Float.MAX_VALUE
                                var bestSeries = -1
                                var bestPoint = -1

                                seriesList.forEachIndexed { sIdx, series ->
                                    series.records.forEachIndexed { pIdx, record ->
                                        val epoch = record.dateTime.toEpochSecond(ZoneOffset.UTC)
                                        val normX = (epoch - minEpoch).toFloat() / timeRange
                                        val screenX = normX * virtualWidth - scrollOffset
                                        val dist = abs(offset.x - screenX)
                                        if (dist < bestDist) {
                                            bestDist = dist
                                            bestSeries = sIdx
                                            bestPoint = pIdx
                                        }
                                    }
                                }

                                if (bestDist < 30.dp.toPx()) {
                                    if (selectedSeriesIdx == bestSeries && selectedPointIdx == bestPoint) {
                                        selectedSeriesIdx = null
                                        selectedPointIdx = null
                                    } else {
                                        selectedSeriesIdx = bestSeries
                                        selectedPointIdx = bestPoint
                                    }
                                } else {
                                    selectedSeriesIdx = null
                                    selectedPointIdx = null
                                }
                            },
                            onDoubleTap = {
                                zoom = 1f
                                scrollOffset = 0f
                                selectedSeriesIdx = null
                                selectedPointIdx = null
                            }
                        )
                    }
            ) {
                val chartWidth = size.width
                val chartHeight = size.height
                chartWidthPx = chartWidth
                val virtualWidth = chartWidth * zoom

                // Grid lines + Y-axis labels
                for (i in 0..3) {
                    val y = chartHeight * i / 3f
                    drawLine(gridColor, Offset(0f, y), Offset(chartWidth, y), 1f)
                    val price = globalMax - (valRange * i / 3)
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

                // Clip data area for zoomed content
                clipRect(0f, 0f, chartWidth, chartHeight) {
                    // Draw each series
                    for (series in seriesList) {
                        if (series.records.size < 2) continue
                        val seriesColor = series.color

                        val path = Path()
                        val fillPath = Path()
                        var lastScreenX = 0f

                        series.records.forEachIndexed { i, record ->
                            val epoch = record.dateTime.toEpochSecond(ZoneOffset.UTC)
                            val normX = (epoch - minEpoch).toFloat() / timeRange
                            val screenX = normX * virtualWidth - scrollOffset
                            val screenY = ((globalMax - record.totalValue) / valRange * chartHeight).toFloat()
                            lastScreenX = screenX

                            if (i == 0) {
                                path.moveTo(screenX, screenY)
                                fillPath.moveTo(screenX, chartHeight)
                                fillPath.lineTo(screenX, screenY)
                            } else {
                                path.lineTo(screenX, screenY)
                                fillPath.lineTo(screenX, screenY)
                            }
                        }

                        fillPath.lineTo(lastScreenX, chartHeight)
                        fillPath.close()

                        val fillAlpha = if (seriesList.size == 1) 0.1f else 0.05f
                        drawPath(fillPath, color = seriesColor.copy(alpha = fillAlpha))
                        drawPath(
                            path = path,
                            color = seriesColor,
                            style = Stroke(width = 3f, cap = StrokeCap.Round)
                        )

                        // Data point dots
                        series.records.forEach { record ->
                            val epoch = record.dateTime.toEpochSecond(ZoneOffset.UTC)
                            val normX = (epoch - minEpoch).toFloat() / timeRange
                            val screenX = normX * virtualWidth - scrollOffset
                            val screenY = ((globalMax - record.totalValue) / valRange * chartHeight).toFloat()
                            drawCircle(color = seriesColor, radius = 4f, center = Offset(screenX, screenY))
                        }
                    }

                    // Tooltip for selected point
                    if (selectedSeriesIdx != null && selectedPointIdx != null) {
                        val sIdx = selectedSeriesIdx!!
                        val pIdx = selectedPointIdx!!
                        if (sIdx < seriesList.size && pIdx < seriesList[sIdx].records.size) {
                            val series = seriesList[sIdx]
                            val record = series.records[pIdx]
                            val epoch = record.dateTime.toEpochSecond(ZoneOffset.UTC)
                            val normX = (epoch - minEpoch).toFloat() / timeRange
                            val screenX = normX * virtualWidth - scrollOffset
                            val screenY = ((globalMax - record.totalValue) / valRange * chartHeight).toFloat()

                            // Vertical dashed line
                            drawLine(
                                color = labelColor.copy(alpha = 0.4f),
                                start = Offset(screenX, 0f),
                                end = Offset(screenX, chartHeight),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                            )

                            // Point highlight
                            drawCircle(color = Color.White, radius = 7f, center = Offset(screenX, screenY))
                            drawCircle(color = series.color, radius = 5f, center = Offset(screenX, screenY))

                            // Tooltip box
                            val tooltipDateFmt = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm")
                            val tooltipStr = "${series.accountName}: ${currencyFormat.format(record.totalValue)}  ${record.dateTime.format(tooltipDateFmt)}"
                            val paint = android.graphics.Paint().apply {
                                textSize = 11.dp.toPx()
                                isAntiAlias = true
                            }
                            val textWidth = paint.measureText(tooltipStr)
                            val tooltipPadH = 8.dp.toPx()
                            val tooltipPadV = 5.dp.toPx()
                            val tooltipW = textWidth + tooltipPadH * 2
                            val tooltipH = paint.textSize + tooltipPadV * 2
                            val tooltipX = (screenX - tooltipW / 2).coerceIn(0f, chartWidth - tooltipW)
                            val tooltipY = (screenY - tooltipH - 12.dp.toPx()).coerceAtLeast(0f)

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
                                paint.apply { color = tooltipTextColor.hashCode() }
                            )
                        }
                    }
                }

                // X-axis date labels (viewport-aware)
                val leftFraction = (scrollOffset / virtualWidth).coerceIn(0f, 1f)
                val centerFraction = ((scrollOffset + chartWidth / 2) / virtualWidth).coerceIn(0f, 1f)
                val rightFraction = ((scrollOffset + chartWidth) / virtualWidth).coerceIn(0f, 1f)

                listOf(
                    0f to leftFraction,
                    chartWidth / 2 to centerFraction,
                    chartWidth to rightFraction
                ).forEach { (screenX, fraction) ->
                    val epoch = minEpoch + (fraction * timeRange).toLong()
                    val dateTime = LocalDateTime.ofEpochSecond(epoch, 0, ZoneOffset.UTC)
                    drawContext.canvas.nativeCanvas.drawText(
                        dateTime.format(dateFormatter),
                        screenX,
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
