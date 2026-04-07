package com.investhelp.app.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val accounts: List<BackupAccount>,
    val items: List<BackupItem>,
    val transactions: List<BackupTransaction>,
    val bankTransfers: List<BackupBankTransfer> = emptyList()
)

@Serializable
data class BackupAccount(
    val id: Long,
    val name: String,
    val description: String,
    val initialValue: Double
)

@Serializable
data class BackupItem(
    val id: Long,
    val name: String,
    val ticker: String? = null,
    val type: String,
    val currentPrice: Double,
    val numShares: Double = 0.0
)

@Serializable
data class BackupTransaction(
    val id: Long,
    val dateEpochDay: Long,
    val timeSecondOfDay: Int? = null,
    val action: String,
    val accountId: Long,
    val ticker: String,
    val numberOfShares: Double,
    val pricePerShare: Double,
    val totalAmount: Double = 0.0,
    val note: String = ""
)

@Serializable
data class BackupBankTransfer(
    val id: Long,
    val dateEpochDay: Long,
    val amount: Double,
    val accountId: Long,
    val note: String = ""
)
