package com.investhelp.app.ui.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.dao.InvestmentAccountDao
import com.investhelp.app.data.local.entity.InvestmentAccountEntity
import com.investhelp.app.data.local.entity.InvestmentItemEntity
import com.investhelp.app.data.local.entity.InvestmentTransactionEntity
import com.investhelp.app.data.remote.AnalysisInfo
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.InvestmentItemRepository
import com.investhelp.app.data.repository.TransactionRepository
import com.investhelp.app.model.InvestmentType
import com.investhelp.app.model.ItemStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ItemViewModel @Inject constructor(
    private val itemRepository: InvestmentItemRepository,
    private val transactionRepository: TransactionRepository,
    private val stockPriceService: StockPriceService,
    private val accountDao: InvestmentAccountDao
) : ViewModel() {

    // --- Refresh/update state ---
    private val _refreshingTickers = MutableStateFlow<Set<String>>(emptySet())
    val refreshingTickers: StateFlow<Set<String>> = _refreshingTickers.asStateFlow()

    private val _isRefreshingAll = MutableStateFlow(false)
    val isRefreshingAll: StateFlow<Boolean> = _isRefreshingAll.asStateFlow()

    private val _priceMessage = MutableStateFlow<String?>(null)
    val priceMessage: StateFlow<String?> = _priceMessage.asStateFlow()

    fun clearPriceMessage() {
        _priceMessage.value = null
    }

    // --- All items (all rows, every account) ---
    val allItems: StateFlow<List<InvestmentItemEntity>> =
        itemRepository.getAllItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Accounts (for add/edit forms) ---
    val accounts: StateFlow<List<InvestmentAccountEntity>> =
        accountDao.getAllAccounts()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Selected item state (for detail screen, loaded by ticker) ---
    private val _selectedItemRows = MutableStateFlow<List<InvestmentItemEntity>>(emptyList())
    val selectedItemRows: StateFlow<List<InvestmentItemEntity>> = _selectedItemRows.asStateFlow()

    private val _itemTransactions = MutableStateFlow<List<InvestmentTransactionEntity>>(emptyList())
    val itemTransactions: StateFlow<List<InvestmentTransactionEntity>> = _itemTransactions.asStateFlow()

    private val _statistics = MutableStateFlow(ItemStatistics(null, null, null, null, null, null))
    val statistics: StateFlow<ItemStatistics> = _statistics.asStateFlow()

    // --- Analysis info ---
    private val _analysisInfo = MutableStateFlow<AnalysisInfo?>(null)
    val analysisInfo: StateFlow<AnalysisInfo?> = _analysisInfo.asStateFlow()

    private val _isLoadingAnalysis = MutableStateFlow(false)
    val isLoadingAnalysis: StateFlow<Boolean> = _isLoadingAnalysis.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

    // --- Position refresh state ---
    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    // --- Analysis ---
    fun fetchAnalysisInfo(ticker: String) {
        viewModelScope.launch {
            _isLoadingAnalysis.value = true
            _analysisError.value = null
            _analysisInfo.value = null
            try {
                _analysisInfo.value = stockPriceService.fetchAnalysisInfo(ticker)
            } catch (e: Exception) {
                _analysisError.value = "Failed to fetch analysis: ${e.message}"
            } finally {
                _isLoadingAnalysis.value = false
            }
        }
    }

    fun clearAnalysisInfo() {
        _analysisInfo.value = null
        _analysisError.value = null
    }

    // --- Load item by ticker (for detail screen) ---
    fun loadItem(ticker: String) {
        viewModelScope.launch {
            itemRepository.getItemsByTicker(ticker).collect { rows ->
                _selectedItemRows.value = rows
            }
        }
        viewModelScope.launch {
            transactionRepository.getTransactionsByTicker(ticker).collect { transactions ->
                _itemTransactions.value = transactions
            }
        }
    }

    // --- Statistics ---
    fun loadStatistics(ticker: String, startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            _statistics.value = itemRepository.getItemStatistics(ticker, startDate, endDate)
        }
    }

    // --- Save item (add or edit metadata) ---
    fun saveItem(
        ticker: String,
        accountId: Long,
        name: String,
        type: InvestmentType,
        currentPrice: Double,
        quantity: Double,
        cost: Double
    ) {
        viewModelScope.launch {
            val existing = itemRepository.getItem(ticker, accountId)
            val item = InvestmentItemEntity(
                ticker = ticker,
                accountId = accountId,
                name = name,
                type = type,
                currentPrice = currentPrice,
                quantity = quantity,
                cost = cost,
                dayGainLoss = existing?.dayGainLoss ?: 0.0,
                totalGainLoss = existing?.totalGainLoss ?: 0.0,
                value = existing?.value ?: (quantity * currentPrice)
            )
            itemRepository.upsertItem(item)
            // Sync metadata across all accounts for same ticker
            itemRepository.updateMetadataByTicker(ticker, name, type, currentPrice)
        }
    }

    // --- Save position (from add form) ---
    fun savePosition(ticker: String, quantity: Double, cost: Double, accountId: Long?, type: InvestmentType? = null) {
        viewModelScope.launch {
            val resolvedAccountId = accountId ?: accountDao.getAllAccounts().first().firstOrNull()?.id
                ?: run {
                    _message.value = "No accounts exist. Create an account first."
                    return@launch
                }

            val existing = itemRepository.getItem(ticker, resolvedAccountId)
            val metadata = existing ?: itemRepository.getFirstByTicker(ticker)
            val resolvedType = type ?: metadata?.type ?: InvestmentType.Stock
            val value = existing?.value ?: 0.0
            val dayGainLoss = existing?.dayGainLoss ?: 0.0
            val totalGainLoss = value - cost

            itemRepository.upsertItem(
                InvestmentItemEntity(
                    ticker = ticker,
                    accountId = resolvedAccountId,
                    name = metadata?.name ?: ticker,
                    type = resolvedType,
                    currentPrice = metadata?.currentPrice ?: 0.0,
                    quantity = quantity,
                    cost = cost,
                    dayGainLoss = dayGainLoss,
                    totalGainLoss = totalGainLoss,
                    value = value
                )
            )
            // Sync type across all accounts for same ticker
            if (type != null) {
                val name = metadata?.name ?: ticker
                val price = metadata?.currentPrice ?: 0.0
                itemRepository.updateMetadataByTicker(ticker, name, resolvedType, price)
            }
        }
    }

    // --- Delete single position row ---
    fun deleteItem(ticker: String, accountId: Long) {
        viewModelScope.launch {
            itemRepository.deleteItem(ticker, accountId)
        }
    }

    // --- Refresh price for a single ticker (updates all rows for that ticker) ---
    fun refreshPrice(ticker: String) {
        if (ticker.isBlank()) return
        viewModelScope.launch {
            _refreshingTickers.value = _refreshingTickers.value + ticker
            try {
                val price = stockPriceService.fetchPrice(ticker)
                itemRepository.updatePriceByTicker(ticker, price)
                // Also update value for each row
                val rows = itemRepository.getItemsByTicker(ticker).first()
                for (row in rows) {
                    val newValue = row.quantity * price
                    itemRepository.upsertItem(row.copy(currentPrice = price, value = newValue, totalGainLoss = newValue - row.cost))
                }
            } catch (e: Exception) {
                _priceMessage.value = "Failed to fetch $ticker: ${e.message}"
            } finally {
                _refreshingTickers.value = _refreshingTickers.value - ticker
            }
        }
    }

    // --- Refresh all positions with live quotes ---
    fun refreshAllPositions() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val allPositions = itemRepository.getAllItems().first()
            var successCount = 0
            var failCount = 0

            // Group by ticker to avoid redundant API calls
            val byTicker = allPositions.groupBy { it.ticker }
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
                    successCount++
                } catch (_: Exception) {
                    failCount++
                }
            }

            _message.value = "Refreshed $successCount tickers" +
                    if (failCount > 0) ", $failCount failed" else ""
            _isRefreshing.value = false
        }
    }

    // --- Refresh all prices (simpler, just updates price) ---
    fun refreshAllPrices() {
        viewModelScope.launch {
            _isRefreshingAll.value = true
            val allPositions = itemRepository.getAllItems().first()
            val tickers = allPositions.map { it.ticker }.distinct()
            if (tickers.isEmpty()) {
                _priceMessage.value = "No items to refresh."
                _isRefreshingAll.value = false
                return@launch
            }
            var successCount = 0
            var failCount = 0
            for (ticker in tickers) {
                _refreshingTickers.value = _refreshingTickers.value + ticker
                try {
                    val quote = stockPriceService.fetchQuote(ticker)
                    val rows = allPositions.filter { it.ticker == ticker }
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
                    successCount++
                } catch (_: Exception) {
                    failCount++
                } finally {
                    _refreshingTickers.value = _refreshingTickers.value - ticker
                }
            }
            _priceMessage.value = "Updated $successCount tickers" +
                    if (failCount > 0) ", $failCount failed" else ""
            _isRefreshingAll.value = false
        }
    }
}
