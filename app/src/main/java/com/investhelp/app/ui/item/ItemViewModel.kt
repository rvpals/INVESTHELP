package com.investhelp.app.ui.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val _refreshingItemIds = MutableStateFlow<Set<Long>>(emptySet())
    val refreshingItemIds: StateFlow<Set<Long>> = _refreshingItemIds.asStateFlow()

    private val _isRefreshingAll = MutableStateFlow(false)
    val isRefreshingAll: StateFlow<Boolean> = _isRefreshingAll.asStateFlow()

    private val _priceMessage = MutableStateFlow<String?>(null)
    val priceMessage: StateFlow<String?> = _priceMessage.asStateFlow()

    fun clearPriceMessage() {
        _priceMessage.value = null
    }

    val allItems: StateFlow<List<InvestmentItemEntity>> =
        itemRepository.getAllItems()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedItem = MutableStateFlow<InvestmentItemEntity?>(null)
    val selectedItem: StateFlow<InvestmentItemEntity?> = _selectedItem.asStateFlow()

    private val _sharesOwned = MutableStateFlow(0.0)
    val sharesOwned: StateFlow<Double> = _sharesOwned.asStateFlow()

    private val _itemTransactions = MutableStateFlow<List<InvestmentTransactionEntity>>(emptyList())
    val itemTransactions: StateFlow<List<InvestmentTransactionEntity>> = _itemTransactions.asStateFlow()

    private val _statistics = MutableStateFlow(ItemStatistics(null, null, null, null, null, null))
    val statistics: StateFlow<ItemStatistics> = _statistics.asStateFlow()

    private val _analysisInfo = MutableStateFlow<AnalysisInfo?>(null)
    val analysisInfo: StateFlow<AnalysisInfo?> = _analysisInfo.asStateFlow()

    private val _isLoadingAnalysis = MutableStateFlow(false)
    val isLoadingAnalysis: StateFlow<Boolean> = _isLoadingAnalysis.asStateFlow()

    private val _analysisError = MutableStateFlow<String?>(null)
    val analysisError: StateFlow<String?> = _analysisError.asStateFlow()

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

    fun loadItem(itemId: Long) {
        viewModelScope.launch {
            itemRepository.getItemById(itemId).collect { item ->
                _selectedItem.value = item
                if (item?.ticker != null) {
                    _sharesOwned.value = itemRepository.computeSharesOwned(item.ticker)
                    transactionRepository.getTransactionsByTicker(item.ticker).collect { transactions ->
                        _itemTransactions.value = transactions
                    }
                }
            }
        }
    }

    fun loadStatistics(itemId: Long, startDate: LocalDate, endDate: LocalDate) {
        viewModelScope.launch {
            val item = itemRepository.getItemById(itemId).first()
            val ticker = item?.ticker ?: return@launch
            _statistics.value = itemRepository.getItemStatistics(ticker, startDate, endDate)
        }
    }

    fun saveItem(name: String, ticker: String?, type: InvestmentType, currentPrice: Double, existingId: Long?) {
        viewModelScope.launch {
            val item = InvestmentItemEntity(
                id = existingId ?: 0,
                name = name,
                ticker = ticker?.takeIf { it.isNotBlank() },
                type = type,
                currentPrice = currentPrice
            )
            if (existingId != null) {
                itemRepository.updateItem(item)
            } else {
                itemRepository.insertItem(item)
            }
        }
    }

    fun refreshPrice(item: InvestmentItemEntity) {
        val ticker = item.ticker
        if (ticker.isNullOrBlank()) {
            _priceMessage.value = "No ticker set for \"${item.name}\""
            return
        }
        viewModelScope.launch {
            _refreshingItemIds.value = _refreshingItemIds.value + item.id
            try {
                val price = stockPriceService.fetchPrice(ticker)
                itemRepository.updateItem(item.copy(currentPrice = price))
            } catch (e: Exception) {
                _priceMessage.value = "Failed to fetch ${ticker}: ${e.message}"
            } finally {
                _refreshingItemIds.value = _refreshingItemIds.value - item.id
            }
        }
    }

    fun refreshAllPrices() {
        viewModelScope.launch {
            _isRefreshingAll.value = true
            val items = itemRepository.getAllItems().first()
            val tickerItems = items.filter { !it.ticker.isNullOrBlank() }
            if (tickerItems.isEmpty()) {
                _priceMessage.value = "No items have tickers set."
                _isRefreshingAll.value = false
                return@launch
            }
            var successCount = 0
            var failCount = 0
            for (item in tickerItems) {
                _refreshingItemIds.value = _refreshingItemIds.value + item.id
                try {
                    val price = stockPriceService.fetchPrice(item.ticker!!)
                    itemRepository.updateItem(item.copy(currentPrice = price))
                    successCount++
                } catch (_: Exception) {
                    failCount++
                } finally {
                    _refreshingItemIds.value = _refreshingItemIds.value - item.id
                }
            }
            _priceMessage.value = "Updated $successCount prices" +
                    if (failCount > 0) ", $failCount failed" else ""
            _isRefreshingAll.value = false
        }
    }

    fun deleteItem(item: InvestmentItemEntity) {
        viewModelScope.launch {
            itemRepository.deleteItem(item)
        }
    }
}
