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

data class SimulationResult(
    val ticker: String,
    val shares: Double,
    val costPerShare: Double,
    val totalCost: Double,
    val currentPrice: Double,
    val currentValue: Double,
    val profitLoss: Double,
    val profitLossPct: Double,
    val prices: List<HistoricalPrice>
)

@HiltViewModel
class SimulationViewModel @Inject constructor(
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val _isLoadingPrice = MutableStateFlow(false)
    val isLoadingPrice: StateFlow<Boolean> = _isLoadingPrice.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentPrice = MutableStateFlow<Double?>(null)
    val currentPrice: StateFlow<Double?> = _currentPrice.asStateFlow()

    private val _result = MutableStateFlow<SimulationResult?>(null)
    val result: StateFlow<SimulationResult?> = _result.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun fetchCurrentPrice(ticker: String) {
        viewModelScope.launch {
            _isLoadingPrice.value = true
            _error.value = null
            try {
                _currentPrice.value = stockPriceService.fetchPrice(ticker)
            } catch (e: Exception) {
                _error.value = "Failed to fetch price: ${e.message}"
            } finally {
                _isLoadingPrice.value = false
            }
        }
    }

    fun runSimulation(ticker: String, shares: Double, costPerShare: Double) {
        viewModelScope.launch {
            _isRunning.value = true
            _error.value = null
            _result.value = null
            try {
                val prices = stockPriceService.fetchHistoricalPrices(ticker, 14)
                if (prices.isEmpty()) {
                    _error.value = "No historical data available for $ticker"
                    return@launch
                }
                val latestPrice = prices.last().close
                val totalCost = shares * costPerShare
                val currentValue = shares * latestPrice
                val profitLoss = currentValue - totalCost
                val profitLossPct = if (totalCost > 0) profitLoss / totalCost * 100 else 0.0

                _result.value = SimulationResult(
                    ticker = ticker,
                    shares = shares,
                    costPerShare = costPerShare,
                    totalCost = totalCost,
                    currentPrice = latestPrice,
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
