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
    val prices: List<HistoricalPrice>
)

@HiltViewModel
class SimulationViewModel @Inject constructor(
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _result = MutableStateFlow<SimulationResult?>(null)
    val result: StateFlow<SimulationResult?> = _result.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun runSimulation(ticker: String, shares: Double, timeRange: TimeRange) {
        viewModelScope.launch {
            _isRunning.value = true
            _error.value = null
            _result.value = null
            try {
                val prices = stockPriceService.fetchHistoricalPrices(ticker, timeRange.days)
                if (prices.size < 2) {
                    _error.value = "Not enough historical data for $ticker"
                    return@launch
                }
                val startPrice = prices.first().close
                val currentPrice = prices.last().close
                val totalCost = shares * startPrice
                val currentValue = shares * currentPrice
                val profitLoss = currentValue - totalCost
                val profitLossPct = if (totalCost > 0) profitLoss / totalCost * 100 else 0.0

                _result.value = SimulationResult(
                    ticker = ticker,
                    shares = shares,
                    timeRange = timeRange,
                    startPrice = startPrice,
                    currentPrice = currentPrice,
                    totalCost = totalCost,
                    currentValue = currentValue,
                    profitLoss = profitLoss,
                    profitLossPct = profitLossPct,
                    prices = prices
                )
            } catch (e: Exception) {
                _error.value = "Simulation failed: ${e.message}"
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun clearResult() {
        _result.value = null
        _error.value = null
    }
}
