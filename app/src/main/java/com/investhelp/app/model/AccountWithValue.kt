package com.investhelp.app.model

import com.investhelp.app.data.local.entity.InvestmentAccountEntity

data class AccountWithValue(
    val account: InvestmentAccountEntity,
    val currentValue: Double
)
