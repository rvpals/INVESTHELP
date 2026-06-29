package com.investhelp.app.ui.sharpe

import com.investhelp.app.util.RollingRiskEngine

sealed interface RollingRiskUiState {
    data object Idle : RollingRiskUiState
    data class Loading(val message: String = "") : RollingRiskUiState
    data class Success(val points: List<RollingRiskEngine.RollingRiskPoint>) : RollingRiskUiState
    data class Error(val message: String) : RollingRiskUiState
}
