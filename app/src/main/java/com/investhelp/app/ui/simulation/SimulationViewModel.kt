package com.investhelp.app.ui.simulation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.remote.HistoricalPrice
import com.investhelp.app.data.remote.StockPriceService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class TimeRange(val label: String, val days: Int, val group: String) {
    ONE_WEEK("1W", 7, "Week"),
    TWO_WEEKS("2W", 14, "Week"),
    ONE_MONTH("1M", 30, "Month"),
    THREE_MONTHS("3M", 90, "Month"),
    SIX_MONTHS("6M", 180, "Month"),
    ONE_YEAR("1Y", 365, "Year"),
    TWO_YEARS("2Y", 730, "Year"),
    FIVE_YEARS("5Y", 1825, "Year"),
    TEN_YEARS("10Y", 3650, "Year"),
    MAX("MAX", Int.MAX_VALUE, "Year")
}

data class SimulationResult(
    val ticker: String,
    val shares: Double,
    val timeRange: TimeRange,
    val startPrice: Double,
    val currentPrice: Double,
    val totalCost: Double,
    val currentValue: Double,
    val profitLoss: Double,
    val profitLossPct: Double,
    val prices: List<HistoricalPrice>,
    val customLabel: String? = null
)

data class DetailChartData(
    val label: String,
    val timeRange: TimeRange,
    val prices: List<HistoricalPrice>,
    val startPrice: Double
)

@HiltViewModel
class SimulationViewModel @Inject constructor(
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _results = MutableStateFlow<List<SimulationResult>>(emptyList())
    val results: StateFlow<List<SimulationResult>> = _results.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _detailCharts = MutableStateFlow<List<DetailChartData>>(emptyList())
    val detailCharts: StateFlow<List<DetailChartData>> = _detailCharts.asStateFlow()

    private val _isRunningDetail = MutableStateFlow(false)
    val isRunningDetail: StateFlow<Boolean> = _isRunningDetail.asStateFlow()

    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError.asStateFlow()

    fun runDetailSimulation(ticker: String) {
        viewModelScope.launch {
            _isRunningDetail.value = true
            _detailError.value = null
            _detailCharts.value = emptyList()

            val ranges = listOf(
                "1 Week" to TimeRange.ONE_WEEK,
                "3 Months" to TimeRange.THREE_MONTHS,
                "5 Years" to TimeRange.FIVE_YEARS,
                "Max" to TimeRange.MAX
            )

            val results = mutableListOf<DetailChartData>()
            try {
                for ((label, range) in ranges) {
                    val prices = stockPriceService.fetchHistoricalPrices(ticker, range.days)
                    if (prices.size >= 2) {
                        results.add(DetailChartData(label, range, prices, prices.first().close))
                    }
                }
                if (results.isEmpty()) {
                    _detailError.value = "Not enough historical data for $ticker"
                } else {
                    _detailCharts.value = results
                }
            } catch (e: Exception) {
                _detailError.value = "Detail simulation failed: ${e.message}"
            } finally {
                _isRunningDetail.value = false
            }
        }
    }

    fun runSimulation(ticker: String, shares: Double, selectedRanges: Set<TimeRange>) {
        viewModelScope.launch {
            _isRunning.value = true
            _error.value = null
            _results.value = emptyList()
            try {
                val sorted = selectedRanges.sortedBy { it.days }
                val resultList = mutableListOf<SimulationResult>()
                for (range in sorted) {
                    val prices = stockPriceService.fetchHistoricalPrices(ticker, range.days)
                    if (prices.size >= 2) {
                        val startPrice = prices.first().close
                        val currentPrice = prices.last().close
                        val totalCost = shares * startPrice
                        val currentValue = shares * currentPrice
                        val profitLoss = currentValue - totalCost
                        val profitLossPct = if (totalCost > 0) profitLoss / totalCost * 100 else 0.0
                        resultList.add(
                            SimulationResult(
                                ticker = ticker,
                                shares = shares,
                                timeRange = range,
                                startPrice = startPrice,
                                currentPrice = currentPrice,
                                totalCost = totalCost,
                                currentValue = currentValue,
                                profitLoss = profitLoss,
                                profitLossPct = profitLossPct,
                                prices = prices
                            )
                        )
                    }
                }
                if (resultList.isEmpty()) {
                    _error.value = "Not enough historical data for $ticker"
                } else {
                    _results.value = resultList
                }
            } catch (e: Exception) {
                _error.value = "Simulation failed: ${e.message}"
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun runTransactionSimulation(ticker: String, shares: Double, rangeDays: Int) {
        viewModelScope.launch {
            _isRunning.value = true
            _error.value = null
            _results.value = emptyList()
            try {
                val prices = stockPriceService.fetchHistoricalPrices(ticker, rangeDays)
                if (prices.size >= 2) {
                    val startPrice = prices.first().close
                    val currentPrice = prices.last().close
                    val totalCost = shares * startPrice
                    val currentValue = shares * currentPrice
                    val profitLoss = currentValue - totalCost
                    val profitLossPct = if (totalCost > 0) profitLoss / totalCost * 100 else 0.0
                    val label = when {
                        rangeDays < 7 -> "${rangeDays}d"
                        rangeDays < 30 -> "${rangeDays / 7}w ${rangeDays % 7}d"
                        rangeDays < 365 -> "${rangeDays / 30}m ${rangeDays % 30}d"
                        else -> "${rangeDays / 365}y ${(rangeDays % 365) / 30}m"
                    }
                    _results.value = listOf(
                        SimulationResult(
                            ticker = ticker,
                            shares = shares,
                            timeRange = TimeRange.entries.lastOrNull { it.days <= rangeDays } ?: TimeRange.MAX,
                            startPrice = startPrice,
                            currentPrice = currentPrice,
                            totalCost = totalCost,
                            currentValue = currentValue,
                            profitLoss = profitLoss,
                            profitLossPct = profitLossPct,
                            prices = prices,
                            customLabel = label
                        )
                    )
                } else {
                    _error.value = "Not enough historical data for $ticker"
                }
            } catch (e: Exception) {
                _error.value = "Simulation failed: ${e.message}"
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun clearResults() {
        _results.value = emptyList()
        _error.value = null
    }
}
