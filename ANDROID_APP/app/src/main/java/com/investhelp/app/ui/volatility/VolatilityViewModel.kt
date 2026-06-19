package com.investhelp.app.ui.volatility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.util.VolatilityCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VolatilityData(
    val ticker: String,
    val companyName: String?,
    val shares: Double,
    val currentPrice: Double,
    val low52w: Double,
    val high52w: Double,
    val rangePositionPct: Double,
    val annualizedVolPct: Double,
    val volatilityLabel: String,
    val dailyStdDevPct: Double,
    val sampleCount: Int
)

@HiltViewModel
class VolatilityViewModel @Inject constructor(
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _data = MutableStateFlow<VolatilityData?>(null)
    val data: StateFlow<VolatilityData?> = _data.asStateFlow()

    // In-memory cache: ticker → (data, fetchTimeMillis)
    private val cache = mutableMapOf<String, Pair<VolatilityData, Long>>()
    private val CACHE_TTL_MS = 3_600_000L  // 1 hour

    fun loadData(ticker: String, shares: Double) {
        val cached = cache[ticker]
        if (cached != null && System.currentTimeMillis() - cached.second < CACHE_TTL_MS) {
            _data.value = cached.first
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                val history = stockPriceService.fetchHistoricalPrices(ticker, 365)
                val analysis = stockPriceService.fetchAnalysisInfo(ticker)
                val quote = stockPriceService.fetchQuote(ticker)

                val closes = history.map { it.close }
                val volResult = VolatilityCalculator.compute(closes)

                val low52w = analysis.fiftyTwoWeekLow
                    ?: closes.minOrNull()
                    ?: 0.0
                val high52w = analysis.fiftyTwoWeekHigh
                    ?: closes.maxOrNull()
                    ?: 0.0

                val result = VolatilityData(
                    ticker = ticker,
                    companyName = analysis.shortName,
                    shares = shares,
                    currentPrice = quote.price,
                    low52w = low52w,
                    high52w = high52w,
                    rangePositionPct = VolatilityCalculator.rangePositionPct(quote.price, low52w, high52w),
                    annualizedVolPct = volResult.annualizedVolPct,
                    volatilityLabel = VolatilityCalculator.volatilityLabel(volResult.annualizedVolPct),
                    dailyStdDevPct = volResult.dailyStdDevPct,
                    sampleCount = history.size
                )
                cache[ticker] = result to System.currentTimeMillis()
                _data.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refresh(ticker: String, shares: Double) {
        cache.remove(ticker)
        loadData(ticker, shares)
    }
}
