package com.investhelp.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.model.AccountWithValue
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class DashboardUiState(
    val accounts: List<AccountWithValue> = emptyList(),
    val totalPortfolioValue: Double = 0.0,
    val positions: List<TickerPosition> = emptyList()
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    accountRepository: AccountRepository,
    private val itemRepository: InvestmentItemRepository,
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshAllPrices() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
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
        itemRepository.getAllItems()
    ) { accounts, items ->
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
            positions = tickerPositions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
