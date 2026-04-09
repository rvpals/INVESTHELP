package com.investhelp.app.ui.transaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.remote.StockPriceService
import com.investhelp.app.data.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AnalyzePriceUiState(
    val isLoading: Boolean = true,
    val currentPrice: Double? = null,
    val avgTransactionPrice: Double? = null,
    val maxTransactionPrice: Double? = null,
    val minTransactionPrice: Double? = null,
    val highLastWeek: Double? = null,
    val lowLastWeek: Double? = null,
    val highLastMonth: Double? = null,
    val lowLastMonth: Double? = null,
    val highLastYear: Double? = null,
    val lowLastYear: Double? = null,
    val highMax: Double? = null,
    val lowMax: Double? = null,
    val error: String? = null
)

@HiltViewModel
class AnalyzePriceViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val stockPriceService: StockPriceService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyzePriceUiState())
    val uiState: StateFlow<AnalyzePriceUiState> = _uiState.asStateFlow()

    fun loadData(ticker: String) {
        viewModelScope.launch {
            _uiState.value = AnalyzePriceUiState(isLoading = true)

            try {
                // Fetch current price
                val currentPrice = stockPriceService.fetchPrice(ticker)
                _uiState.value = _uiState.value.copy(currentPrice = currentPrice)

                // Get transaction stats
                val transactions = transactionRepository.getTransactionsByTicker(ticker).first()
                val prices = transactions.map { it.pricePerShare }
                _uiState.value = _uiState.value.copy(
                    avgTransactionPrice = if (prices.isNotEmpty()) prices.average() else null,
                    maxTransactionPrice = prices.maxOrNull(),
                    minTransactionPrice = prices.minOrNull()
                )

                // Fetch historical high/low for each range
                try {
                    val weekData = stockPriceService.fetchHistoricalPrices(ticker, 7)
                    _uiState.value = _uiState.value.copy(
                        highLastWeek = weekData.maxByOrNull { it.close }?.close,
                        lowLastWeek = weekData.minByOrNull { it.close }?.close
                    )
                } catch (_: Exception) { }

                try {
                    val monthData = stockPriceService.fetchHistoricalPrices(ticker, 30)
                    _uiState.value = _uiState.value.copy(
                        highLastMonth = monthData.maxByOrNull { it.close }?.close,
                        lowLastMonth = monthData.minByOrNull { it.close }?.close
                    )
                } catch (_: Exception) { }

                try {
                    val yearData = stockPriceService.fetchHistoricalPrices(ticker, 365)
                    _uiState.value = _uiState.value.copy(
                        highLastYear = yearData.maxByOrNull { it.close }?.close,
                        lowLastYear = yearData.minByOrNull { it.close }?.close
                    )
                } catch (_: Exception) { }

                try {
                    val maxData = stockPriceService.fetchHistoricalPrices(ticker, Int.MAX_VALUE)
                    _uiState.value = _uiState.value.copy(
                        highMax = maxData.maxByOrNull { it.close }?.close,
                        lowMax = maxData.minByOrNull { it.close }?.close
                    )
                } catch (_: Exception) { }

                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Failed to load: ${e.message}"
                )
            }
        }
    }
}
