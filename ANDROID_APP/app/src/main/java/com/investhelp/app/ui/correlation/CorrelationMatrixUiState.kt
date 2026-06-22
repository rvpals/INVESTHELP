package com.investhelp.app.ui.correlation

import com.investhelp.app.util.CorrelationUtils.MatrixResult

sealed interface CorrelationMatrixUiState {
    data object Loading : CorrelationMatrixUiState
    data class Error(val message: String) : CorrelationMatrixUiState
    data class Success(
        val result: MatrixResult,
        val explainerExpanded: Boolean = false
    ) : CorrelationMatrixUiState
}
