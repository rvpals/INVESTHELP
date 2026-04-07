package com.investhelp.app.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val version: Int = 1,
    val accounts: List<BackupAccount>,
    val items: List<BackupItem>,
    val transactions: List<BackupTransaction>
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
    val currentPrice: Double
)

@Serializable
data class BackupTransaction(
    val id: Long,
    val dateEpochDay: Long,
    val timeSecondOfDay: Int,
    val action: String,
    val accountId: Long,
    val investmentItemId: Long,
    val numberOfShares: Double,
    val pricePerShare: Double
)
