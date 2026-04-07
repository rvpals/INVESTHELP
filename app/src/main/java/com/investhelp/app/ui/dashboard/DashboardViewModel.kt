package com.investhelp.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.investhelp.app.data.repository.AccountRepository
import com.investhelp.app.model.AccountWithValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DashboardUiState(
    val accounts: List<AccountWithValue> = emptyList(),
    val totalPortfolioValue: Double = 0.0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    accountRepository: AccountRepository
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = accountRepository.getAllAccountsWithValues()
        .map { accounts ->
            DashboardUiState(
                accounts = accounts,
                totalPortfolioValue = accounts.sumOf { it.currentValue }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
