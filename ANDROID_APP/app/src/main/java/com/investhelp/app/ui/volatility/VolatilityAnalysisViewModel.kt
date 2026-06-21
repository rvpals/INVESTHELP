package com.investhelp.app.ui.volatility

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.dao.VolatilityCacheDao
import com.investhelp.app.data.local.entity.VolatilityCacheEntity
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.util.VolatilityCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val stockPriceService: StockPriceService,
    private val volatilityCacheDao: VolatilityCacheDao
) : ViewModel() {

    private val _items = MutableStateFlow<List<PositionVolatilityItem>>(emptyList())
    val items: StateFlow<List<PositionVolatilityItem>> = _items.asStateFlow()

    private val _isInitialLoading = MutableStateFlow(true)
    val isInitialLoading: StateFlow<Boolean> = _isInitialLoading.asStateFlow()

    private val _loadedCount = MutableStateFlow(0)
    val loadedCount: StateFlow<Int> = _loadedCount.asStateFlow()

    private val _lastCalculatedAt = MutableStateFlow<Long?>(null)
    val lastCalculatedAt: StateFlow<Long?> = _lastCalculatedAt.asStateFlow()

    init { loadFromDb() }

    private fun loadFromDb() {
        viewModelScope.launch {
            val cached = volatilityCacheDao.getAll()
            if (cached.isNotEmpty()) {
                val allItems = itemRepository.getAllItems().first()
                val logoMap = allItems.associate { it.ticker to it.logo }
                val typeMap = allItems.associate { it.ticker to it.type }
                _items.value = cached.map { entity ->
                    val type = typeMap[entity.ticker] ?: if (entity.type == "ETF") InvestmentType.ETF else InvestmentType.Stock
                    PositionVolatilityItem(
                        ticker = entity.ticker,
                        companyName = entity.companyName,
                        type = type,
                        shares = entity.shares,
                        logo = logoMap[entity.ticker],
                        data = VolatilityData(
                            ticker = entity.ticker,
                            companyName = entity.companyName,
                            shares = entity.shares,
                            currentPrice = entity.currentPrice,
                            low52w = entity.low52w,
                            high52w = entity.high52w,
                            rangePositionPct = entity.rangePositionPct,
                            annualizedVolPct = entity.annualizedVolPct,
                            volatilityLabel = entity.volatilityLabel,
                            dailyStdDevPct = entity.dailyStdDevPct,
                            sampleCount = entity.sampleCount
                        ),
                        loading = false
                    )
                }
                _lastCalculatedAt.value = volatilityCacheDao.getLastCalculatedAt()
                _isInitialLoading.value = false
            } else {
                fetchAll(forceRefresh = false)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            volatilityCacheDao.deleteAll()
            _lastCalculatedAt.value = null
            fetchAll(forceRefresh = true)
        }
    }

    private suspend fun fetchAll(forceRefresh: Boolean) {
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

        val fetchedEntities = mutableListOf<VolatilityCacheEntity>()
        val now = System.currentTimeMillis() / 1000L

        for (entity in positions) {
            val result = fetchVolatility(entity.ticker, entity.quantity)
            if (result != null) {
                fetchedEntities.add(result.toEntity(now))
            }
        }

        if (fetchedEntities.isNotEmpty()) {
            volatilityCacheDao.upsertAll(fetchedEntities)
            _lastCalculatedAt.value = now
        }
    }

    private suspend fun fetchVolatility(ticker: String, shares: Double): VolatilityData? {
        return try {
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
            _items.update { list ->
                list.map { if (it.ticker == ticker) it.copy(data = data, loading = false, error = null) else it }
            }
            _loadedCount.update { it + 1 }
            data
        } catch (e: Exception) {
            _items.update { list ->
                list.map { if (it.ticker == ticker) it.copy(loading = false, error = e.message ?: "Failed") else it }
            }
            _loadedCount.update { it + 1 }
            null
        }
    }

    private fun VolatilityData.toEntity(calculatedAt: Long): VolatilityCacheEntity {
        val itemType = _items.value.find { it.ticker == ticker }?.type ?: InvestmentType.Stock
        return VolatilityCacheEntity(
            ticker = ticker,
            companyName = companyName,
            type = itemType.name,
            shares = shares,
            currentPrice = currentPrice,
            annualizedVolPct = annualizedVolPct,
            dailyStdDevPct = dailyStdDevPct,
            volatilityLabel = volatilityLabel,
            low52w = low52w,
            high52w = high52w,
            rangePositionPct = rangePositionPct,
            sampleCount = sampleCount,
            calculatedAt = calculatedAt
        )
    }
}
