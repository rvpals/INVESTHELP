package com.investhelp.app.ui.correlation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.dao.CorrelationCacheDao
import com.investhelp.app.data.local.entity.CorrelationCacheEntity
import com.investhelp.app.data.remote.HistoricalPrice
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.util.CorrelationUtils
import com.investhelp.app.util.CorrelationUtils.MatrixResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import javax.inject.Inject

@HiltViewModel
class CorrelationMatrixViewModel @Inject constructor(
    private val itemRepository: InvestmentItemRepository,
    private val stockPriceService: StockPriceService,
    private val correlationCacheDao: CorrelationCacheDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<CorrelationMatrixUiState>(CorrelationMatrixUiState.Loading)
    val uiState: StateFlow<CorrelationMatrixUiState> = _uiState.asStateFlow()

    private var explainerExpanded = false

    init {
        loadFromCache()
    }

    private fun loadFromCache() {
        viewModelScope.launch {
            val cached = correlationCacheDao.get()
            if (cached != null) {
                val result = cached.toMatrixResult()
                _uiState.value = CorrelationMatrixUiState.Success(
                    result = result,
                    explainerExpanded = explainerExpanded
                )
            } else {
                fetchAll()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = CorrelationMatrixUiState.Loading
            correlationCacheDao.deleteAll()
            fetchAll()
        }
    }

    fun toggleExplainer() {
        val current = _uiState.value
        if (current is CorrelationMatrixUiState.Success) {
            explainerExpanded = !explainerExpanded
            _uiState.value = current.copy(explainerExpanded = explainerExpanded)
        }
    }

    private suspend fun fetchAll() {
        val tickers = itemRepository.getAllItems().first()
            .map { it.ticker }
            .distinct()

        if (tickers.size < 2) {
            _uiState.value = CorrelationMatrixUiState.Error(
                "Add at least 2 holdings to see correlation data."
            )
            return
        }

        val allSymbols = (tickers + "^GSPC").distinct()

        // Parallel fetch for all symbols
        val fetchResults: Map<String, Result<List<HistoricalPrice>>> = coroutineScope {
            allSymbols.map { symbol ->
                symbol to async {
                    runCatching { stockPriceService.fetchHistoricalPrices(symbol, 365) }
                }
            }.associate { (symbol, deferred) -> symbol to deferred.await() }
        }

        val failedTickers = fetchResults
            .filter { it.value.isFailure && it.key != "^GSPC" }
            .keys.toList()

        val succeededPrices: Map<String, List<HistoricalPrice>> = fetchResults
            .filter { it.value.isSuccess }
            .mapValues { it.value.getOrDefault(emptyList()) }

        val tickerPrices = tickers
            .filter { it !in failedTickers }
            .associateWith { succeededPrices[it] ?: emptyList() }
            .filter { it.value.isNotEmpty() }

        if (tickerPrices.size < 2) {
            _uiState.value = CorrelationMatrixUiState.Error(
                "Not enough data to build a correlation matrix."
            )
            return
        }

        // Inner-join all series to a common set of trading dates
        val marketPrices = succeededPrices["^GSPC"] ?: emptyList()
        var commonTimestamps = tickerPrices.values.minByOrNull { it.size }!!
            .map { it.timestamp }.toSet()
        for (series in tickerPrices.values) {
            commonTimestamps = commonTimestamps.intersect(series.map { it.timestamp }.toSet())
        }
        if (marketPrices.isNotEmpty()) {
            commonTimestamps = commonTimestamps.intersect(marketPrices.map { it.timestamp }.toSet())
        }
        val sortedTs = commonTimestamps.sorted()

        fun List<HistoricalPrice>.align(): List<Double> {
            val map = associate { it.timestamp to it.close }
            return sortedTs.mapNotNull { map[it] }
        }

        val alignedPrices = tickerPrices.mapValues { it.value.align() }.filter { it.value.size >= 2 }
        val alignedMarket = marketPrices.align()

        val result = CorrelationUtils.buildMatrix(alignedPrices, alignedMarket)
            .copy(failedTickers = failedTickers)

        correlationCacheDao.upsert(result.toEntity())

        _uiState.value = CorrelationMatrixUiState.Success(
            result = result,
            explainerExpanded = explainerExpanded
        )
    }

    // ── Serialisation ──────────────────────────────────────────────────────────

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private fun MatrixResult.toEntity(): CorrelationCacheEntity {
        val tickersJson = buildJsonArray { tickers.forEach { add(it) } }.toString()
        val matrixJson = buildJsonArray {
            matrix.forEach { row -> add(buildJsonArray { row.forEach { add(it) } }) }
        }.toString()
        val marketJson = buildJsonObject {
            marketCorrelation.forEach { (k, v) -> put(k, v) }
        }.toString()
        val failedJson = buildJsonArray { failedTickers.forEach { add(it) } }.toString()
        return CorrelationCacheEntity(
            id = 1,
            tickersJson = tickersJson,
            matrixJson = matrixJson,
            marketCorrelationJson = marketJson,
            failedTickersJson = failedJson,
            calculatedAt = calculatedAt
        )
    }

    private fun CorrelationCacheEntity.toMatrixResult(): MatrixResult {
        val tickers = jsonParser.parseToJsonElement(tickersJson)
            .jsonArray.map { it.jsonPrimitive.content }
        val matrix = jsonParser.parseToJsonElement(matrixJson)
            .jsonArray.map { row -> row.jsonArray.map { it.jsonPrimitive.double } }
        val marketCorr = (jsonParser.parseToJsonElement(marketCorrelationJson) as JsonObject)
            .mapValues { it.value.jsonPrimitive.double }
        val failed = jsonParser.parseToJsonElement(failedTickersJson)
            .jsonArray.map { it.jsonPrimitive.content }
        return MatrixResult(
            tickers = tickers,
            matrix = matrix,
            marketCorrelation = marketCorr,
            failedTickers = failed,
            calculatedAt = this.calculatedAt
        )
    }
}
