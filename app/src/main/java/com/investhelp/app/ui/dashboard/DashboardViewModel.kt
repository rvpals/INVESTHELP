package com.investhelp.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.local.dao.PositionDao
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.model.AccountWithValue
import dagger.hilt.android.lifecycle.HiltViewModel
import com.investhelp.app.data.local.entity.PositionEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    positionDao: PositionDao
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        accountRepository.getAllAccountsWithValues(),
        positionDao.getAllPositions()
    ) { accounts, positions ->
        val tickerPositions = positions
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
            totalPortfolioValue = accounts.sumOf { it.currentValue },
            positions = tickerPositions
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
