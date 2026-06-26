package com.investhelp.app.ui.sharpe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.AppLog
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
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class SharpeRatioViewModel @Inject constructor(
    private val itemRepository: InvestmentItemRepository,
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val _uiState = MutableStateFlow<SharpeRatioUiState>(SharpeRatioUiState.Idle)
    val uiState: StateFlow<SharpeRatioUiState> = _uiState.asStateFlow()

    /** Displayed in the risk-free rate text field as a percentage string (e.g. "5.0"). */
    val riskFreeRatePercent = MutableStateFlow("5.0")

    /** Calendar-day lookback window passed to fetchHistoricalPrices. */
    val lookbackCalendarDays = MutableStateFlow(365)

    init { compute() }

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

            // Align price series to shared trading days
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

            // Compute per-ticker daily returns from the aligned price series
            val tickerDailyReturns = alignedSeries.pricesByTicker.mapValues { (_, prices) ->
                SharpeCalculator.calculateDailyReturns(prices)
            }

            // Timestamps for the return series are offset by one (each return = day i vs day i-1)
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
                _uiState.value = SharpeRatioUiState.Success(
                    result = SharpeCalculator.SharpeResult(
                        sharpeRatio = null,
                        annualizedReturn = 0.0,
                        annualizedVolatility = 0.0,
                        riskFreeRate = riskFreeRate,
                        lookbackDays = lookbackDays,
                        calculationDate = LocalDate.now(),
                        skippedTickers = skippedTickers,
                        skipReasons = skipReasons,
                        insufficientDataReason = insufficientReason
                    ),
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

            AppLog.log(
                "SharpeRatio: sharpe=$sharpeRatio, " +
                "return=${String.format("%.2f", annualizedReturn * 100)}%, " +
                "vol=${String.format("%.2f", annualizedVolatility * 100)}%, " +
                "days=${portfolioDailyReturns.size}, " +
                "skipped=${skippedTickers.size}"
            )

            _uiState.value = SharpeRatioUiState.Success(
                result = SharpeCalculator.SharpeResult(
                    sharpeRatio = sharpeRatio,
                    annualizedReturn = annualizedReturn,
                    annualizedVolatility = annualizedVolatility,
                    riskFreeRate = riskFreeRate,
                    lookbackDays = lookbackDays,
                    calculationDate = LocalDate.now(),
                    skippedTickers = skippedTickers,
                    skipReasons = skipReasons,
                    insufficientDataReason = insufficientDataReason
                ),
                portfolioReturnSeries = portfolioReturnSeries
            )
        }
    }

    /**
     * Fetches historical prices for [ticker] over [rangeDays] calendar days.
     * Retries once on network failure. Returns null if both attempts fail or return empty.
     */
    private suspend fun fetchWithRetry(ticker: String, rangeDays: Int): List<HistoricalPrice>? {
        return try {
            stockPriceService.fetchHistoricalPrices(ticker, rangeDays).ifEmpty { null }
        } catch (firstException: Exception) {
            try {
                stockPriceService.fetchHistoricalPrices(ticker, rangeDays).ifEmpty { null }
            } catch (retryException: Exception) {
                AppLog.log("SharpeRatio: $ticker — both fetch attempts failed: ${retryException.message}")
                null
            }
        }
    }
}
