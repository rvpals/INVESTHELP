package com.investhelp.app.ui.positions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.ui.components.CollapsibleCard
import com.investhelp.app.ui.item.ItemViewModel
import java.nio.ByteBuffer
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun PositionDetailScreen(
    viewModel: ItemViewModel,
    onNavigateToItem: (String) -> Unit
) {
    val allItems by viewModel.allItems.collectAsStateWithLifecycle()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }

    data class TabInfo(val title: String, val icon: ImageVector)
    val tabs = listOf(
        TabInfo("STOCK", Icons.Default.ShowChart),
        TabInfo("ETF", Icons.Default.TrendingUp),
        TabInfo("Analysis", Icons.Default.Analytics),
        TabInfo("Dividend", Icons.Default.Payments)
    )

    val stockItems = remember(allItems) {
        allItems.filter { it.type == InvestmentType.Stock }
    }
    val etfItems = remember(allItems) {
        allItems.filter { it.type == InvestmentType.ETF }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTabIndex == index
                val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTabIndex = index }
                        .background(bgColor)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(tab.icon, contentDescription = tab.title, modifier = Modifier.size(18.dp), tint = contentColor)
                        Text(tab.title, style = MaterialTheme.typography.labelMedium, color = contentColor, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        when (selectedTabIndex) {
            0 -> PositionTable(items = stockItems, onItemClick = onNavigateToItem)
            1 -> PositionTable(items = etfItems, onItemClick = onNavigateToItem)
            2 -> AnalysisTab(stockItems = stockItems, etfItems = etfItems, onItemClick = onNavigateToItem)
            3 -> DividendTab(allItems = allItems, onItemClick = onNavigateToItem)
        }
    }
}

private enum class PositionSortField {
    TICKER, SHARES, PRICE, VALUE, DAY_GL
}

@Composable
private fun PositionTable(
    items: List<InvestmentItemEntity>,
    onItemClick: (String) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val sharesFormat = DecimalFormat("#,##0.##")
    val priceFormat = DecimalFormat("#,##0.00")
    val dividerColor = MaterialTheme.colorScheme.outline

    var sortField by rememberSaveable { mutableStateOf(PositionSortField.VALUE.name) }
    var sortAsc by rememberSaveable { mutableStateOf(false) }

    val sortedItems = remember(items, sortField, sortAsc) {
        val field = PositionSortField.valueOf(sortField)
        val comparator: Comparator<InvestmentItemEntity> = when (field) {
            PositionSortField.TICKER -> compareBy { it.ticker }
            PositionSortField.SHARES -> compareBy { it.quantity }
            PositionSortField.PRICE -> compareBy { it.currentPrice }
            PositionSortField.VALUE -> compareBy { it.value }
            PositionSortField.DAY_GL -> compareBy { it.dayGainLoss }
        }
        if (sortAsc) items.sortedWith(comparator) else items.sortedWith(comparator.reversed())
    }

    fun onHeaderClick(field: PositionSortField) {
        if (sortField == field.name) {
            sortAsc = !sortAsc
        } else {
            sortField = field.name
            sortAsc = field == PositionSortField.TICKER
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (sortedItems.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No positions",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val horizontalScroll = rememberScrollState()
            val verticalScroll = rememberScrollState()
            val currentSortField = PositionSortField.valueOf(sortField)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(verticalScroll)
                    .horizontalScroll(horizontalScroll)
            ) {
                HorizontalDivider(color = dividerColor)
                Row(
                    modifier = Modifier
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    SortableHeader("Ticker", PositionSortField.TICKER, currentSortField, sortAsc, 100, TextAlign.Start) { onHeaderClick(it) }
                    VerticalDivider(color = dividerColor)
                    SortableHeader("Shares", PositionSortField.SHARES, currentSortField, sortAsc, 70, TextAlign.End) { onHeaderClick(it) }
                    VerticalDivider(color = dividerColor)
                    SortableHeader("Price", PositionSortField.PRICE, currentSortField, sortAsc, 80, TextAlign.End) { onHeaderClick(it) }
                    VerticalDivider(color = dividerColor)
                    SortableHeader("Value", PositionSortField.VALUE, currentSortField, sortAsc, 90, TextAlign.End) { onHeaderClick(it) }
                    VerticalDivider(color = dividerColor)
                    SortableHeader("Day G/L", PositionSortField.DAY_GL, currentSortField, sortAsc, 90, TextAlign.End) { onHeaderClick(it) }
                }
                HorizontalDivider(thickness = 2.dp, color = dividerColor)

                val altColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                sortedItems.forEachIndexed { index, item ->
                    val dayColor = if (item.dayGainLoss >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    val daySign = if (item.dayGainLoss > 0) "+" else ""

                    Row(
                        modifier = Modifier
                            .height(IntrinsicSize.Min)
                            .background(if (index % 2 == 1) altColor else Color.Transparent)
                            .clickable { onItemClick(item.ticker) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Row(
                            modifier = Modifier.width(100.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TickerIcon(ticker = item.ticker, name = item.name, logo = item.logo)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = item.ticker,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        VerticalDivider(color = dividerColor)
                        Text(
                            text = sharesFormat.format(item.quantity),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(70.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider(color = dividerColor)
                        Text(
                            text = priceFormat.format(item.currentPrice),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(80.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider(color = dividerColor)
                        Text(
                            text = currencyFormat.format(item.value),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.width(90.dp),
                            textAlign = TextAlign.End
                        )
                        VerticalDivider(color = dividerColor)
                        Text(
                            text = "$daySign${currencyFormat.format(item.dayGainLoss)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = dayColor,
                            modifier = Modifier.width(90.dp).padding(end = 4.dp),
                            textAlign = TextAlign.End
                        )
                    }
                    HorizontalDivider(color = dividerColor)
                }
            }
        }
    }
}

@Composable
private fun SortableHeader(
    label: String,
    field: PositionSortField,
    currentField: PositionSortField,
    ascending: Boolean,
    widthDp: Int,
    align: TextAlign,
    onClick: (PositionSortField) -> Unit
) {
    val isActive = field == currentField
    Row(
        modifier = Modifier
            .width(widthDp.dp)
            .clickable { onClick(field) }
            .padding(vertical = 4.dp),
        horizontalArrangement = if (align == TextAlign.End) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isActive && align == TextAlign.End) {
            Icon(
                imageVector = if (ascending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface
        )
        if (isActive && align != TextAlign.End) {
            Icon(
                imageVector = if (ascending) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private val pieColors = listOf(
    Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC04), Color(0xFF34A853),
    Color(0xFFFF6D00), Color(0xFF46BDC6), Color(0xFFAB47BC), Color(0xFF7CB342),
    Color(0xFFE91E63), Color(0xFF00ACC1), Color(0xFFFF7043), Color(0xFF5C6BC0)
)

private val tickerIconColors = listOf(
    Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC04), Color(0xFF34A853),
    Color(0xFFFF6D01), Color(0xFF46BDC6), Color(0xFF7B1FA2), Color(0xFFD81B60),
    Color(0xFF00897B), Color(0xFF5C6BC0), Color(0xFFFFA000), Color(0xFF8D6E63)
)

@Composable
private fun TickerIcon(ticker: String, name: String, logo: ByteArray? = null) {
    val context = LocalContext.current
    val hash = ticker.hashCode()
    val baseColor = tickerIconColors[(hash and 0x7FFFFFFF) % tickerIconColors.size]
    val gradient = Brush.linearGradient(
        colors = listOf(
            baseColor.copy(alpha = 0.85f),
            baseColor,
            baseColor.copy(
                red = baseColor.red * 0.65f,
                green = baseColor.green * 0.65f,
                blue = baseColor.blue * 0.65f
            )
        )
    )

    Box(
        modifier = Modifier
            .size(30.dp)
            .shadow(3.dp, RoundedCornerShape(8.dp))
            .background(gradient, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = (if (name != ticker) name else ticker).first().uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        val imageData: Any = if (logo != null) ByteBuffer.wrap(logo) else "https://companiesmarketcap.com/img/company-logos/64/${ticker.lowercase()}.webp"
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageData)
                .crossfade(true)
                .build(),
            contentDescription = "$ticker logo",
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
        )
    }
}

@Composable
private fun AnalysisTab(
    stockItems: List<InvestmentItemEntity>,
    etfItems: List<InvestmentItemEntity>,
    onItemClick: (String) -> Unit
) {
    var stockPinned by rememberSaveable { mutableStateOf(true) }
    var etfPinned by rememberSaveable { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CollapsibleCard(
            title = "Stock",
            pinned = stockPinned,
            onPinToggle = { stockPinned = it }
        ) {
            if (stockItems.isNotEmpty()) {
                AnalysisPieChartWithTable(items = stockItems, onItemClick = onItemClick)
            } else {
                Text(
                    "No stock positions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        CollapsibleCard(
            title = "ETF",
            pinned = etfPinned,
            onPinToggle = { etfPinned = it }
        ) {
            if (etfItems.isNotEmpty()) {
                AnalysisPieChartWithTable(items = etfItems, onItemClick = onItemClick)
            } else {
                Text(
                    "No ETF positions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnalysisPieChartWithTable(
    items: List<InvestmentItemEntity>,
    onItemClick: (String) -> Unit
) {
    val sortedItems = remember(items) { items.sortedByDescending { it.value } }
    val totalValue = sortedItems.sumOf { it.value }
    val sharesFormat = DecimalFormat("#,##0.##")
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val largestIndex = 0

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(12.dp))

        // 3D Pie chart
        Canvas(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .aspectRatio(1f)
        ) {
            val diameter = size.minDimension * 0.85f
            val radius = diameter / 2f
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val explodeOffset = radius * 0.12f
            val depth = radius * 0.08f

            // Draw 3D side (bottom layer) first
            var startAngle = -90f
            sortedItems.forEachIndexed { index, item ->
                val sweep = (item.value / totalValue * 360.0).toFloat()
                val midAngle = Math.toRadians((startAngle + sweep / 2f).toDouble())
                val offsetX = if (index == largestIndex) (explodeOffset * cos(midAngle)).toFloat() else 0f
                val offsetY = if (index == largestIndex) (explodeOffset * sin(midAngle)).toFloat() else 0f

                val sliceColor = pieColors[index % pieColors.size]
                val darkColor = sliceColor.copy(
                    red = sliceColor.red * 0.55f,
                    green = sliceColor.green * 0.55f,
                    blue = sliceColor.blue * 0.55f
                )

                val left = centerX - radius + offsetX
                val top = centerY - radius + offsetY + depth
                drawArc(
                    color = darkColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(left, top),
                    size = Size(diameter, diameter)
                )
                startAngle += sweep
            }

            // Draw top face
            startAngle = -90f
            sortedItems.forEachIndexed { index, item ->
                val sweep = (item.value / totalValue * 360.0).toFloat()
                val midAngle = Math.toRadians((startAngle + sweep / 2f).toDouble())
                val offsetX = if (index == largestIndex) (explodeOffset * cos(midAngle)).toFloat() else 0f
                val offsetY = if (index == largestIndex) (explodeOffset * sin(midAngle)).toFloat() else 0f

                val sliceColor = pieColors[index % pieColors.size]

                val left = centerX - radius + offsetX
                val top = centerY - radius + offsetY
                drawArc(
                    color = sliceColor,
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    topLeft = Offset(left, top),
                    size = Size(diameter, diameter)
                )
                startAngle += sweep
            }

            // Draw percentage labels inside slices
            startAngle = -90f
            sortedItems.forEachIndexed { index, item ->
                val sweep = (item.value / totalValue * 360.0).toFloat()
                if (sweep > 18f) {
                    val midAngle = Math.toRadians((startAngle + sweep / 2f).toDouble())
                    val offsetX = if (index == largestIndex) (explodeOffset * cos(midAngle)).toFloat() else 0f
                    val offsetY = if (index == largestIndex) (explodeOffset * sin(midAngle)).toFloat() else 0f
                    val labelRadius = radius * 0.6f
                    val lx = centerX + offsetX + (labelRadius * cos(midAngle)).toFloat()
                    val ly = centerY + offsetY + (labelRadius * sin(midAngle)).toFloat()

                    val pct = item.value / totalValue * 100.0
                    drawContext.canvas.nativeCanvas.drawText(
                        String.format("%.1f%%", pct),
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

        Spacer(modifier = Modifier.height(16.dp))

        // Data table
        val dividerColor = MaterialTheme.colorScheme.outline
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = dividerColor)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Ticker",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1.2f)
                )
                VerticalDivider(color = dividerColor)
                Text(
                    text = "Shares",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.8f),
                    textAlign = TextAlign.End
                )
                VerticalDivider(color = dividerColor)
                Text(
                    text = "Value",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.End
                )
                VerticalDivider(color = dividerColor)
                Text(
                    text = "%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(0.6f),
                    textAlign = TextAlign.End
                )
            }
            HorizontalDivider(thickness = 2.dp, color = dividerColor)

            val altColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            sortedItems.forEachIndexed { index, item ->
                val pct = if (totalValue > 0) item.value / totalValue * 100 else 0.0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .background(if (index % 2 == 1) altColor else Color.Transparent)
                        .clickable { onItemClick(item.ticker) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(pieColors[index % pieColors.size])
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.ticker,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (index == largestIndex) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1.2f)
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = sharesFormat.format(item.quantity),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = currencyFormat.format(item.value),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                    VerticalDivider(color = dividerColor)
                    Text(
                        text = String.format("%.1f%%", pct),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.6f),
                        textAlign = TextAlign.End
                    )
                }
                HorizontalDivider(color = dividerColor)
            }
        }
    }
}

// --- Dividend Tab ---

private enum class DividendSortOption(val label: String) {
    AnnualDividend("Annual Dividend"),
    DividendRate("Div/Share"),
    Ticker("Ticker"),
    Shares("Shares")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DividendTab(
    allItems: List<InvestmentItemEntity>,
    onItemClick: (String) -> Unit
) {
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)
    val stockDividends = allItems.filter { it.type == InvestmentType.Stock && it.dividendRate > 0 && it.quantity > 0 }
    val etfDividends = allItems.filter { it.type == InvestmentType.ETF && it.dividendRate > 0 && it.quantity > 0 }
    val totalAnnual = (stockDividends + etfDividends).sumOf { it.dividendRate * it.quantity }

    if (stockDividends.isEmpty() && etfDividends.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("No dividend-paying positions", style = MaterialTheme.typography.bodyLarge)
            Text("Dividend data is fetched during price refresh", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total Annual Dividend Income", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = currencyFormat.format(totalAnnual),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0)
                    )
                }
            }
        }

        if (stockDividends.isNotEmpty()) {
            item {
                DividendPieCard(
                    title = "Stock",
                    items = stockDividends,
                    currencyFormat = currencyFormat,
                    onItemClick = onItemClick
                )
            }
        }

        if (etfDividends.isNotEmpty()) {
            item {
                DividendPieCard(
                    title = "ETF",
                    items = etfDividends,
                    currencyFormat = currencyFormat,
                    onItemClick = onItemClick
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DividendPieCard(
    title: String,
    items: List<InvestmentItemEntity>,
    currencyFormat: NumberFormat,
    onItemClick: (String) -> Unit
) {
    val dividendData = items.map { Triple(it.ticker, it.dividendRate * it.quantity, it) }
    val totalDividend = dividendData.sumOf { it.second }
    val maxIndex = if (dividendData.isNotEmpty()) dividendData.indices.maxByOrNull { dividendData[it].second } ?: 0 else 0

    var sortOption by remember { mutableStateOf(DividendSortOption.AnnualDividend) }
    var sortAsc by remember { mutableStateOf(false) }

    val sortedData = when (sortOption) {
        DividendSortOption.AnnualDividend -> if (sortAsc) dividendData.sortedBy { it.second } else dividendData.sortedByDescending { it.second }
        DividendSortOption.DividendRate -> if (sortAsc) dividendData.sortedBy { it.third.dividendRate } else dividendData.sortedByDescending { it.third.dividendRate }
        DividendSortOption.Ticker -> if (sortAsc) dividendData.sortedBy { it.first } else dividendData.sortedByDescending { it.first }
        DividendSortOption.Shares -> if (sortAsc) dividendData.sortedBy { it.third.quantity } else dividendData.sortedByDescending { it.third.quantity }
    }

    val priceFormat = remember { DecimalFormat("#,##0.00") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Annual: ${currencyFormat.format(totalDividend)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF1565C0),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .aspectRatio(1f)
                    .align(Alignment.CenterHorizontally)
            ) {
                val explodeOffset = 12.dp.toPx()
                val diameter = size.minDimension - explodeOffset * 2
                val center = Offset(size.width / 2f, size.height / 2f)
                var startAngle = -90f

                dividendData.forEachIndexed { index, (_, annualDiv, _) ->
                    val sweep = (annualDiv / totalDividend * 360.0).toFloat()
                    val midAngle = Math.toRadians((startAngle + sweep / 2).toDouble())

                    val offset = if (index == maxIndex) {
                        Offset(
                            (cos(midAngle) * explodeOffset).toFloat(),
                            (sin(midAngle) * explodeOffset).toFloat()
                        )
                    } else Offset.Zero

                    val topLeft = Offset(
                        center.x - diameter / 2f + offset.x,
                        center.y - diameter / 2f + offset.y
                    )

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
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dividendData.forEachIndexed { index, (ticker, annualDiv, _) ->
                    val pct = if (totalDividend > 0) annualDiv / totalDividend * 100 else 0.0
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(pieColors[index % pieColors.size])
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$ticker ${"%.1f".format(pct)}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (index == maxIndex) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                DividendSortOption.entries.forEach { opt ->
                    val isSelected = sortOption == opt
                    TextButton(
                        onClick = {
                            if (isSelected) sortAsc = !sortAsc
                            else { sortOption = opt; sortAsc = false }
                        }
                    ) {
                        Text(
                            text = opt.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isSelected) {
                            Icon(
                                if (sortAsc) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Text("Ticker", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                Text("Shares", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Div/Share", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Annual", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f), color = Color(0xFF1565C0))
                Text("%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.6f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            sortedData.forEachIndexed { index, (ticker, annualDiv, item) ->
                val pct = if (totalDividend > 0) annualDiv / totalDividend * 100 else 0.0
                val bgColor = if (index % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor)
                        .clickable { onItemClick(ticker) }
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ticker,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = priceFormat.format(item.quantity),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$${priceFormat.format(item.dividendRate)}",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = currencyFormat.format(annualDiv),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1565C0),
                        modifier = Modifier.weight(1.2f)
                    )
                    Text(
                        text = "${"%.1f".format(pct)}%",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(0.6f)
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            }
        }
    }
}
