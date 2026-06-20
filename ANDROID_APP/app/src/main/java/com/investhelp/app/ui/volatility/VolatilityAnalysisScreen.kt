package com.investhelp.app.ui.volatility

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.investhelp.app.model.InvestmentType
import java.nio.ByteBuffer
import java.text.NumberFormat
import java.util.Locale

private val LABEL_ORDER = listOf("Low", "Moderate", "High", "Very High")

private val LABEL_COLORS = mapOf(
    "Low" to Color(0xFF388E3C),
    "Moderate" to Color(0xFFF57C00),
    "High" to Color(0xFFE64A19),
    "Very High" to Color(0xFFB71C1C)
)

private val LABEL_RANGES = mapOf(
    "Low" to "< 15%",
    "Moderate" to "15–30%",
    "High" to "30–60%",
    "Very High" to "> 60%"
)

private val tickerIconColors = listOf(
    Color(0xFF4285F4), Color(0xFFEA4335), Color(0xFFFBBC04), Color(0xFF34A853),
    Color(0xFFFF6D01), Color(0xFF46BDC6), Color(0xFF7B1FA2), Color(0xFFD81B60),
    Color(0xFF00897B), Color(0xFF5C6BC0), Color(0xFFFFA000), Color(0xFF8D6E63)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolatilityAnalysisScreen(
    viewModel: VolatilityAnalysisViewModel,
    onNavigateToItem: (String) -> Unit,
    onBack: () -> Unit
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val isInitialLoading by viewModel.isInitialLoading.collectAsStateWithLifecycle()

    val stocks = items.filter { it.type == InvestmentType.Stock }
    val etfs = items.filter { it.type == InvestmentType.ETF }

    var selectedTab by remember { mutableIntStateOf(0) }
    val currencyFormat = NumberFormat.getCurrencyInstance(Locale.US)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Volatility Analysis") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Stocks (${stocks.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("ETFs (${etfs.size})") }
                )
            }

            if (isInitialLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Loading positions…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                val currentList = if (selectedTab == 0) stocks else etfs
                val total = currentList.size
                val loaded = currentList.count { !it.loading }

                if (loaded < total) {
                    LinearProgressIndicator(
                        progress = { if (total > 0) loaded.toFloat() / total else 0f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Fetching $loaded / $total…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                if (currentList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No positions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    VolatilityGroupedList(
                        items = currentList,
                        currencyFormat = currencyFormat,
                        onNavigateToItem = onNavigateToItem
                    )
                }
            }
        }
    }
}

@Composable
private fun VolatilityGroupedList(
    items: List<PositionVolatilityItem>,
    currencyFormat: NumberFormat,
    onNavigateToItem: (String) -> Unit
) {
    val loadedItems = items.filter { it.data != null }
    val loadingItems = items.filter { it.loading }
    val errorItems = items.filter { !it.loading && it.data == null && it.error != null }

    val grouped = LABEL_ORDER.associateWith { label ->
        loadedItems.filter { it.data?.volatilityLabel == label }
            .sortedBy { it.data?.annualizedVolPct ?: 0.0 }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        LABEL_ORDER.forEach { label ->
            val group = grouped[label] ?: emptyList()
            if (group.isNotEmpty()) {
                val color = LABEL_COLORS[label] ?: Color.Gray
                val range = LABEL_RANGES[label] ?: ""
                item(key = "header_$label") {
                    GroupHeader(label = label, range = range, count = group.size, color = color)
                }
                items(group, key = { it.ticker }) { item ->
                    VolatilityRow(
                        item = item,
                        currencyFormat = currencyFormat,
                        onClick = { onNavigateToItem(item.ticker) }
                    )
                }
            }
        }

        if (loadingItems.isNotEmpty()) {
            item(key = "header_loading") {
                GroupHeader(
                    label = "Loading",
                    range = "",
                    count = loadingItems.size,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            items(loadingItems, key = { "loading_${it.ticker}" }) { item ->
                LoadingRow(ticker = item.ticker)
            }
        }

        if (errorItems.isNotEmpty()) {
            item(key = "header_error") {
                GroupHeader(label = "Failed", range = "", count = errorItems.size, color = MaterialTheme.colorScheme.error)
            }
            items(errorItems, key = { "error_${it.ticker}" }) { item ->
                ErrorRow(ticker = item.ticker, error = item.error ?: "Error")
            }
        }
    }
}

@Composable
private fun GroupHeader(label: String, range: String, count: Int, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(50))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        if (range.isNotBlank()) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = range,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            text = "$count",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun VolatilityRow(
    item: PositionVolatilityItem,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    val data = item.data ?: return
    val labelColor = LABEL_COLORS[data.volatilityLabel] ?: Color.Gray
    val positionValue = data.currentPrice * data.shares

    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TickerIcon(
                ticker = item.ticker,
                name = item.companyName ?: item.ticker,
                logo = item.logo
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.ticker,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!item.companyName.isNullOrBlank() && item.companyName != item.ticker) {
                    Text(
                        item.companyName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${"%.1f".format(data.annualizedVolPct)}%",
                    modifier = Modifier
                        .background(labelColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = labelColor
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = currencyFormat.format(positionValue),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(start = 58.dp))
}

@Composable
private fun LoadingRow(ticker: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(ticker, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(
            "Fetching…",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    HorizontalDivider()
}

@Composable
private fun ErrorRow(ticker: String, error: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(ticker, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text(
            "Failed",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
    HorizontalDivider()
}

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
            .size(36.dp)
            .shadow(3.dp, RoundedCornerShape(10.dp))
            .background(gradient, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = (if (name != ticker) name else ticker).first().uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        val imageData: Any = if (logo != null) ByteBuffer.wrap(logo) else
            "https://companiesmarketcap.com/img/company-logos/64/${ticker.lowercase()}.webp"
        AsyncImage(
            model = ImageRequest.Builder(context).data(imageData).crossfade(true).build(),
            contentDescription = "$ticker logo",
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
        )
    }
}
