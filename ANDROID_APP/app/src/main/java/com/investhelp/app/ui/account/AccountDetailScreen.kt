package com.investhelp.app.ui.account

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.data.local.entity.AccountPerformanceEntity
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountDetailScreen(
    accountId: Long,
    viewModel: AccountViewModel,
    onEditAccount: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(accountId) {
        viewModel.loadAccount(accountId)
    }

    val account by viewModel.selectedAccount.collectAsStateWithLifecycle()
    val performanceRecords by viewModel.performanceRecords.collectAsStateWithLifecycle()
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(account?.name ?: "Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onEditAccount) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            account?.let { acc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (acc.description.isNotBlank()) {
                            Text(
                                text = acc.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Initial Value", style = MaterialTheme.typography.labelLarge)
                                Text(
                                    currencyFormat.format(acc.initialValue),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            acc.lastValue?.let { lastVal ->
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Last Value", style = MaterialTheme.typography.labelLarge)
                                    Text(
                                        currencyFormat.format(lastVal),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Performance Chart
            if (performanceRecords.size >= 2) {
                Text(
                    "Performance Chart",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                AccountPerformanceChart(
                    records = performanceRecords,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Performance Records Table
            if (performanceRecords.isNotEmpty()) {
                Text(
                    "Performance Records (${performanceRecords.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                val altColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp, vertical = 8.dp)
                        )
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                        Text(
                            text = "Value",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp, vertical = 8.dp)
                        )
                        VerticalDivider(modifier = Modifier.fillMaxHeight())
                        Text(
                            text = "Note",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp, vertical = 8.dp)
                        )
                    }
                    HorizontalDivider()

                    performanceRecords.reversed().forEachIndexed { index, record ->
                        val rowBg = if (index % 2 == 1) altColor else Color.Transparent
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                                .background(rowBg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = record.date.format(dateFormatter),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                            )
                            VerticalDivider(modifier = Modifier.fillMaxHeight())
                            Text(
                                text = currencyFormat.format(record.totalValue),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                            )
                            VerticalDivider(modifier = Modifier.fillMaxHeight())
                            Text(
                                text = record.note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 6.dp, vertical = 6.dp),
                                maxLines = 2
                            )
                        }
                        HorizontalDivider()
                    }
                }
            } else {
                Text(
                    "No performance records yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AccountPerformanceChart(
    records: List<AccountPerformanceEntity>,
    modifier: Modifier = Modifier
) {
    val chartColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tooltipBg = MaterialTheme.colorScheme.inverseSurface
    val tooltipTextColor = MaterialTheme.colorScheme.inverseOnSurface

    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val dateFormatter = DateTimeFormatter.ofPattern("MM/dd")

    val allValues = records.map { it.totalValue }
    val globalMin = allValues.min() * 0.998
    val globalMax = allValues.max() * 1.002
    val valRange = (globalMax - globalMin).let { if (it < 0.01) 1.0 else it }

    val allEpochs = records.map { it.date.toEpochDay() }
    val minEpoch = allEpochs.min()
    val maxEpoch = allEpochs.max()
    val timeRange = (maxEpoch - minEpoch).let { if (it < 1L) 1L else it }

    var zoom by remember(records) { mutableStateOf(1f) }
    var scrollOffset by remember(records) { mutableStateOf(0f) }
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

    var selectedPointIdx by remember(records) { mutableStateOf<Int?>(null) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp, bottom = 24.dp, start = 48.dp, end = 12.dp)
                .transformable(transformState)
                .pointerInput(records) {
                    detectTapGestures(
                        onTap = { offset ->
                            val cw = size.width.toFloat()
                            val virtualWidth = cw * zoom
                            var bestDist = Float.MAX_VALUE
                            var bestPoint = -1

                            records.forEachIndexed { pIdx, record ->
                                val epoch = record.date.toEpochDay()
                                val normX = (epoch - minEpoch).toFloat() / timeRange
                                val screenX = normX * virtualWidth - scrollOffset
                                val dist = abs(offset.x - screenX)
                                if (dist < bestDist) {
                                    bestDist = dist
                                    bestPoint = pIdx
                                }
                            }

                            if (bestDist < 30.dp.toPx()) {
                                selectedPointIdx = if (selectedPointIdx == bestPoint) null else bestPoint
                            } else {
                                selectedPointIdx = null
                            }
                        },
                        onDoubleTap = {
                            zoom = 1f
                            scrollOffset = 0f
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

            clipRect(0f, 0f, chartWidth, chartHeight) {
                val path = Path()
                val fillPath = Path()

                val points = records.map { record ->
                    val epoch = record.date.toEpochDay()
                    val normX = (epoch - minEpoch).toFloat() / timeRange
                    val screenX = normX * virtualWidth - scrollOffset
                    val screenY = ((globalMax - record.totalValue) / valRange * chartHeight).toFloat()
                    Offset(screenX, screenY)
                }

                points.forEachIndexed { i, pt ->
                    if (i == 0) {
                        path.moveTo(pt.x, pt.y)
                        fillPath.moveTo(pt.x, chartHeight)
                        fillPath.lineTo(pt.x, pt.y)
                    } else {
                        path.lineTo(pt.x, pt.y)
                        fillPath.lineTo(pt.x, pt.y)
                    }
                }

                if (points.isNotEmpty()) {
                    fillPath.lineTo(points.last().x, chartHeight)
                    fillPath.close()
                    drawPath(fillPath, color = chartColor.copy(alpha = 0.1f))
                }

                drawPath(
                    path = path,
                    color = chartColor,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                // Data points
                records.forEachIndexed { idx, record ->
                    val epoch = record.date.toEpochDay()
                    val normX = (epoch - minEpoch).toFloat() / timeRange
                    val screenX = normX * virtualWidth - scrollOffset
                    val screenY = ((globalMax - record.totalValue) / valRange * chartHeight).toFloat()
                    val hasNote = record.note.isNotBlank()
                    if (hasNote) {
                        drawCircle(color = Color.White, radius = 9f, center = Offset(screenX, screenY))
                        drawCircle(color = chartColor, radius = 7f, center = Offset(screenX, screenY))
                    } else {
                        drawCircle(color = chartColor, radius = 4f, center = Offset(screenX, screenY))
                    }
                }

                // Tooltip
                if (selectedPointIdx != null && selectedPointIdx!! < records.size) {
                    val pIdx = selectedPointIdx!!
                    val record = records[pIdx]
                    val epoch = record.date.toEpochDay()
                    val normX = (epoch - minEpoch).toFloat() / timeRange
                    val screenX = normX * virtualWidth - scrollOffset
                    val screenY = ((globalMax - record.totalValue) / valRange * chartHeight).toFloat()

                    drawLine(
                        color = labelColor.copy(alpha = 0.4f),
                        start = Offset(screenX, 0f),
                        end = Offset(screenX, chartHeight),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
                    )

                    drawCircle(color = Color.White, radius = 7f, center = Offset(screenX, screenY))
                    drawCircle(color = chartColor, radius = 5f, center = Offset(screenX, screenY))

                    val tooltipDateFmt = DateTimeFormatter.ofPattern("MM/dd/yy")
                    val line1 = "${currencyFormat.format(record.totalValue)}  ${record.date.format(tooltipDateFmt)}"
                    val hasNote = record.note.isNotBlank()
                    val paint = android.graphics.Paint().apply {
                        textSize = 11.dp.toPx()
                        isAntiAlias = true
                    }
                    val line1Width = paint.measureText(line1)
                    val tooltipPadH = 8.dp.toPx()
                    val tooltipPadV = 5.dp.toPx()
                    val lineHeight = paint.textSize * 1.3f

                    val noteText = if (hasNote) record.note else null
                    val noteWidth = noteText?.let { paint.measureText(it) } ?: 0f
                    val tooltipW = maxOf(line1Width, noteWidth) + tooltipPadH * 2
                    val tooltipH = if (hasNote) {
                        tooltipPadV * 2 + lineHeight + paint.textSize
                    } else {
                        paint.textSize + tooltipPadV * 2
                    }
                    val tooltipX = (screenX - tooltipW / 2).coerceIn(0f, chartWidth - tooltipW)
                    val tooltipY = (screenY - tooltipH - 12.dp.toPx()).coerceAtLeast(0f)

                    drawRoundRect(
                        color = tooltipBg,
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(tooltipW, tooltipH),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                    drawContext.canvas.nativeCanvas.drawText(
                        line1,
                        tooltipX + tooltipPadH,
                        tooltipY + tooltipPadV + paint.textSize * 0.85f,
                        paint.apply { color = tooltipTextColor.hashCode() }
                    )
                    if (hasNote && noteText != null) {
                        val notePaint = android.graphics.Paint().apply {
                            textSize = 11.dp.toPx()
                            isAntiAlias = true
                            color = tooltipTextColor.hashCode()
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                        }
                        drawContext.canvas.nativeCanvas.drawText(
                            noteText,
                            tooltipX + tooltipPadH,
                            tooltipY + tooltipPadV + lineHeight + paint.textSize * 0.85f,
                            notePaint
                        )
                    }
                }
            }

            // X-axis date labels
            val leftFraction = (scrollOffset / virtualWidth).coerceIn(0f, 1f)
            val centerFraction = ((scrollOffset + chartWidth / 2) / virtualWidth).coerceIn(0f, 1f)
            val rightFraction = ((scrollOffset + chartWidth) / virtualWidth).coerceIn(0f, 1f)

            listOf(
                0f to leftFraction,
                chartWidth / 2 to centerFraction,
                chartWidth to rightFraction
            ).forEach { (screenX, fraction) ->
                val epochDay = minEpoch + (fraction * timeRange).toLong()
                val labelDate = LocalDate.ofEpochDay(epochDay)
                drawContext.canvas.nativeCanvas.drawText(
                    labelDate.format(dateFormatter),
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
