package com.investhelp.app.ui.sharpe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.AppLog
import com.investhelp.app.data.local.dao.SharpeCacheDao
import com.investhelp.app.data.local.entity.SharpeCacheEntity
import com.investhelp.app.data.remote.HistoricalPrice
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.util.SharpeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class SharpeRatioViewModel @Inject constructor(
    private val itemRepository: InvestmentItemRepository,
    private val stockPriceService: StockPriceService,
    private val sharpeCacheDao: SharpeCacheDao
) : ViewModel() {

    private val _uiState = MutableStateFlow<SharpeRatioUiState>(SharpeRatioUiState.Idle)
    val uiState: StateFlow<SharpeRatioUiState> = _uiState.asStateFlow()

    /** Displayed in the risk-free rate text field as a percentage string (e.g. "5.0"). */
    val riskFreeRatePercent = MutableStateFlow("5.0")

    /** Calendar-day lookback window passed to fetchHistoricalPrices. */
    val lookbackCalendarDays = MutableStateFlow(365)

    init { tryLoadFromCache() }

    private fun tryLoadFromCache() {
        viewModelScope.launch {
            val cached = sharpeCacheDao.get() ?: return@launch
            // Restore UI controls to match the cached computation's parameters
            riskFreeRatePercent.value = String.format("%.1f", cached.riskFreeRate * 100.0)
            lookbackCalendarDays.value = cached.lookbackDays
            _uiState.value = cached.toUiSuccess()
        }
    }

    fun compute() {
        viewModelScope.launch {
            val riskFreeRate = (riskFreeRatePercent.value.toDoubleOrNull() ?: 5.0) / 100.0
            val lookbackDays = lookbackCalendarDays.value

            _uiState.value = SharpeRatioUiState.Loading("Loading positions…")

            val allItems = itemRepository.getAllItems().first()
            val positions = allItems.filter {
                (it.type == InvestmentType.Stock || it.type == InvestmentType.ETF) && it.quantity > 0
            }

            if (positions.isEmpty()) {
                _uiState.value = SharpeRatioUiState.Error("No stock or ETF positions found.")
                return@launch
            }

            val priceMap = mutableMapOf<String, List<HistoricalPrice>>()
            val skippedTickers = mutableListOf<String>()
            val skipReasons = mutableMapOf<String, String>()

            positions.forEachIndexed { index, position ->
                _uiState.value = SharpeRatioUiState.Loading(
                    "Fetching prices…  ${index + 1} / ${positions.size}  (${position.ticker})"
                )
                val history = fetchWithRetry(position.ticker, lookbackDays)
                when {
                    history == null -> {
                        skippedTickers.add(position.ticker)
                        skipReasons[position.ticker] = "Price history unavailable"
                        AppLog.log("SharpeRatio: skipped ${position.ticker} — fetch failed after retry")
                    }
                    history.size < SharpeCalculator.MINIMUM_RETURN_OBSERVATIONS + 1 -> {
                        skippedTickers.add(position.ticker)
                        skipReasons[position.ticker] = "Only ${history.size} trading days returned"
                        AppLog.log("SharpeRatio: skipped ${position.ticker} — insufficient data (${history.size} days)")
                    }
                    else -> priceMap[position.ticker] = history
                }
            }

            if (priceMap.isEmpty()) {
                _uiState.value = SharpeRatioUiState.Error(
                    "No price data available for any position.\nSkipped: ${skippedTickers.joinToString()}"
                )
                return@launch
            }

            val validPositions = positions.filter { priceMap.containsKey(it.ticker) }
            val holdings = validPositions.associate { it.ticker to it.quantity }
            val currentPrices = validPositions.associate { it.ticker to it.currentPrice }
            val weights = SharpeCalculator.calculatePortfolioWeights(holdings, currentPrices)

            if (weights.isEmpty()) {
                _uiState.value = SharpeRatioUiState.Error(
                    "Unable to compute portfolio weights — all position values are zero."
                )
                return@launch
            }

            val filteredPriceMap = priceMap.filter { weights.containsKey(it.key) }
            val alignedSeries = SharpeCalculator.alignPriceSeriesForAllTickers(
                priceMap = filteredPriceMap,
                onSkip = { ticker, reason ->
                    if (!skippedTickers.contains(ticker)) {
                        skippedTickers.add(ticker)
                        skipReasons[ticker] = reason
                    }
                    AppLog.log("SharpeRatio: alignment skipped $ticker — $reason")
                }
            )

            if (alignedSeries.timestamps.isEmpty()) {
                _uiState.value = SharpeRatioUiState.Error(
                    "No overlapping trading days across positions after alignment."
                )
                return@launch
            }

            val tickerDailyReturns = alignedSeries.pricesByTicker.mapValues { (_, prices) ->
                SharpeCalculator.calculateDailyReturns(prices)
            }
            val returnTimestamps = alignedSeries.timestamps.drop(1)
            val portfolioDailyReturns = SharpeCalculator.calculatePortfolioDailyReturns(
                tickerDailyReturns = tickerDailyReturns,
                weights = weights
            )
            val portfolioReturnSeries = returnTimestamps.zip(portfolioDailyReturns)

            if (portfolioDailyReturns.size < SharpeCalculator.MINIMUM_RETURN_OBSERVATIONS) {
                val insufficientReason =
                    "Only ${portfolioDailyReturns.size} aligned trading days " +
                    "(minimum ${SharpeCalculator.MINIMUM_RETURN_OBSERVATIONS} required)"
                val result = SharpeCalculator.SharpeResult(
                    sharpeRatio = null,
                    annualizedReturn = 0.0,
                    annualizedVolatility = 0.0,
                    riskFreeRate = riskFreeRate,
                    lookbackDays = lookbackDays,
                    calculationDate = LocalDate.now(),
                    skippedTickers = skippedTickers,
                    skipReasons = skipReasons,
                    insufficientDataReason = insufficientReason,
                    alignedTradingDays = portfolioDailyReturns.size
                )
                sharpeCacheDao.upsert(result.toEntity(portfolioReturnSeries))
                _uiState.value = SharpeRatioUiState.Success(
                    result = result,
                    portfolioReturnSeries = portfolioReturnSeries
                )
                return@launch
            }

            val annualizedReturn = SharpeCalculator.annualizeReturn(portfolioDailyReturns)
            val dailyRiskFreeRate = SharpeCalculator.calculateDailyRiskFreeRate(riskFreeRate)
            val excessReturns = SharpeCalculator.calculateExcessReturns(portfolioDailyReturns, dailyRiskFreeRate)
            val annualizedVolatility = SharpeCalculator.annualizeStandardDeviation(excessReturns)
            val sharpeRatio = SharpeCalculator.calculateSharpeRatio(
                annualizedReturn = annualizedReturn,
                riskFreeRate = riskFreeRate,
                annualizedStandardDeviation = annualizedVolatility
            )
            val insufficientDataReason = if (sharpeRatio == null)
                "Annualized volatility is zero — portfolio has no measurable price variation in this period"
            else null

            val meanDailyReturn = portfolioDailyReturns.average()

            // Per-ticker breakdown for the Calculation Detail card
            val tickerDetails = weights.map { (ticker, weight) ->
                val position = validPositions.first { it.ticker == ticker }
                val tickerReturns = tickerDailyReturns[ticker] ?: emptyList()
                val tickerExcess = tickerReturns.map { it - dailyRiskFreeRate }
                val tickerAnnReturn = if (tickerReturns.isNotEmpty())
                    tickerReturns.average() * SharpeCalculator.TRADING_DAYS_PER_YEAR else 0.0
                val tickerAnnVol = if (tickerExcess.size >= 2) {
                    val mean = tickerExcess.average()
                    val variance = tickerExcess.sumOf { (it - mean) * (it - mean) } / (tickerExcess.size - 1)
                    sqrt(variance) * sqrt(SharpeCalculator.TRADING_DAYS_PER_YEAR.toDouble())
                } else 0.0
                SharpeCalculator.TickerDetail(
                    ticker = ticker,
                    shares = position.quantity,
                    currentPrice = position.currentPrice,
                    positionValue = position.quantity * position.currentPrice,
                    weight = weight,
                    annualizedReturn = tickerAnnReturn,
                    annualizedVolatility = tickerAnnVol,
                    tradingDays = tickerReturns.size
                )
            }.sortedByDescending { it.weight }

            AppLog.log(
                "SharpeRatio: sharpe=$sharpeRatio, " +
                "return=${String.format("%.2f", annualizedReturn * 100)}%, " +
                "vol=${String.format("%.2f", annualizedVolatility * 100)}%, " +
                "days=${portfolioDailyReturns.size}, " +
                "skipped=${skippedTickers.size}"
            )

            val result = SharpeCalculator.SharpeResult(
                sharpeRatio = sharpeRatio,
                annualizedReturn = annualizedReturn,
                annualizedVolatility = annualizedVolatility,
                riskFreeRate = riskFreeRate,
                lookbackDays = lookbackDays,
                calculationDate = LocalDate.now(),
                skippedTickers = skippedTickers,
                skipReasons = skipReasons,
                insufficientDataReason = insufficientDataReason,
                tickerDetails = tickerDetails,
                alignedTradingDays = portfolioDailyReturns.size,
                meanDailyReturn = meanDailyReturn,
                dailyRiskFreeRateUsed = dailyRiskFreeRate
            )

            sharpeCacheDao.upsert(result.toEntity(portfolioReturnSeries))
            _uiState.value = SharpeRatioUiState.Success(
                result = result,
                portfolioReturnSeries = portfolioReturnSeries
            )
        }
    }

    /**
     * Always forces interval="1d" regardless of lookback window.
     * Without this, Yahoo Finance switches to weekly data for ranges > 1825 days (5yr+),
     * which breaks the ×252 annualisation assumption used throughout SharpeCalculator.
     */
    private suspend fun fetchWithRetry(ticker: String, rangeDays: Int): List<HistoricalPrice>? {
        return try {
            stockPriceService.fetchHistoricalPrices(ticker, rangeDays, interval = "1d").ifEmpty { null }
        } catch (firstException: Exception) {
            try {
                stockPriceService.fetchHistoricalPrices(ticker, rangeDays, interval = "1d").ifEmpty { null }
            } catch (retryException: Exception) {
                AppLog.log("SharpeRatio: $ticker — both fetch attempts failed: ${retryException.message}")
                null
            }
        }
    }

    // ── JSON serialisation ─────────────────────────────────────────────────────

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private fun SharpeCalculator.SharpeResult.toEntity(
        portfolioReturnSeries: List<Pair<Long, Double>>
    ): SharpeCacheEntity {
        val tickerDetailsJson = buildJsonArray {
            tickerDetails.forEach { d ->
                add(buildJsonObject {
                    put("ticker", d.ticker)
                    put("shares", d.shares)
                    put("currentPrice", d.currentPrice)
                    put("positionValue", d.positionValue)
                    put("weight", d.weight)
                    put("annualizedReturn", d.annualizedReturn)
                    put("annualizedVolatility", d.annualizedVolatility)
                    put("tradingDays", d.tradingDays)
                })
            }
        }.toString()

        val returnSeriesJson = buildJsonArray {
            portfolioReturnSeries.forEach { (ts, ret) ->
                add(buildJsonArray { add(ts); add(ret) })
            }
        }.toString()

        val skippedJson = buildJsonArray { skippedTickers.forEach { add(it) } }.toString()
        val reasonsJson = buildJsonObject { skipReasons.forEach { (k, v) -> put(k, v) } }.toString()

        return SharpeCacheEntity(
            id = 1,
            riskFreeRate = riskFreeRate,
            lookbackDays = lookbackDays,
            sharpeRatio = sharpeRatio,
            annualizedReturn = annualizedReturn,
            annualizedVolatility = annualizedVolatility,
            alignedTradingDays = alignedTradingDays,
            meanDailyReturn = meanDailyReturn,
            dailyRiskFreeRateUsed = dailyRiskFreeRateUsed,
            calculationDate = calculationDate.toString(),
            tickerDetailsJson = tickerDetailsJson,
            portfolioReturnSeriesJson = returnSeriesJson,
            skippedTickersJson = skippedJson,
            skipReasonsJson = reasonsJson,
            insufficientDataReason = insufficientDataReason,
            calculatedAt = System.currentTimeMillis() / 1000L
        )
    }

    private fun SharpeCacheEntity.toUiSuccess(): SharpeRatioUiState.Success {
        val tickerDetails = jsonParser.parseToJsonElement(tickerDetailsJson)
            .jsonArray.map { el ->
                val obj = el.jsonObject
                SharpeCalculator.TickerDetail(
                    ticker = obj["ticker"]!!.jsonPrimitive.content,
                    shares = obj["shares"]!!.jsonPrimitive.double,
                    currentPrice = obj["currentPrice"]!!.jsonPrimitive.double,
                    positionValue = obj["positionValue"]!!.jsonPrimitive.double,
                    weight = obj["weight"]!!.jsonPrimitive.double,
                    annualizedReturn = obj["annualizedReturn"]!!.jsonPrimitive.double,
                    annualizedVolatility = obj["annualizedVolatility"]!!.jsonPrimitive.double,
                    tradingDays = obj["tradingDays"]!!.jsonPrimitive.int
                )
            }

        val portfolioReturnSeries = jsonParser.parseToJsonElement(portfolioReturnSeriesJson)
            .jsonArray.map { el ->
                val pair = el.jsonArray
                Pair(pair[0].jsonPrimitive.long, pair[1].jsonPrimitive.double)
            }

        val skippedTickers = jsonParser.parseToJsonElement(skippedTickersJson)
            .jsonArray.map { it.jsonPrimitive.content }

        val skipReasons = (jsonParser.parseToJsonElement(skipReasonsJson) as JsonObject)
            .mapValues { it.value.jsonPrimitive.content }

        return SharpeRatioUiState.Success(
            result = SharpeCalculator.SharpeResult(
                sharpeRatio = sharpeRatio,
                annualizedReturn = annualizedReturn,
                annualizedVolatility = annualizedVolatility,
                riskFreeRate = riskFreeRate,
                lookbackDays = lookbackDays,
                calculationDate = LocalDate.parse(calculationDate),
                skippedTickers = skippedTickers,
                skipReasons = skipReasons,
                insufficientDataReason = insufficientDataReason,
                tickerDetails = tickerDetails,
                alignedTradingDays = alignedTradingDays,
                meanDailyReturn = meanDailyReturn,
                dailyRiskFreeRateUsed = dailyRiskFreeRateUsed
            ),
            portfolioReturnSeries = portfolioReturnSeries,
            isFromCache = true,
            cachedAt = calculatedAt
        )
    }
}
