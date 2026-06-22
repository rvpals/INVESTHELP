# Correlation Matrix Feature — Claude Code Implementation Plan

## Overview
Add a "Correlation Matrix" screen to the existing Android investment tracking app.
Shows pairwise Pearson correlations between all portfolio holdings using 1 year of
daily closing prices, plus correlation vs. S&P 500 as benchmark. Match all existing
architecture: Hilt DI, MVVM + Repository, Jetpack Compose + Material 3, Kotlin
coroutines/Flow.

---

## Files to Create / Modify

### New files
```
ANDROID_APP/app/src/main/java/com/investhelp/app/
  util/CorrelationUtils.kt
  data/repository/CorrelationRepository.kt          (interface)
  data/repository/CorrelationRepositoryImpl.kt      (implementation)
  ui/correlation/CorrelationMatrixUiState.kt
  ui/correlation/CorrelationMatrixViewModel.kt
  ui/correlation/CorrelationMatrixScreen.kt

ANDROID_APP/app/src/test/java/com/investhelp/app/
  util/CorrelationUtilsTest.kt
```

### Modified files
```
ANDROID_APP/app/src/main/java/com/investhelp/app/
  ui/navigation/NavRoutes.kt          — add CorrelationMatrixRoute
  ui/navigation/InvestHelpNavHost.kt  — add composable<CorrelationMatrixRoute>
  MainActivity.kt                     — add DropdownMenuItem to hamburger menu
  di/RepositoryModule.kt              — bind CorrelationRepository
```

---

## Step 1 — NavRoutes.kt

File: `ui/navigation/NavRoutes.kt`

Add one line alongside the existing serializable route objects (e.g., after
`VolatilityAnalysisRoute`):

```kotlin
@Serializable object CorrelationMatrixRoute
```

---

## Step 2 — CorrelationUtils.kt

File: `util/CorrelationUtils.kt`

Pure Kotlin object — zero Android dependencies so it is unit-testable with JUnit.

```kotlin
package com.investhelp.app.util

import com.investhelp.app.data.remote.StockPriceService.HistoricalPrice
import kotlin.math.sqrt

object CorrelationUtils {

    // Aligns two price series to their shared trading dates (inner join).
    // Returns aligned close prices as Pair(listA, listB).
    fun alignPriceSeries(
        a: List<HistoricalPrice>,
        b: List<HistoricalPrice>
    ): Pair<List<Double>, List<Double>> {
        val mapB = b.associate { it.timestamp to it.close }
        val paired = a.mapNotNull { pa ->
            val cb = mapB[pa.timestamp] ?: return@mapNotNull null
            pa.close to cb
        }
        return paired.map { it.first } to paired.map { it.second }
    }

    // Converts aligned close prices to daily returns: (close[i] - close[i-1]) / close[i-1].
    // Requires at least 2 prices; returns empty list otherwise.
    fun dailyReturns(closes: List<Double>): List<Double> {
        if (closes.size < 2) return emptyList()
        return (1 until closes.size).map { i ->
            (closes[i] - closes[i - 1]) / closes[i - 1]
        }
    }

    // Pearson correlation (sample, n-1 denominator).
    // Returns Double.NaN if insufficient data or zero variance.
    fun pearson(returnsA: List<Double>, returnsB: List<Double>): Double {
        val n = minOf(returnsA.size, returnsB.size)
        if (n < 2) return Double.NaN
        val a = returnsA.take(n)
        val b = returnsB.take(n)
        val meanA = a.average()
        val meanB = b.average()
        val num = a.indices.sumOf { (a[it] - meanA) * (b[it] - meanB) }
        val stdA = sampleStdDev(a, meanA)
        val stdB = sampleStdDev(b, meanB)
        if (stdA == 0.0 || stdB == 0.0) return Double.NaN
        return num / ((n - 1) * stdA * stdB)
    }

    private fun sampleStdDev(values: List<Double>, mean: Double): Double {
        val n = values.size
        if (n < 2) return 0.0
        val variance = values.sumOf { (it - mean) * (it - mean) } / (n - 1)
        return sqrt(variance)
    }

    data class MatrixResult(
        val tickers: List<String>,                    // ordered ticker list
        val matrix: List<List<Double>>,               // N×N, symmetric, diagonal = 1.0
        val marketCorrelation: Map<String, Double>,   // ticker → corr with ^GSPC
        val failedTickers: List<String>,              // excluded due to fetch errors
        val updatedAt: Long                           // epoch ms
    )

    // Builds the full N×N correlation matrix and market correlations.
    // priceMap: ticker → aligned List<Double> of daily close prices (already date-aligned across all tickers).
    // marketPrices: aligned closes for ^GSPC (same date set).
    fun buildMatrix(
        priceMap: Map<String, List<Double>>,
        marketPrices: List<Double>
    ): MatrixResult {
        val tickers = priceMap.keys.toList()
        val returnMap = priceMap.mapValues { dailyReturns(it.value) }
        val marketReturns = dailyReturns(marketPrices)
        val n = tickers.size

        val matrix = List(n) { i ->
            List(n) { j ->
                when {
                    i == j -> 1.0
                    else -> pearson(returnMap[tickers[i]]!!, returnMap[tickers[j]]!!)
                }
            }
        }

        val marketCorrelation = tickers.associateWith { ticker ->
            pearson(returnMap[ticker]!!, marketReturns)
        }

        return MatrixResult(
            tickers = tickers,
            matrix = matrix,
            marketCorrelation = marketCorrelation,
            failedTickers = emptyList(),
            updatedAt = System.currentTimeMillis()
        )
    }

    // Average of all non-diagonal, upper-triangle pairs.
    fun averageCorrelation(result: MatrixResult): Double {
        val n = result.tickers.size
        if (n < 2) return Double.NaN
        val values = mutableListOf<Double>()
        for (i in 0 until n) {
            for (j in (i + 1) until n) {
                val v = result.matrix[i][j]
                if (!v.isNaN()) values.add(v)
            }
        }
        return if (values.isEmpty()) Double.NaN else values.average()
    }

    data class PairStat(val tickerA: String, val tickerB: String, val value: Double)

    // Highest correlation pair (upper triangle only, excludes diagonal).
    fun mostCorrelatedPair(result: MatrixResult): PairStat? {
        val n = result.tickers.size
        var best: PairStat? = null
        for (i in 0 until n) {
            for (j in (i + 1) until n) {
                val v = result.matrix[i][j]
                if (v.isNaN()) continue
                if (best == null || v > best.value)
                    best = PairStat(result.tickers[i], result.tickers[j], v)
            }
        }
        return best
    }

    // Ticker with lowest average correlation to all others.
    fun mostDiversifyingTicker(result: MatrixResult): Pair<String, Double>? {
        val n = result.tickers.size
        if (n < 2) return null
        return result.tickers.mapIndexed { i, ticker ->
            val others = (0 until n).filter { it != i }
                .mapNotNull { j -> result.matrix[i][j].takeIf { !it.isNaN() } }
            ticker to if (others.isEmpty()) Double.NaN else others.average()
        }.minByOrNull { it.second }
    }
}
```

---

## Step 3 — CorrelationRepository.kt (interface)

File: `data/repository/CorrelationRepository.kt`

```kotlin
package com.investhelp.app.data.repository

import com.investhelp.app.util.CorrelationUtils.MatrixResult

interface CorrelationRepository {
    // Fetches 1 year of daily prices for all tickers + ^GSPC, aligns dates,
    // computes and returns the full MatrixResult. Results are cached for 1 hour.
    // failedTickers in the result lists any ticker that could not be fetched.
    suspend fun buildCorrelationMatrix(tickers: List<String>): MatrixResult

    // Clears the in-memory cache, forcing a fresh fetch on next call.
    fun clearCache()
}
```

---

## Step 4 — CorrelationRepositoryImpl.kt

File: `data/repository/CorrelationRepositoryImpl.kt`

```kotlin
package com.investhelp.app.data.repository

import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.util.CorrelationUtils
import com.investhelp.app.util.CorrelationUtils.MatrixResult
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CorrelationRepositoryImpl @Inject constructor(
    private val stockPriceService: StockPriceService
) : CorrelationRepository {

    private val CACHE_TTL_MS = 60 * 60 * 1000L  // 1 hour
    private var cachedResult: MatrixResult? = null

    override fun clearCache() {
        cachedResult = null
    }

    override suspend fun buildCorrelationMatrix(tickers: List<String>): MatrixResult {
        // Return cache if still fresh.
        cachedResult?.let {
            if (System.currentTimeMillis() - it.updatedAt < CACHE_TTL_MS) return it
        }

        val allSymbols = (tickers + "^GSPC").distinct()

        // Fetch all symbols in parallel.
        val fetchResults: Map<String, Result<List<StockPriceService.HistoricalPrice>>> =
            coroutineScope {
                allSymbols.map { symbol ->
                    symbol to async {
                        runCatching { stockPriceService.fetchHistoricalPrices(symbol, 365) }
                    }
                }.associate { (symbol, deferred) -> symbol to deferred.await() }
            }

        val failed = fetchResults.filter { it.value.isFailure }.keys
            .filter { it != "^GSPC" }
        val succeeded = fetchResults.filter { it.value.isSuccess }

        val marketPrices = succeeded["^GSPC"]?.getOrNull()
        val tickerPrices: Map<String, List<StockPriceService.HistoricalPrice>> =
            tickers.filter { it !in failed }
                .associateWith { succeeded[it]?.getOrNull() ?: emptyList() }
                .filter { it.value.isNotEmpty() }

        // If fewer than 2 tickers have data, return a result with only failedTickers set.
        if (tickerPrices.size < 2) {
            return MatrixResult(
                tickers = emptyList(),
                matrix = emptyList(),
                marketCorrelation = emptyMap(),
                failedTickers = failed.toList(),
                updatedAt = System.currentTimeMillis()
            )
        }

        // Inner-join all ticker price series to common trading dates.
        // Use the dates from the ticker with the fewest data points as the basis.
        val baseline = tickerPrices.values.minByOrNull { it.size }!!
        val commonTimestamps = run {
            var common = baseline.map { it.timestamp }.toSet()
            for (series in tickerPrices.values) {
                common = common.intersect(series.map { it.timestamp }.toSet())
            }
            if (marketPrices != null) {
                common = common.intersect(marketPrices.map { it.timestamp }.toSet())
            }
            common.sorted()
        }

        fun List<StockPriceService.HistoricalPrice>.filterAndSort(): List<Double> =
            filter { it.timestamp in commonTimestamps }
                .sortedBy { it.timestamp }
                .map { it.close }

        val alignedPrices: Map<String, List<Double>> =
            tickerPrices.mapValues { it.value.filterAndSort() }
                .filter { it.value.size >= 2 }

        val alignedMarket: List<Double> = marketPrices?.filterAndSort() ?: emptyList()

        val result = CorrelationUtils.buildMatrix(alignedPrices, alignedMarket)
            .copy(failedTickers = failed.toList())

        cachedResult = result
        return result
    }
}
```

---

## Step 5 — CorrelationMatrixUiState.kt

File: `ui/correlation/CorrelationMatrixUiState.kt`

```kotlin
package com.investhelp.app.ui.correlation

import com.investhelp.app.util.CorrelationUtils.MatrixResult

sealed interface CorrelationMatrixUiState {
    data object Loading : CorrelationMatrixUiState
    data class Error(val message: String) : CorrelationMatrixUiState
    data class Success(
        val result: MatrixResult,
        val explainerExpanded: Boolean = false
    ) : CorrelationMatrixUiState
}
```

---

## Step 6 — CorrelationMatrixViewModel.kt

File: `ui/correlation/CorrelationMatrixViewModel.kt`

```kotlin
package com.investhelp.app.ui.correlation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.dao.InvestmentItemDao
import com.investhelp.app.data.repository.CorrelationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CorrelationMatrixViewModel @Inject constructor(
    private val correlationRepository: CorrelationRepository,
    private val itemDao: InvestmentItemDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<CorrelationMatrixUiState>(CorrelationMatrixUiState.Loading)
    val uiState: StateFlow<CorrelationMatrixUiState> = _uiState.asStateFlow()

    // Survives configuration changes inside the ViewModel.
    private var explainerExpanded = false

    init {
        loadMatrix()
    }

    fun loadMatrix(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = CorrelationMatrixUiState.Loading
            if (forceRefresh) correlationRepository.clearCache()

            // Fetch all tickers from investment_positions table.
            val tickers = itemDao.getAllPositionsSnapshot()
                .map { it.ticker }
                .distinct()

            if (tickers.size < 2) {
                _uiState.value = CorrelationMatrixUiState.Error(
                    "Add at least 2 holdings to see correlation data."
                )
                return@launch
            }

            val result = runCatching { correlationRepository.buildCorrelationMatrix(tickers) }
                .getOrElse {
                    _uiState.value = CorrelationMatrixUiState.Error(
                        "Failed to load price data: ${it.message}"
                    )
                    return@launch
                }

            if (result.tickers.size < 2) {
                _uiState.value = CorrelationMatrixUiState.Error(
                    "Not enough data to build a correlation matrix."
                )
                return@launch
            }

            _uiState.value = CorrelationMatrixUiState.Success(
                result = result,
                explainerExpanded = explainerExpanded
            )
        }
    }

    fun toggleExplainer() {
        val current = _uiState.value
        if (current is CorrelationMatrixUiState.Success) {
            explainerExpanded = !explainerExpanded
            _uiState.value = current.copy(explainerExpanded = explainerExpanded)
        }
    }
}
```

> **DAO note:** `itemDao.getAllPositionsSnapshot()` — check the exact DAO method name for
> a one-shot (non-Flow) query that returns all positions. If only a Flow method exists,
> call `.first()` on it. Use the DAO that's already bound (the `investment_positions` table
> was renamed in migration v26→v27; the existing Hilt-injected DAO is already correct).

---

## Step 7 — CorrelationMatrixScreen.kt

File: `ui/correlation/CorrelationMatrixScreen.kt`

Full implementation below. Follow the exact same Scaffold + TopAppBar + CollapsibleCard
pattern used in `SimulationScreen.kt` and `VolatilityAnalysisScreen.kt`.

```kotlin
package com.investhelp.app.ui.correlation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.util.CorrelationUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

// ─── Color helpers ────────────────────────────────────────────────────────────

private fun correlationColor(value: Double): Color = when {
    value >= 0.75 -> Color(0xFFE53935)
    value >= 0.50 -> Color(0xFFFB8C00)
    value >= 0.25 -> Color(0xFFFDD835)
    value >= 0.00 -> Color(0xFF43A047)
    else          -> Color(0xFF1E88E5)
}

private fun diagonalColor() = Color(0xFF424242)

private fun correlationTextColor(value: Double): Color = when {
    value >= 0.25 && value < 0.75 -> Color.Black  // yellow/orange — dark text
    else -> Color.White
}

private fun correlationLabel(value: Double): String = when {
    value >= 0.75 -> "highly correlated"
    value >= 0.50 -> "moderately correlated"
    value >= 0.25 -> "low correlation"
    value >= 0.00 -> "largely uncorrelated"
    else          -> "inversely correlated"
}

private fun correlationExplainer(tickerA: String, tickerB: String, value: Double): String = when {
    value >= 0.75 ->
        "$tickerA and $tickerB are highly correlated. When one rises or falls sharply, " +
        "the other tends to follow. Owning both may offer less diversification than expected."
    value >= 0.50 ->
        "$tickerA and $tickerB are moderately correlated with significant overlap in price " +
        "movement. They provide some diversification, but tend to react similarly to market events."
    value >= 0.25 ->
        "$tickerA and $tickerB show low correlation with decent independent movement. " +
        "This pair contributes meaningfully to portfolio diversification."
    value >= 0.00 ->
        "$tickerA and $tickerB are largely uncorrelated — they tend to move independently. " +
        "This is a strong diversification pair."
    else ->
        "$tickerA and $tickerB are inversely correlated — one tends to rise when the other falls. " +
        "This is the strongest form of diversification."
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrelationMatrixScreen(
    onNavigateBack: () -> Unit,
    viewModel: CorrelationMatrixViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedPair by remember { mutableStateOf<Triple<String, String, Double>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text("Correlation Matrix", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Based on 1 year of daily returns",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (uiState is CorrelationMatrixUiState.Success) {
                        IconButton(onClick = { viewModel.loadMatrix(forceRefresh = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is CorrelationMatrixUiState.Loading -> LoadingContent()
                is CorrelationMatrixUiState.Error   -> ErrorContent(state.message)
                is CorrelationMatrixUiState.Success -> SuccessContent(
                    state = state,
                    onExplainerToggle = viewModel::toggleExplainer,
                    onCellTap = { a, b, v -> selectedPair = Triple(a, b, v) }
                )
            }
        }
    }

    // Cell-tap dialog
    selectedPair?.let { (a, b, v) ->
        AlertDialog(
            onDismissRequest = { selectedPair = null },
            title = { Text("$a  vs  $b", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(correlationColor(v), RoundedCornerShape(3.dp))
                        )
                        Text(
                            "Correlation: ${"%.2f".format(v)}  (${correlationLabel(v)})",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(correlationExplainer(a, b, v))
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedPair = null }) { Text("Close") }
            }
        )
    }
}

// ─── Loading / Error ──────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text("Fetching price history…", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(message, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
    }
}

// ─── Success layout ───────────────────────────────────────────────────────────

@Composable
private fun SuccessContent(
    state: CorrelationMatrixUiState.Success,
    onExplainerToggle: () -> Unit,
    onCellTap: (String, String, Double) -> Unit
) {
    val result = state.result
    val updatedStr = remember(result.updatedAt) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(result.updatedAt))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Last-updated timestamp
        Text(
            "Updated $updatedStr",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        // Failed tickers warning banner
        if (result.failedTickers.isNotEmpty()) {
            WarningBanner(result.failedTickers)
        }

        // Collapsible explainer card
        ExplainerCard(expanded = state.explainerExpanded, onToggle = onExplainerToggle)

        // Matrix grid
        MatrixGrid(result = result, onCellTap = onCellTap)

        // Market sensitivity row
        MarketSensitivityRow(result = result)

        // Summary insights
        SummaryInsightsCard(result = result)

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Warning banner ───────────────────────────────────────────────────────────

@Composable
private fun WarningBanner(failedTickers: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Could not load data for ${failedTickers.joinToString(", ")} — excluded from analysis.",
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ─── Explainer card ───────────────────────────────────────────────────────────

@Composable
private fun ExplainerCard(expanded: Boolean, onToggle: () -> Unit) {
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Card(modifier = Modifier.fillMaxWidth()) {
        // Header row — always visible, tappable
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "What is a Correlation Matrix?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(chevronRotation)
            )
        }

        AnimatedVisibility(visible = expanded) {
            HorizontalDivider()
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // What it measures
                ExplainerSection(title = "What it measures") {
                    Text(
                        "Correlation measures how much two stocks move together on a daily basis. " +
                        "A high correlation means they tend to rise and fall on the same days — so " +
                        "owning both gives you less protection than you might think.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Scale legend
                ExplainerSection(title = "The scale") {
                    val legendItems = listOf(
                        Triple("0.75 – 1.00", "Highly correlated — moves nearly in lockstep", Color(0xFFE53935)),
                        Triple("0.50 – 0.74", "Moderately correlated — significant overlap", Color(0xFFFB8C00)),
                        Triple("0.25 – 0.49", "Low correlation — decent diversification", Color(0xFFFDD835)),
                        Triple("0.00 – 0.24", "Uncorrelated — strong diversification", Color(0xFF43A047)),
                        Triple("Negative",    "Inverse relationship — one rises as other falls", Color(0xFF1E88E5))
                    )
                    legendItems.forEach { (range, desc, color) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(color, RoundedCornerShape(3.dp))
                            )
                            Text(
                                "$range  $desc",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // What to look for
                ExplainerSection(title = "What to look for") {
                    Text(
                        "If most of your holdings show red or orange against each other, your " +
                        "portfolio may not be as diversified as it looks. Aim for a mix of greens " +
                        "and yellows across sectors.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // The diagonal
                ExplainerSection(title = "The diagonal") {
                    Text(
                        "The diagonal is always 1.0 — every stock is perfectly correlated with itself.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun ExplainerSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        content()
    }
}

// ─── Matrix grid ──────────────────────────────────────────────────────────────

private val CELL_SIZE = 52.dp
private val HEADER_WIDTH = 64.dp
private val CELL_TEXT_SIZE = 10.sp

@Composable
private fun MatrixGrid(
    result: CorrelationUtils.MatrixResult,
    onCellTap: (String, String, Double) -> Unit
) {
    val tickers = result.tickers
    val hScroll = rememberScrollState()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "Correlation Matrix",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Column headers (rotated 45°) — scroll horizontally
            Row(modifier = Modifier.horizontalScroll(hScroll)) {
                Spacer(Modifier.width(HEADER_WIDTH))  // gap for row-header column
                tickers.forEach { ticker ->
                    Box(
                        modifier = Modifier
                            .width(CELL_SIZE)
                            .height(CELL_SIZE),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Text(
                            ticker,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = CELL_TEXT_SIZE),
                            modifier = Modifier
                                .rotate(-45f)
                                .padding(bottom = 4.dp),
                            maxLines = 1
                        )
                    }
                }
            }

            HorizontalDivider()

            // Rows: sticky left header + scrollable cells
            tickers.forEachIndexed { i, rowTicker ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sticky row header
                    Text(
                        rowTicker,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = CELL_TEXT_SIZE),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .width(HEADER_WIDTH)
                            .padding(end = 4.dp),
                        maxLines = 1
                    )

                    // Scrollable cells — share the same hScroll state
                    Row(modifier = Modifier.horizontalScroll(hScroll)) {
                        tickers.forEachIndexed { j, colTicker ->
                            val value = result.matrix[i][j]
                            val isDiag = i == j
                            val bgColor = if (isDiag) diagonalColor() else correlationColor(value)
                            val textColor = if (isDiag) Color.White else correlationTextColor(value)

                            Box(
                                modifier = Modifier
                                    .size(CELL_SIZE)
                                    .padding(1.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(bgColor)
                                    .then(
                                        if (!isDiag) Modifier.clickable {
                                            onCellTap(rowTicker, colTicker, value)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (value.isNaN()) "—" else "%.2f".format(value),
                                    color = textColor,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = CELL_TEXT_SIZE,
                                        fontWeight = if (isDiag) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Market sensitivity row ───────────────────────────────────────────────────

@Composable
private fun MarketSensitivityRow(result: CorrelationUtils.MatrixResult) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Market Sensitivity  (vs S&P 500)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                result.tickers.forEach { ticker ->
                    val v = result.marketCorrelation[ticker]
                    val bgColor = if (v != null && !v.isNaN()) correlationColor(v) else Color.Gray
                    val txtColor = if (v != null && !v.isNaN()) correlationTextColor(v) else Color.White
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "$ticker  ${if (v != null && !v.isNaN()) "%.2f".format(v) else "—"}",
                            color = txtColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "Shows how closely each holding tracks the overall market. " +
                "Values near 1.0 mean the stock largely follows the S&P 500.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Summary insights card ────────────────────────────────────────────────────

@Composable
private fun SummaryInsightsCard(result: CorrelationUtils.MatrixResult) {
    val avgCorr       = remember(result) { CorrelationUtils.averageCorrelation(result) }
    val mostCorr      = remember(result) { CorrelationUtils.mostCorrelatedPair(result) }
    val mostDiversify = remember(result) { CorrelationUtils.mostDiversifyingTicker(result) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Portfolio Insights",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            if (!avgCorr.isNaN()) {
                InsightBullet(
                    "Your holdings have an average correlation of ${"%.2f".format(avgCorr)}" +
                    if (avgCorr > 0.70) " — moderately high. Consider adding assets from uncorrelated sectors."
                    else " — reasonable diversification across your holdings."
                )
            }

            mostCorr?.let {
                InsightBullet(
                    "${it.tickerA} and ${it.tickerB} are your most correlated holdings " +
                    "(${"%.2f".format(it.value)}). They may behave as a single position in volatile markets."
                )
            }

            mostDiversify?.let { (ticker, avg) ->
                InsightBullet(
                    "$ticker has the lowest average correlation to your other holdings " +
                    "(${"%.2f".format(avg)}), making it your most diversifying position."
                )
            }

            if (!avgCorr.isNaN() && avgCorr > 0.70) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "⚠️  High average correlation detected. Your portfolio may be less diversified " +
                        "than it appears. Consider holdings in different sectors or asset classes.",
                        modifier = Modifier.padding(10.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightBullet(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("•", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        Text(text, style = MaterialTheme.typography.bodySmall)
    }
}
```

---

## Step 8 — InvestHelpNavHost.kt

Find the composable block section and add the new destination alongside the
existing ones (e.g., after `VolatilityAnalysisRoute`):

```kotlin
composable<CorrelationMatrixRoute> {
    CorrelationMatrixScreen(
        onNavigateBack = { navController.popBackStack() }
    )
}
```

Import: `import com.investhelp.app.ui.correlation.CorrelationMatrixScreen`

---

## Step 9 — MainActivity.kt (hamburger menu)

In the `GlobalTopBar()` composable, add a new `DropdownMenuItem` inside the
`DropdownMenu { … }` block, after the "Volatility Analysis" entry:

```kotlin
DropdownMenuItem(
    text = { Text("Correlation Matrix") },
    onClick = {
        menuExpanded = false
        navController.navigate(CorrelationMatrixRoute) {
            popUpTo(DashboardRoute) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    },
    leadingIcon = {
        Icon3D(
            Icons.Default.GridOn,    // or Icons.Default.TableChart
            contentDescription = null,
            tint = Color(0xFF00838F),
            iconSize = 16.dp,
            boxSize = 28.dp
        )
    }
)
```

Import: `import com.investhelp.app.ui.navigation.CorrelationMatrixRoute`

---

## Step 10 — RepositoryModule.kt

Add the binding inside the existing `RepositoryModule` abstract class:

```kotlin
@Binds
abstract fun bindCorrelationRepository(
    impl: CorrelationRepositoryImpl
): CorrelationRepository
```

Imports needed:
```kotlin
import com.investhelp.app.data.repository.CorrelationRepository
import com.investhelp.app.data.repository.CorrelationRepositoryImpl
```

---

## Step 11 — Unit Tests

File: `ANDROID_APP/app/src/test/java/com/investhelp/app/util/CorrelationUtilsTest.kt`

Create the `src/test/java/com/investhelp/app/util/` directory path if it does not exist.

```kotlin
package com.investhelp.app.util

import com.investhelp.app.data.remote.StockPriceService.HistoricalPrice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CorrelationUtilsTest {

    private fun prices(vararg closes: Double, baseTs: Long = 1_000L): List<HistoricalPrice> =
        closes.mapIndexed { i, c -> HistoricalPrice(timestamp = baseTs + i, close = c) }

    @Test
    fun `pearson of identical series is 1`() {
        val r = listOf(0.01, -0.02, 0.03, 0.01, -0.01)
        assertEquals(1.0, CorrelationUtils.pearson(r, r), 1e-9)
    }

    @Test
    fun `pearson of perfectly inverse series is -1`() {
        val a = listOf(0.01, 0.02, 0.03)
        val b = a.map { -it }
        assertEquals(-1.0, CorrelationUtils.pearson(a, b), 1e-9)
    }

    @Test
    fun `pearson of uncorrelated constant series returns NaN`() {
        val a = listOf(0.01, 0.02, 0.03)
        val b = listOf(0.05, 0.05, 0.05)
        assertTrue(CorrelationUtils.pearson(a, b).isNaN())
    }

    @Test
    fun `dailyReturns computes correctly`() {
        val closes = listOf(100.0, 105.0, 100.0)
        val returns = CorrelationUtils.dailyReturns(closes)
        assertEquals(2, returns.size)
        assertEquals(0.05, returns[0], 1e-9)
        assertEquals(-1.0 / 21.0, returns[1], 1e-9)
    }

    @Test
    fun `dailyReturns with single price returns empty`() {
        assertTrue(CorrelationUtils.dailyReturns(listOf(100.0)).isEmpty())
    }

    @Test
    fun `alignPriceSeries inner-joins on timestamp`() {
        val a = prices(10.0, 11.0, 12.0, baseTs = 1)
        val b = listOf(
            HistoricalPrice(timestamp = 1, close = 20.0),
            HistoricalPrice(timestamp = 3, close = 22.0)  // ts=2 missing
        )
        val (alignedA, alignedB) = CorrelationUtils.alignPriceSeries(a, b)
        assertEquals(2, alignedA.size)
        assertEquals(10.0, alignedA[0], 1e-9)
        assertEquals(12.0, alignedA[1], 1e-9)
        assertEquals(20.0, alignedB[0], 1e-9)
        assertEquals(22.0, alignedB[1], 1e-9)
    }

    @Test
    fun `buildMatrix diagonal is 1`() {
        val prices = mapOf(
            "AAPL" to listOf(100.0, 102.0, 101.0, 105.0, 103.0),
            "MSFT" to listOf(200.0, 198.0, 202.0, 201.0, 205.0)
        )
        val result = CorrelationUtils.buildMatrix(prices, emptyList())
        assertEquals(1.0, result.matrix[0][0], 1e-9)
        assertEquals(1.0, result.matrix[1][1], 1e-9)
    }

    @Test
    fun `buildMatrix is symmetric`() {
        val prices = mapOf(
            "AAPL" to listOf(100.0, 102.0, 101.0, 105.0, 103.0),
            "MSFT" to listOf(200.0, 198.0, 202.0, 201.0, 205.0)
        )
        val result = CorrelationUtils.buildMatrix(prices, emptyList())
        assertEquals(result.matrix[0][1], result.matrix[1][0], 1e-9)
    }

    @Test
    fun `averageCorrelation for 2 perfectly correlated returns 1`() {
        val prices = mapOf(
            "A" to listOf(10.0, 11.0, 12.0, 13.0, 14.0),
            "B" to listOf(10.0, 11.0, 12.0, 13.0, 14.0)
        )
        val result = CorrelationUtils.buildMatrix(prices, emptyList())
        assertEquals(1.0, CorrelationUtils.averageCorrelation(result), 1e-9)
    }
}
```

---

## DAO Method Check

Before running: verify the exact one-shot DAO method name on `InvestmentItemDao`
(or whatever DAO handles `investment_positions`). The ViewModel calls:

```kotlin
itemDao.getAllPositionsSnapshot()
```

If the DAO only exposes a Flow, replace with:

```kotlin
val tickers = itemDao.getAllPositions().first().map { it.ticker }.distinct()
```

Check the DAO file at:
`data/local/dao/InvestmentItemDao.kt` (or `InvestmentPositionDao.kt`)
and use the correct method name.

---

## Implementation Order

1. `CorrelationUtils.kt` + unit tests — verify math works before wiring UI
2. `NavRoutes.kt` — add the route object
3. `CorrelationRepository.kt` + `CorrelationRepositoryImpl.kt`
4. `RepositoryModule.kt` — bind the repository
5. `CorrelationMatrixUiState.kt`
6. `CorrelationMatrixViewModel.kt` — wire DAO + repository
7. `CorrelationMatrixScreen.kt` — full UI
8. `InvestHelpNavHost.kt` — register composable destination
9. `MainActivity.kt` — add hamburger menu item
10. Build + test on device/emulator

---

## Dependency Check

No new Gradle dependencies are required. The feature uses:
- `StockPriceService` (already injected via Hilt, already in project)
- `kotlinx.coroutines` (already a dependency)
- Jetpack Compose + Material 3 (already in project)
- Hilt (already configured)
- Standard Kotlin math (no external math library needed)

---

## Enhancement 1 — Larger Cells + Horizontal Scroll Snap

**Problem:** With > 6 tickers the 52dp cells become illegible and mid-column scroll
positions are confusing.

**Changes (all in `CorrelationMatrixScreen.kt`):**

```
CELL_SIZE       52.dp → 68.dp
ROW_LABEL_WIDTH 68.dp → 72.dp
CELL_FONT       10.sp → 11.sp
```

**Snap logic** — add inside `MatrixGrid`, triggered when horizontal fling ends:

```kotlin
val density = LocalDensity.current
val cellPx  = with(density) { CELL_SIZE.toPx() }

LaunchedEffect(hScroll.isScrollInProgress) {
    if (!hScroll.isScrollInProgress) {
        val target = (hScroll.value / cellPx).roundToInt() * cellPx.toInt()
        if (target != hScroll.value) hScroll.animateScrollTo(target)
    }
}
```

`LaunchedEffect` is keyed on `isScrollInProgress`. When it flips to `false` (fling
complete or finger lifted) the coroutine snaps to the nearest column boundary.
The `target != hScroll.value` guard prevents retriggering on the snap's own animation end.

No external dependencies needed; `ScrollState.animateScrollTo` is built-in.

---

## Enhancement 2 — High Correlation Filter Toggle

**UX:** A `FilterChip` above the matrix dims (alpha 0.20) every non-diagonal cell
whose value is < 0.75, leaving only the "red" pairs fully visible.

**State:** `rememberSaveable { mutableStateOf(false) }` in `CorrelationMatrixScreen`
(pure UI preference, no ViewModel needed — survives rotation via `rememberSaveable`).

**Chip placement:** Row above the `MatrixGrid` card inside `SuccessContent`.

```kotlin
// In SuccessContent
var filterHighCorr by rememberSaveable { mutableStateOf(false) }

FilterChip(
    selected = filterHighCorr,
    onClick   = { filterHighCorr = !filterHighCorr },
    label     = { Text("Highlight ≥ 0.75 only") },
    leadingIcon = if (filterHighCorr) {
        { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
    } else null
)
```

**Cell rendering** — pass `filterHighCorr` into `MatrixGrid`:

```kotlin
val isFiltered = filterHighCorr && !isDiag && v < 0.75
Box(
    modifier = Modifier
        .size(CELL_SIZE)
        .alpha(if (isFiltered) 0.20f else 1f)
        ...
)
```

No new state in ViewModel; no new files.

---

## Enhancement 3 — Share / Export Matrix as Image

**UX:** Share icon (top bar, next to Refresh, Success state only). Tapping it:
1. Renders the full matrix as a `Bitmap` using `android.graphics.Canvas`
2. Saves to `MediaStore` (`Pictures/InvestHelp/correlation_matrix_<ts>.png`) — visible in
   the camera roll immediately
3. Fires `Intent.ACTION_SEND` so the user can also share to any app (WhatsApp, email, etc.)

**Implementation pattern** — mirrors the existing `saveChartToPng` in `ItemDetailScreen.kt`.
Call it from `rememberCoroutineScope().launch(Dispatchers.IO)` to keep the main thread free.

```kotlin
// Top-level suspend fun in CorrelationMatrixScreen.kt
private suspend fun saveAndShareMatrix(context: Context, result: MatrixResult) =
    withContext(Dispatchers.IO) {
        val bitmap = renderMatrixBitmap(result)          // internal drawing function

        val filename = "correlation_matrix_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/InvestHelp")
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        uri?.let { imageUri ->
            context.contentResolver.openOutputStream(imageUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(context,
                    "Saved to Pictures/InvestHelp/$filename", Toast.LENGTH_SHORT).show()
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share matrix"))
            }
        }
        bitmap.recycle()
    }
```

**`renderMatrixBitmap`** — draws title, column headers (rotated), row labels, colour-coded
cells with correlation text, and a colour legend row.
Cell size in the bitmap: 80 px. Row-label column: 100 px. Header row: 80 px.
Total width  = `100 + N×80 + 40`.
Total height = `50 + 80 + N×80 + 70`.
No external library; uses `android.graphics.Paint`, `Canvas`, `Path`.

**Permission:** None needed on API 29+ (minSdk = 29). `MediaStore` external images are
writable without `WRITE_EXTERNAL_STORAGE` on modern Android.

**Share button in TopAppBar:**

```kotlin
actions = {
    if (uiState is CorrelationMatrixUiState.Success) {
        val scope   = rememberCoroutineScope()
        val context = LocalContext.current
        IconButton(onClick = {
            scope.launch { saveAndShareMatrix(context, state.result) }
        }) {
            Icon(Icons.Default.Share, contentDescription = "Export as image")
        }
        IconButton(onClick = { viewModel.refresh() }) {
            Icon(Icons.Default.Refresh, contentDescription = "Recalculate")
        }
    }
}
```

No ViewModel changes, no new files, no new Gradle dependencies.
