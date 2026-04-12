package com.investhelp.app.ui.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.model.AccountWithValue
import com.investhelp.app.ui.settings.SettingsViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TickerPosition(
    val ticker: String,
    val totalQuantity: Double,
    val totalValue: Double
)

data class MarketIndexQuote(
    val symbol: String,
    val label: String,
    val price: Double = 0.0,
    val change: Double = 0.0,
    val changePercent: Double = 0.0
)

data class DashboardUiState(
    val accounts: List<AccountWithValue> = emptyList(),
    val totalPortfolioValue: Double = 0.0,
    val positions: List<TickerPosition> = emptyList(),
    val marketIndices: List<MarketIndexQuote> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    accountRepository: AccountRepository,
    private val itemRepository: InvestmentItemRepository,
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val prefs = context.getSharedPreferences(
        SettingsViewModel.PREFS_NAME, Context.MODE_PRIVATE
    )

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _marketIndices = MutableStateFlow<List<MarketIndexQuote>>(emptyList())

    init {
        refreshMarketIndices()
    }

    fun refreshMarketIndices() {
        viewModelScope.launch {
            val enabledSymbols = prefs.getStringSet(
                SettingsViewModel.KEY_MARKET_INDICES, null
            ) ?: SettingsViewModel.DEFAULT_MARKET_INDICES

            val orderedSymbols = SettingsViewModel.AVAILABLE_MARKET_INDICES
                .filter { it.symbol in enabledSymbols }

            // Initialize with empty prices to show cards immediately
            _marketIndices.value = orderedSymbols.map {
                MarketIndexQuote(symbol = it.symbol, label = it.label)
            }

            // Fetch each quote
            for (config in orderedSymbols) {
                try {
                    val quote = stockPriceService.fetchQuote(config.symbol)
                    val change = quote.price - quote.previousClose
                    val changePct = if (quote.previousClose != 0.0)
                        change / quote.previousClose * 100.0 else 0.0

                    _marketIndices.value = _marketIndices.value.map {
                        if (it.symbol == config.symbol) it.copy(
                            price = quote.price,
                            change = change,
                            changePercent = changePct
                        ) else it
                    }
                } catch (_: Exception) { }
            }
        }
    }

    fun refreshAllPrices() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                refreshMarketIndices()

                val allItems = itemRepository.getAllItems().first()
                val byTicker = allItems.groupBy { it.ticker }
                for ((ticker, rows) in byTicker) {
                    try {
                        val quote = stockPriceService.fetchQuote(ticker)
                        val resolvedName = quote.shortName ?: rows.first().name
                        for (row in rows) {
                            val newValue = quote.price * row.quantity
                            val dayChange = (quote.price - quote.previousClose) * row.quantity
                            itemRepository.upsertItem(
                                row.copy(
                                    name = resolvedName,
                                    currentPrice = quote.price,
                                    value = newValue,
                                    dayGainLoss = dayChange,
                                    totalGainLoss = newValue - row.cost
                                )
                            )
                        }
                    } catch (_: Exception) { }
                }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getAllAccountsWithValues(),
        itemRepository.getAllItems(),
        _marketIndices
    ) { accounts, items, indices ->
        val tickerPositions = items
            .groupBy { it.ticker }
            .map { (ticker, list) ->
                TickerPosition(
                    ticker = ticker,
                    totalQuantity = list.sumOf { it.quantity },
                    totalValue = list.sumOf { it.value }
                )
            }
            .filter { it.totalValue > 0 }
            .sortedByDescending { it.totalValue }

        DashboardUiState(
            accounts = accounts,
            totalPortfolioValue = items.sumOf { it.value },
            positions = tickerPositions,
            marketIndices = indices
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
