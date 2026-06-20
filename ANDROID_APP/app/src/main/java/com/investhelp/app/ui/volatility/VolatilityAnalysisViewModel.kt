package com.investhelp.app.ui.volatility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.util.VolatilityCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PositionVolatilityItem(
    val ticker: String,
    val companyName: String?,
    val type: InvestmentType,
    val shares: Double,
    val logo: ByteArray? = null,
    val data: VolatilityData? = null,
    val error: String? = null,
    val loading: Boolean = true
)

@HiltViewModel
class VolatilityAnalysisViewModel @Inject constructor(
    private val itemRepository: InvestmentItemRepository,
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val _items = MutableStateFlow<List<PositionVolatilityItem>>(emptyList())
    val items: StateFlow<List<PositionVolatilityItem>> = _items.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _loadedCount = MutableStateFlow(0)
    val loadedCount: StateFlow<Int> = _loadedCount.asStateFlow()

    private val cache = mutableMapOf<String, Pair<VolatilityData, Long>>()
    private val CACHE_TTL_MS = 3_600_000L

    init { loadAll() }

    fun loadAll(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isInitialLoading.value = true
            _loadedCount.value = 0

            val allItems = itemRepository.getAllItems().first()
            val positions = allItems.filter {
                it.type == InvestmentType.Stock || it.type == InvestmentType.ETF
            }

            _items.value = positions.map { entity ->
                PositionVolatilityItem(
                    ticker = entity.ticker,
                    companyName = entity.name.ifBlank { null },
                    type = entity.type,
                    shares = entity.quantity,
                    logo = entity.logo,
                    loading = true
                )
            }
            _isInitialLoading.value = false

            positions.map { entity ->
                async { fetchVolatility(entity.ticker, entity.quantity, forceRefresh) }
            }.forEach { it.await() }
        }
    }

    fun refresh() = loadAll(forceRefresh = true)

    private suspend fun fetchVolatility(ticker: String, shares: Double, forceRefresh: Boolean) {
        if (forceRefresh) cache.remove(ticker)
        val cached = cache[ticker]
        if (cached != null && System.currentTimeMillis() - cached.second < CACHE_TTL_MS) {
            _items.update { list ->
                list.map { if (it.ticker == ticker) it.copy(data = cached.first, loading = false) else it }
            }
            _loadedCount.update { it + 1 }
            return
        }
        try {
            val history = stockPriceService.fetchHistoricalPrices(ticker, 365)
            val analysis = stockPriceService.fetchAnalysisInfo(ticker)
            val quote = stockPriceService.fetchQuote(ticker)
            val closes = history.map { it.close }
            val volResult = VolatilityCalculator.compute(closes)
            val low52w = analysis.fiftyTwoWeekLow ?: closes.minOrNull() ?: 0.0
            val high52w = analysis.fiftyTwoWeekHigh ?: closes.maxOrNull() ?: 0.0
            val data = VolatilityData(
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
            cache[ticker] = data to System.currentTimeMillis()
            _items.update { list ->
                list.map { if (it.ticker == ticker) it.copy(data = data, loading = false, error = null) else it }
            }
        } catch (e: Exception) {
            _items.update { list ->
                list.map { if (it.ticker == ticker) it.copy(loading = false, error = e.message ?: "Failed") else it }
            }
        }
        _loadedCount.update { it + 1 }
    }
}
