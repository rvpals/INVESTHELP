package com.investhelp.app.ui.correlation

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.investhelp.app.util.CorrelationUtils
import com.investhelp.app.util.CorrelationUtils.MatrixResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

// ─── Colour helpers ───────────────────────────────────────────────────────────

private fun correlationColor(v: Double): Color = when {
    v >= 0.75 -> Color(0xFFE53935)
    v >= 0.50 -> Color(0xFFFB8C00)
    v >= 0.25 -> Color(0xFFFDD835)
    v >= 0.00 -> Color(0xFF43A047)
    else      -> Color(0xFF1E88E5)
}

private val diagonalColor = Color(0xFF424242)

private fun cellTextColor(v: Double): Color = when {
    v >= 0.25 && v < 0.75 -> Color.Black
    else -> Color.White
}

private fun correlationLabel(v: Double): String = when {
    v >= 0.75 -> "highly correlated"
    v >= 0.50 -> "moderately correlated"
    v >= 0.25 -> "low correlation"
    v >= 0.00 -> "largely uncorrelated"
    else      -> "inversely correlated"
}

private fun cellExplainer(a: String, b: String, v: Double): String = when {
    v >= 0.75 ->
        "$a and $b are highly correlated. When one rises or falls sharply, the other tends to " +
        "follow. Owning both may offer less diversification than expected."
    v >= 0.50 ->
        "$a and $b are moderately correlated with significant overlap in price movement. They " +
        "provide some diversification, but tend to react similarly to market events."
    v >= 0.25 ->
        "$a and $b show low correlation with decent independent movement. This pair contributes " +
        "meaningfully to portfolio diversification."
    v >= 0.00 ->
        "$a and $b are largely uncorrelated — they tend to move independently. This is a strong " +
        "diversification pair."
    else ->
        "$a and $b are inversely correlated — one tends to rise when the other falls. This is " +
        "the strongest form of diversification."
}

// ─── Cell sizing constants ────────────────────────────────────────────────────

private val CELL_SIZE       = 68.dp   // enlarged for legibility with 6+ tickers
private val ROW_LABEL_WIDTH = 72.dp
private val CELL_FONT       = 11.sp

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CorrelationMatrixScreen(
    onNavigateBack: () -> Unit,
    viewModel: CorrelationMatrixViewModel = hiltViewModel()
) {
    val uiState  by viewModel.uiState.collectAsStateWithLifecycle()
    val context  = LocalContext.current
    val scope    = rememberCoroutineScope()

    var selectedCell     by remember { mutableStateOf<Triple<String, String, Double>?>(null) }
    // Filter toggle survives rotation via rememberSaveable
    var filterHighCorr   by rememberSaveable { mutableStateOf(false) }

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
                        val successState = uiState as CorrelationMatrixUiState.Success
                        IconButton(onClick = {
                            scope.launch {
                                saveAndShareMatrix(context, successState.result)
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Export as image")
                        }
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Recalculate")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is CorrelationMatrixUiState.Loading -> LoadingState()
                is CorrelationMatrixUiState.Error   -> ErrorState(state.message)
                is CorrelationMatrixUiState.Success -> SuccessContent(
                    state          = state,
                    filterHighCorr = filterHighCorr,
                    onToggleFilter = { filterHighCorr = !filterHighCorr },
                    onToggleExplainer = viewModel::toggleExplainer,
                    onCellTap = { a, b, v -> selectedCell = Triple(a, b, v) }
                )
            }
        }
    }

    // Cell detail dialog
    selectedCell?.let { (a, b, v) ->
        AlertDialog(
            onDismissRequest = { selectedCell = null },
            title = { Text("$a  vs  $b", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(correlationColor(v), RoundedCornerShape(3.dp))
                        )
                        Text(
                            "Correlation: ${"%.2f".format(v)}  (${correlationLabel(v)})",
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(cellExplainer(a, b, v), style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { selectedCell = null }) { Text("Close") } }
        )
    }
}

// ─── Loading / Error ──────────────────────────────────────────────────────────

@Composable
private fun LoadingState() {
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
private fun ErrorState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
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
    filterHighCorr: Boolean,
    onToggleFilter: () -> Unit,
    onToggleExplainer: () -> Unit,
    onCellTap: (String, String, Double) -> Unit
) {
    val result = state.result
    val lastCalcStr = remember(result.calculatedAt) {
        SimpleDateFormat("MMM d 'at' h:mm a", Locale.getDefault())
            .format(Date(result.calculatedAt * 1000L))
    }
    // Shared horizontal scroll — header row and every data row use the same state
    val hScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Calculated on $lastCalcStr",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        if (result.failedTickers.isNotEmpty()) {
            FailedBanner(result.failedTickers)
        }

        ExplainerCard(expanded = state.explainerExpanded, onToggle = onToggleExplainer)

        // ── Filter toggle ──────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Filter:", style = MaterialTheme.typography.labelMedium)
            FilterChip(
                selected = filterHighCorr,
                onClick  = onToggleFilter,
                label    = { Text("Highlight ≥ 0.75 only") },
                leadingIcon = if (filterHighCorr) {
                    { Icon(Icons.Default.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                } else null
            )
        }
        if (filterHighCorr) {
            Text(
                "Dimmed cells have correlation < 0.75. Only highly correlated pairs are shown at full opacity.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        MatrixGrid(
            result = result,
            hScroll = hScroll,
            filterHighCorr = filterHighCorr,
            onCellTap = onCellTap
        )

        if (result.marketCorrelation.isNotEmpty()) {
            MarketSensitivityRow(result = result)
        }

        SummaryInsightsCard(result = result)

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Failure banner ───────────────────────────────────────────────────────────

@Composable
private fun FailedBanner(failed: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            "Could not load data for ${failed.joinToString(", ")} — excluded from analysis.",
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

// ─── Explainer card ───────────────────────────────────────────────────────────

@Composable
private fun ExplainerCard(expanded: Boolean, onToggle: () -> Unit) {
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
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
                    modifier = Modifier.rotate(chevron)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ExplainerSection("What it measures") {
                            Text(
                                "Correlation measures how much two stocks move together on a daily " +
                                "basis. A high correlation means they tend to rise and fall on the same " +
                                "days — so owning both gives you less protection than you might think.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        ExplainerSection("The scale") {
                            val items = listOf(
                                Triple("0.75 – 1.00", "Highly correlated — moves nearly in lockstep",  Color(0xFFE53935)),
                                Triple("0.50 – 0.74", "Moderately correlated — significant overlap",   Color(0xFFFB8C00)),
                                Triple("0.25 – 0.49", "Low correlation — decent diversification",      Color(0xFFFDD835)),
                                Triple("0.00 – 0.24", "Uncorrelated — strong diversification",         Color(0xFF43A047)),
                                Triple("Negative",    "Inverse relationship — one rises as other falls", Color(0xFF1E88E5))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items.forEach { (range, desc, color) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(Modifier.size(12.dp).background(color, RoundedCornerShape(3.dp)))
                                        Text("$range  —  $desc", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                        ExplainerSection("What to look for") {
                            Text(
                                "If most of your holdings show red or orange against each other, your " +
                                "portfolio may not be as diversified as it looks. Aim for a mix of " +
                                "greens and yellows across sectors.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        ExplainerSection("The diagonal") {
                            Text(
                                "The diagonal is always 1.0 — every stock is perfectly correlated with itself.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
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

@Composable
private fun MatrixGrid(
    result: MatrixResult,
    hScroll: ScrollState,
    filterHighCorr: Boolean,
    onCellTap: (String, String, Double) -> Unit
) {
    val tickers = result.tickers
    val density = LocalDensity.current
    val cellPx  = with(density) { CELL_SIZE.toPx() }

    // Snap to nearest column boundary when scroll settles
    LaunchedEffect(hScroll.isScrollInProgress) {
        if (!hScroll.isScrollInProgress) {
            val target = (hScroll.value / cellPx).roundToInt() * cellPx.toInt()
            if (target != hScroll.value) hScroll.animateScrollTo(target)
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                "Correlation Matrix",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Column headers (rotated 45°) — share hScroll with data rows
            Row {
                Spacer(Modifier.width(ROW_LABEL_WIDTH))
                Row(modifier = Modifier.horizontalScroll(hScroll)) {
                    tickers.forEach { ticker ->
                        Box(
                            modifier = Modifier.width(CELL_SIZE).height(CELL_SIZE),
                            contentAlignment = Alignment.BottomStart
                        ) {
                            Text(
                                ticker,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = CELL_FONT),
                                modifier = Modifier.rotate(-45f).padding(bottom = 4.dp, start = 4.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Data rows — sticky label + scrollable cells on same hScroll
            tickers.forEachIndexed { i, rowTicker ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        rowTicker,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = CELL_FONT),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(ROW_LABEL_WIDTH).padding(end = 4.dp),
                        maxLines = 1
                    )
                    Row(modifier = Modifier.horizontalScroll(hScroll)) {
                        tickers.forEachIndexed { j, colTicker ->
                            val v      = result.matrix[i][j]
                            val isDiag = i == j
                            val bg     = if (isDiag) diagonalColor else correlationColor(v)
                            val text   = if (isDiag) Color.White else cellTextColor(v)
                            // Dim cells that don't meet the filter threshold
                            val cellAlpha = if (filterHighCorr && !isDiag && v < 0.75) 0.20f else 1f

                            Box(
                                modifier = Modifier
                                    .size(CELL_SIZE)
                                    .padding(1.dp)
                                    .alpha(cellAlpha)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(bg)
                                    .then(
                                        if (!isDiag) Modifier.clickable {
                                            onCellTap(rowTicker, colTicker, v)
                                        } else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (v.isNaN()) "—" else "%.2f".format(v),
                                    color = text,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = CELL_FONT,
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

// ─── Market sensitivity ───────────────────────────────────────────────────────

@Composable
private fun MarketSensitivityRow(result: MatrixResult) {
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
                    val v   = result.marketCorrelation[ticker]
                    val bg  = if (v != null && !v.isNaN()) correlationColor(v) else Color.Gray
                    val txt = if (v != null && !v.isNaN()) cellTextColor(v) else Color.White
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(bg)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "$ticker  ${if (v != null && !v.isNaN()) "%.2f".format(v) else "—"}",
                            color = txt,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Shows how closely each holding tracks the overall market. Values near 1.0 mean " +
                "the stock largely follows the S&P 500.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Summary insights ─────────────────────────────────────────────────────────

@Composable
private fun SummaryInsightsCard(result: MatrixResult) {
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
                    if (avgCorr > 0.70)
                        " — moderately high. Consider adding assets from uncorrelated sectors."
                    else
                        " — reasonable diversification across your holdings."
                )
            }

            mostCorr?.let { pair ->
                InsightBullet(
                    "${pair.tickerA} and ${pair.tickerB} are your most correlated holdings " +
                    "(${"%.2f".format(pair.value)}). They may behave as a single position in " +
                    "volatile markets."
                )
            }

            mostDiversify?.let { (ticker, avg) ->
                if (!avg.isNaN()) {
                    InsightBullet(
                        "$ticker has the lowest average correlation to your other holdings " +
                        "(${"%.2f".format(avg)}), making it your most diversifying position."
                    )
                }
            }

            if (!avgCorr.isNaN() && avgCorr > 0.70) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
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

// ─── Save + Share ─────────────────────────────────────────────────────────────

private suspend fun saveAndShareMatrix(context: Context, result: MatrixResult) =
    withContext(Dispatchers.IO) {
        val bitmap = renderMatrixBitmap(result)

        val filename = "correlation_matrix_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/InvestHelp")
        }
        val uri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        )
        uri?.let { imageUri ->
            context.contentResolver.openOutputStream(imageUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "Saved to Pictures/InvestHelp/$filename",
                    Toast.LENGTH_SHORT
                ).show()
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Share Correlation Matrix")
                )
            }
        }
        bitmap.recycle()
    }

private fun renderMatrixBitmap(result: MatrixResult): Bitmap {
    val tickers   = result.tickers
    val n         = tickers.size
    val cellPx    = 80f
    val labelPx   = 100f
    val headerPx  = 80f
    val titlePx   = 50f
    val legendPx  = 70f
    val padH      = 20f

    val width  = (labelPx + n * cellPx + padH).toInt()
    val height = (titlePx + headerPx + n * cellPx + legendPx + padH).toInt()

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    // ── Paints ──
    val titlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK; textSize = 28f; isAntiAlias = true
        isFakeBoldText = true; textAlign = android.graphics.Paint.Align.CENTER
    }
    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY; textSize = 20f; isAntiAlias = true
    }
    val cellTextPaint = android.graphics.Paint().apply {
        textSize = 22f; isAntiAlias = true; isFakeBoldText = false
        textAlign = android.graphics.Paint.Align.CENTER
    }
    val legendLabelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.DKGRAY; textSize = 18f; isAntiAlias = true
    }
    val fillPaint = android.graphics.Paint().apply { isAntiAlias = true }

    // ── Title ──
    canvas.drawText(
        "Correlation Matrix",
        width / 2f, titlePx - 10f, titlePaint
    )

    // ── Column headers (rotated -45°) ──
    tickers.forEachIndexed { j, ticker ->
        val cx = labelPx + j * cellPx + cellPx / 2f
        val cy = titlePx + headerPx - 4f
        canvas.save()
        canvas.rotate(-45f, cx, cy)
        labelPaint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText(ticker, cx - cellPx / 2f + 4f, cy, labelPaint)
        canvas.restore()
    }

    // ── Cells ──
    tickers.forEachIndexed { i, rowTicker ->
        val rowY = titlePx + headerPx + i * cellPx

        // Row label
        labelPaint.textAlign = android.graphics.Paint.Align.RIGHT
        canvas.drawText(rowTicker, labelPx - 6f, rowY + cellPx / 2f + 7f, labelPaint)

        tickers.forEachIndexed { j, _ ->
            val v      = result.matrix[i][j]
            val isDiag = i == j
            val left   = labelPx + j * cellPx
            val top    = rowY
            val rect   = android.graphics.RectF(left + 1, top + 1, left + cellPx - 1, top + cellPx - 1)

            val argb = if (isDiag) {
                android.graphics.Color.rgb(66, 66, 66)
            } else {
                when {
                    v >= 0.75 -> android.graphics.Color.rgb(229,  57,  53)
                    v >= 0.50 -> android.graphics.Color.rgb(251, 140,   0)
                    v >= 0.25 -> android.graphics.Color.rgb(253, 216,  53)
                    v >= 0.00 -> android.graphics.Color.rgb( 67, 160,  71)
                    else      -> android.graphics.Color.rgb( 30, 136, 229)
                }
            }
            fillPaint.color = argb
            canvas.drawRoundRect(rect, 6f, 6f, fillPaint)

            cellTextPaint.color = if (isDiag || v < 0.25 || v >= 0.75) {
                android.graphics.Color.WHITE
            } else {
                android.graphics.Color.BLACK
            }
            val label = if (v.isNaN()) "—" else "%.2f".format(v)
            canvas.drawText(label, left + cellPx / 2f, top + cellPx / 2f + 8f, cellTextPaint)
        }
    }

    // ── Colour legend ──
    val legendY = titlePx + headerPx + n * cellPx + 10f
    val legendItems = listOf(
        Pair("≥0.75 High",   android.graphics.Color.rgb(229, 57,  53)),
        Pair("≥0.50 Mod",    android.graphics.Color.rgb(251, 140,  0)),
        Pair("≥0.25 Low",    android.graphics.Color.rgb(253, 216, 53)),
        Pair("≥0.00 None",   android.graphics.Color.rgb( 67, 160, 71)),
        Pair("< 0  Inverse", android.graphics.Color.rgb( 30, 136, 229))
    )
    val boxW  = 22f
    val boxH  = 22f
    val gap   = (width - padH * 2) / legendItems.size
    legendItems.forEachIndexed { k, (label, color) ->
        val lx = padH + k * gap
        fillPaint.color = color
        canvas.drawRoundRect(
            android.graphics.RectF(lx, legendY, lx + boxW, legendY + boxH),
            4f, 4f, fillPaint
        )
        legendLabelPaint.textAlign = android.graphics.Paint.Align.LEFT
        canvas.drawText(label, lx + boxW + 4f, legendY + boxH - 4f, legendLabelPaint)
    }

    return bitmap
}
